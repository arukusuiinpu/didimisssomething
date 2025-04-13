package norivensuu.didimisssomething;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.*;

@Environment(EnvType.CLIENT)
public class DidIMissSomething implements PreLaunchEntrypoint {
	public static final String MOD_ID = "didimisssomething";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    static {
        LOGGER.info("Haiiii my little meow meows!");

        didIMissSomething(DidIMissSomething.Config.getApiURL(), DidIMissSomething.Config.getGithubToken(), DidIMissSomething.Config.getGitlabToken());
    }

    @Override
    public void onPreLaunch() {

    }

    public static boolean checkModsAdditionalFolder() {
        LOGGER.info("Checking mods-additional...");
        File modsAdditional = new File("mods-additional");
        if (!modsAdditional.exists()) {
            modsAdditional.mkdirs();
        }

        String currentHash = computeFolderHash(modsAdditional);
        File stateFile = new File("didimisssomething/mods-additional-state.txt");
        String previousHash = "";
        if (stateFile.exists()) {
            try {
                previousHash = new String(Files.readAllBytes(stateFile.toPath()), StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!currentHash.equals(previousHash)) {
            try {
                Files.write(stateFile.toPath(), currentHash.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception e) {
                e.printStackTrace();
            }
            LOGGER.info("mods-additional folder has changed. Restarting game...");
            return false;
        }
        else {
            LOGGER.info("There were no additional mods :>");
            return true;
        }
    }

    public static String computeFolderHash(File folder) {
        List<String> fileData = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    fileData.add(computeFolderHash(file));
                } else {
                    fileData.add(file.getAbsolutePath() + file.lastModified() + file.length());
                }
            }
        }
        Collections.sort(fileData);
        StringBuilder sb = new StringBuilder();
        for (String s : fileData) {
            sb.append(s);
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            BigInteger bigInt = new BigInteger(1, digest);
            String hashText = bigInt.toString(16);
            while (hashText.length() < 32) {
                hashText = "0" + hashText;
            }
            return hashText;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static class Config {
        public static File getConfig() {
            File config = new File("config/" + MOD_ID + ".txt");

            if (!config.exists()) {
                try {
                    config.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            return config;
        }
        public static File getMirrorConfig() {
            File config = new File("config/" + MOD_ID + "-mirror.txt");

            if (!config.exists()) {
                try {
                    config.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            return config;
        }
        public static String getData(File config, String data, String defaultData) {

            if (config != null) {
                try {
                    for (String line : FileUtils.readLines(config)) {
                        if (line.startsWith(data + ":")) {
                            return line.substring(data.length() + 1).trim();
                        }
                    }

                    Files.write(Paths.get(config.toURI()), (data + ":" + defaultData + "\n").getBytes(), StandardOpenOption.APPEND);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
        public static String getApiURL() {
            if (usingMirror()) return getData(getConfig(), "mirrorApiURL", "PLACE_YOUR_MIRROR_API_URL_IN_HERE");
            else return getData(getConfig(), "apiURL", "PLACE_YOUR_API_URL_IN_HERE");
        }
        public static String getGithubToken() {
            return getData(getConfig(), "githubToken", "PLACE_YOUR_GITHUB_TOKEN_IN_HERE");
        }
        public static String getGitlabToken() {
            return getData(getConfig(), "gitlabToken", "PLACE_YOUR_GITLAB_TOKEN_IN_HERE");
        }
        public static boolean usingMirror() {
            return Objects.equals(getData(getMirrorConfig(), "usingMirror", "false"), "true");
        }
    }

    public static boolean didIMissSomething(String apiURL, String githubToken, String gitlabToken) {
        if (apiURL == null || !URI.create(apiURL).isAbsolute()) return false;

        boolean properApi = (Config.usingMirror() && !apiURL.equals("PLACE_YOUR_MIRROR_API_URL_IN_HERE")) || (!Config.usingMirror() && !apiURL.equals("PLACE_YOUR_MIRROR_API_URL_IN_HERE"));
        if (properApi) {
            boolean isGitLab = apiURL.contains("gitlab.com");

            String latestRelease = isGitLab
                    ? getTheLatestGitLabRelease(apiURL, gitlabToken)
                    : getTheLatestRelease(apiURL, githubToken);

            File folder = new File("didimisssomething/");
            if (!folder.exists()) folder.mkdir();

            if (!checkTheLatestRelease(new File("didimisssomething/release.txt"), latestRelease) || !checkModsAdditionalFolder()) {
                checkModsAdditionalFolder();
                restartGame();
            }
        } else {
            LOGGER.info("Please specify your apiURL in {}config\\{}.txt!", new File("").getAbsolutePath(), MOD_ID);
        }
        return false;
    }


    public static void restartGame() {
        createAndLaunchUpdater();
    }

    public static void createAndLaunchUpdater() {
        try {
            File updaterJar = new File("didimisssomething/autoupdater.jar");

            try (InputStream in = DidIMissSomething.class.getResourceAsStream("/autoupdater.jar")) {
                if (in == null) {
                    LOGGER.error("Unable to locate updater resource (autoupdater.jar)");
                    return;
                }
                updaterJar.getParentFile().mkdirs();
                Files.copy(in, updaterJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

            ProcessBuilder pb = new ProcessBuilder(javaBin, "-jar", updaterJar.getAbsolutePath());
            pb.directory(new File("didimisssomething"));
            //pb.inheritIO();
            LOGGER.info("Launching autoupdater.jar...");
            pb.start();
            LOGGER.info("DO NOT START MINECRAFT UNTIL THE UPDATER IS DONE!");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static boolean checkTheLatestRelease(File file, String latestRelease) {
        LOGGER.info("Checking the '{}'...", file);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            List<String> lines = Files.readAllLines(file.toPath());

            for (String line : lines) {
                if (line.startsWith("latest-release:")) {
                    String currentRelease = line.split(":")[1].trim();

                    if (currentRelease.equals(latestRelease)) {
                        LOGGER.info("The latest release is already in the file.");
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getTheLatestRelease(String apiUrl, String githubToken) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + githubToken);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            InputStream inputStream = connection.getInputStream();
            String response = new String(inputStream.readAllBytes());
            JSONParser parser = new JSONParser();
            JSONObject release = (JSONObject) parser.parse(response);

            return release.getAsString("tag_name");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getTheLatestGitLabRelease(String apiURL, String gitlabToken) {
        try {
            URL url = URI.create(apiURL).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("PRIVATE-TOKEN", gitlabToken);
            connection.setRequestMethod("GET");

            InputStream responseStream = connection.getInputStream();
            String json = IOUtils.toString(responseStream, StandardCharsets.UTF_8);
            responseStream.close();

            JSONArray releases = (JSONArray) new JSONParser().parse(json);
            if (!releases.isEmpty()) {
                JSONObject latestRelease = (JSONObject) releases.get(0);
                return (String) latestRelease.get("tag_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}