package norivensuu.didimisssomething;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.parser.JSONParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.*;

public class DidIMissSomething {
	public static final String MOD_ID = "didimisssomething";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String projectPath = System.getProperty("user.dir");

    public static void initialize() {
        LOGGER.info("Haiiii my little meow meows!");

        Config.get("useModsMetadataToUpdate", false);
        didIMissSomething(Config.getRepositoryApiUrls());
    }

    public static boolean didIMissSomething(Map<String, Object> repositoryApiUrls) {
        boolean properApis = !repositoryApiUrls.equals(Map.of(
                "PLACE_YOUR_API_URL_IN_HERE", "PLACE_YOUR_GITHUB_TOKEN_IN_HERE",
                "PLACE_YOUR_MIRROR_API_URL_IN_HERE", "PLACE_YOUR_GITLAB_TOKEN_IN_HERE"
        ));

        if (!properApis) {
            LOGGER.warn("Please specify your repositoryApiUrls in {}\\config\\\\{}.json!", new File(projectPath).getAbsolutePath(), MOD_ID);
            return false;
        }

        for (var entry : repositoryApiUrls.entrySet())
        {
            try {
                String apiURL = entry.getKey();
                String token = entry.getValue().toString();

                if (apiURL != null && URI.create(apiURL).isAbsolute()) {
                    boolean isGitLab = apiURL.contains("gitlab.com");

                    String latestRelease = isGitLab
                            ? getTheLatestGitLabRelease(apiURL, token)
                            : getTheLatestRelease(apiURL, token);
                    // No other ways to check the latest release for now :(, sorry...

                    File folder = new File(projectPath + "/didimisssomething/");
                    if (!folder.exists()) folder.mkdir();

                    if (!checkTheLatestRelease(new File(projectPath + "/didimisssomething/release.txt"), latestRelease) || !checkModsAdditionalFolder()) {
                        checkModsAdditionalFolder();
                        restartGame();
                    }

                    return true; // Just to be sure
                } else {
                    LOGGER.warn("Please provide a valid apiURL instead of {} in {}\\config\\\\{}.json!.", apiURL, new File(projectPath).getAbsolutePath(), MOD_ID);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static class Config {
        private static final File CONFIG_FILE = new File(projectPath + "/config/" + MOD_ID + ".json");
        private static Map<String, Object> configData = new HashMap<>();

        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        static {
            load();
        }

        public static void load() {
            try {
                if (CONFIG_FILE.exists()) {
                    String content = FileUtils.readFileToString(CONFIG_FILE, StandardCharsets.UTF_8);
                    JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);

                    JSONObject json = (JSONObject) parser.parse(content);
                    configData.putAll(json);
                } else {
                    CONFIG_FILE.getParentFile().mkdirs();
                    save();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load config, using defaults", e);
                configData = new JSONObject();
            }
        }

        private static void save() {
            try {
                String prettyJson = GSON.toJson(configData);

                FileUtils.writeStringToFile(CONFIG_FILE, prettyJson, StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static <T> T get(String key, T defaultValue) {
            if (!configData.containsKey(key)) {
                configData.put(key, defaultValue);
                save();
                return defaultValue;
            }

            Object value = configData.get(key);

            if (defaultValue instanceof String) {
                return (T) String.valueOf(value);
            }

            return (T) value;
        }

        public static Map<String, Object> getRepositoryApiUrls() {
            return get("repositoryApiUrls", Map.of(
                    "PLACE_YOUR_API_URL_IN_HERE", "PLACE_YOUR_GITHUB_TOKEN_IN_HERE",
                    "PLACE_YOUR_MIRROR_API_URL_IN_HERE", "PLACE_YOUR_GITLAB_TOKEN_IN_HERE"
            ));
        }
    }

    public static boolean checkModsAdditionalFolder() {
        LOGGER.info("Checking mods-additional...");
        File modsAdditional = new File(projectPath + "/mods-additional");
        if (!modsAdditional.exists()) {
            modsAdditional.mkdirs();
        }

        String currentHash = computeFolderHash(modsAdditional);
        File stateFile = new File(projectPath + "/didimisssomething/mods-additional-state.txt");
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

    public static void restartGame() {
        createAndLaunchUpdater();
    }

    public static void createAndLaunchUpdater() {
        try {
            File updaterJar = new File(projectPath + "/didimisssomething/autoupdater.jar");

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
            pb.directory(new File(projectPath + "/didimisssomething"));
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