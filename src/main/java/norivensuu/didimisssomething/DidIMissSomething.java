package norivensuu.didimisssomething;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.discovery.*;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.fabricmc.loader.impl.launch.knot.KnotClient;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.VersionOverrides;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.block.CropBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.main.Main;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!currentHash.equals(previousHash)) {
            try {
                Files.write(stateFile.toPath(), currentHash.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
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
                } catch (IOException e) {
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
                } catch (IOException e) {
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
                } catch (IOException e) {
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
        boolean properApi = (Config.usingMirror() && !apiURL.equals("PLACE_YOUR_MIRROR_API_URL_IN_HERE")) || (!Config.usingMirror() && !apiURL.equals("PLACE_YOUR_MIRROR_API_URL_IN_HERE"));
        if (apiURL != null && properApi) {
            boolean isGitLab = apiURL.contains("gitlab.com");

            String latestRelease = isGitLab
                    ? getTheLatestGitLabRelease(apiURL, gitlabToken)
                    : getTheLatestRelease(apiURL, githubToken);

            File folder = new File("didimisssomething/");
            if (!folder.exists()) folder.mkdir();

            if (!checkTheLatestRelease(new File("didimisssomething/release.txt"), latestRelease)) {
                LOGGER.info("Downloading release {}...", latestRelease);

                File zip = isGitLab
                        ? downloadTheLatestGitLabRelease(apiURL, new File("didimisssomething/downloads/latest-release.zip"), gitlabToken)
                        : downloadTheLatestRelease(apiURL, new File("didimisssomething/downloads/latest-release.zip"), githubToken);

                if (zip != null) {
                    try {
                        FileUtils.deleteDirectory(new File("didimisssomething/downloads/unpacked/latest-release/"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    LOGGER.info("Unpacking '{}'...", zip.getName());
                    unpackZip(zip, "didimisssomething/downloads/unpacked/latest-release/");
                }

                restartGame(latestRelease);
            }
            if (!checkModsAdditionalFolder()) {
                populateMods(latestRelease);
                System.exit(0);
            }
        } else {
            LOGGER.info("Please specify your apiURL in {}config\\{}.txt!", new File("").getAbsolutePath(), MOD_ID);
        }
        return false;
    }


    public static void restartGame(String latestRelease) {
        populateMods(latestRelease);

        try {
            LOGGER.info("RESTARTING in 5 seconds!");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        checkModsAdditionalFolder();

        System.exit(0);
    }

    public static void populateMods(String latestRelease) {
        try {
            createUpdateModsBatFile(latestRelease);

            Process process = Runtime.getRuntime().exec(new String[] {"cmd", "/c", "start " + new File("").getAbsolutePath() + "\\didimisssomething\\updateModsAndRestart.bat"});

            LOGGER.info("updateModsAndRestart.bat executed successfully: {}", process.info());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createUpdateModsBatFile(String latestRelease) {
        String batContent =
                "@echo off\r\n"
                        + "setlocal EnableDelayedExpansion\r\n"
                        + "REM Ensure required directories exist\r\n"
                        + "if not exist \"" + new File("").getAbsolutePath() + "\\mods\" mkdir \"" + new File("").getAbsolutePath() + "\\mods\"\r\n"
                        + "if not exist \"" + new File("").getAbsolutePath() + "\\config\" mkdir \"" + new File("").getAbsolutePath() + "\\config\"\r\n"
                        + "if not exist \"" + new File("").getAbsolutePath() + "\\didimisssomething\\mods-previous\" mkdir \"" + new File("").getAbsolutePath() + "\\didimisssomething\\mods-previous\"\r\n"
                        + "if not exist \"" + new File("").getAbsolutePath() + "\\mods-additional\" mkdir \"" + new File("").getAbsolutePath() + "\\mods-additional\"\r\n"
                        + "\r\n"
                        + "echo Copying the mods from \"" + new File("").getAbsolutePath() + "\\mods\" to \"" + new File("").getAbsolutePath() + "\\didimisssomething\\mods-previous\"...\r\n"
                        + "rmdir /S /Q \"" + new File("").getAbsolutePath() + "\\didimisssomething\\mods-previous\"\r\n"
                        + "mkdir \"" + new File("").getAbsolutePath() + "\\didimisssomething\\mods-previous\"\r\n"
                        + "xcopy /E /I /Y \"" + new File("").getAbsolutePath() + "\\mods\\*\" \"" + new File("").getAbsolutePath() + "\\didimisssomething\\mods-previous\\\"\r\n"
                        + "\r\n"
                        + "echo Adding downloaded mods to \"" + new File("").getAbsolutePath() + "\\mods\"...\r\n"
                        + "rmdir /S /Q \"" + new File("").getAbsolutePath() + "\\mods\"\r\n"
                        + "mkdir \"" + new File("").getAbsolutePath() + "\\mods\"\r\n"
                        + "\r\n"
                        + "echo Adding back mods from \"" + new File("").getAbsolutePath() + "\\mods-additional\"...\r\n"
                        + "rmdir /S /Q \"" + new File("").getAbsolutePath() + "\\mods\"\r\n"
                        + "mkdir \"" + new File("").getAbsolutePath() + "\\mods\"\r\n"
                        + "xcopy /E /I /Y \"" + new File("").getAbsolutePath() + "\\mods-additional\\*\" \"" + new File("").getAbsolutePath() + "\\mods\\\"\r\n"
                        + "\r\n"
                        + "echo Populating mods and configs from \"" + new File("").getAbsolutePath() + "\\didimisssomething\\downloads\\unpacked\\latest-release\\*\"\r\n"
                        + "for /d %%D in (\"" + new File("").getAbsolutePath() + "\\didimisssomething\\downloads\\unpacked\\latest-release\\*\") do (\r\n"
                        + "   set \"source=%%D\"\r\n"
                        + "   if exist \"!source!\\mods\\\" xcopy /E /I /Y \"!source!\\mods\\*\" \"" + new File("").getAbsolutePath() + "\\mods\\\"\r\n"
                        + "   if exist \"!source!\\config\\\" xcopy /E /I /Y \"!source!\\config\\*\" \"" + new File("").getAbsolutePath() + "\\config\\\"\r\n"
                        + "   if exist \"!source!\\datapacks\\\" xcopy /E /I /Y \"!source!\\datapacks\\*\" \"" + new File("").getAbsolutePath() + "\\datapacks\\\"\r\n"
                        + ")\r\n"
                        + "\r\n"
                        + "echo latest-release:" + latestRelease + " > \"" + new File("").getAbsolutePath() + "\\didimisssomething\\release.txt\"\r\n"
                        + "\r\n"
                        + "echo RESTART THE GAME\r\n"
                        + "\r\n"
                        + "echo This file will close in 5 seconds...\r\n"
                        + "timeout 5 > NUL\r\n"
                        + "exit 0\r\n";

        File batFile = new File("didimisssomething\\updateModsAndRestart.bat");
        try {
            Files.write(batFile.toPath(), batContent.getBytes(StandardCharsets.UTF_8));
            System.out.println("Batch file created successfully at: " + batFile.getAbsolutePath());
        } catch (IOException e) {
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
        } catch (IOException e) {
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

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File downloadTheLatestRelease(String apiUrl, File destination, String githubToken) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + githubToken);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            InputStream inputStream = connection.getInputStream();
            String response = new String(inputStream.readAllBytes());
            JSONParser parser = new JSONParser();
            JSONObject release = (JSONObject) parser.parse(response);

            String zipballUrl = release.getAsString("zipball_url");

            return downloadURL(zipballUrl, destination, githubToken);

        } catch (IOException | ParseException e) {
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
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File downloadTheLatestGitLabRelease(String url, File destination, String gitlabToken) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestProperty("PRIVATE-TOKEN", gitlabToken);
            connection.setRequestMethod("GET");

            InputStream inputStream = connection.getInputStream();
            String response = new String(inputStream.readAllBytes());

            String zipURL = "";
            JSONArray releases = (JSONArray) new JSONParser().parse(response);
            if (!releases.isEmpty()) {
                JSONObject latestRelease = (JSONObject) releases.getFirst();

                JSONObject assets = (JSONObject) latestRelease.get("assets");
                JSONArray sources = (JSONArray) assets.get("sources");
                for (Object sourceObject : sources) {
                    JSONObject source = (JSONObject) sourceObject;

                    if (source.get("format").equals("zip")) {
                        zipURL = (String) source.get("url");
                    }
                }
            }

            return downloadURL(zipURL, destination, gitlabToken);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File downloadURL(String url, File destination, String token) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");

            if (url.contains("gitlab.com")) {
                connection.setRequestProperty("PRIVATE-TOKEN", token);
            }
            else {
                connection.setRequestProperty("Authorization", "token " + token);
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            }

            InputStream inputStream = connection.getInputStream();
            FileUtils.copyInputStreamToFile(inputStream, destination);
            inputStream.close();

            LOGGER.info("Downloaded to: {}", destination.getAbsolutePath());
            return destination;
        } catch (IOException e) {
            LOGGER.info("Download failed: {}", e.getMessage());

            System.exit(0);
        }
        return null;
    }

    public static boolean unpackZip(File file, String outputDir) {
        try (java.util.zip.ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryDestination = new File(outputDir,  entry.getName());
                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    try (InputStream in = zipFile.getInputStream(entry);
                         OutputStream out = new FileOutputStream(entryDestination)) {
                        IOUtils.copy(in, out);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}