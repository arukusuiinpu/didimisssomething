package norivensuu.didimisssomething;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DidIMissSomething implements PreLaunchEntrypoint {
	public static final String MOD_ID = "didimisssomething";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onPreLaunch() {
        LOGGER.info("Haiiii my little meow meows!");

        didIMissSomething(Config.getApiURL(), Config.getGithubToken());
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
        public static String getData(String data, String defaultData) {
            File config = getConfig();

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
            return getData("apiURL", "PLACE_YOUR_API_URL_IN_HERE");
        }
        public static String getGithubToken() {
            return getData("githubToken", "PLACE_YOUR_GITHUB_TOKEN_IN_HERE");
        }
    }

    public static boolean didIMissSomething(String apiURL, String githubToken) {
        if (apiURL != null && !apiURL.equals("PLACE_YOUR_API_URL_IN_HERE")) {
            String latestRelease = getTheLatestRelease(apiURL, githubToken);
            if (!checkTheLatestRelease(new File("release.txt"), latestRelease)) {
                LOGGER.info("Downloading release {}...", latestRelease);

                File zip = downloadTheLatestRelease(apiURL, new File("downloads/latest-release.zip"), githubToken);

                if (zip != null) {
                    try {
                        FileUtils.deleteDirectory(new File("downloads/unpacked/latest-release/"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    LOGGER.info("Unpacking '{}'...", zip.getName());
                    unpackZip(zip, "downloads/unpacked/latest-release/");
                }

                restartGame(latestRelease);
            }
        }
        else LOGGER.info("Please specify your apiURL in {}config\\{}.txt!", new File("").getAbsolutePath(), MOD_ID);
        return false;
    }

    public static void restartGame(String latestRelease) {
        try {
            createUpdateModsBatFile(latestRelease);

            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "updateModsAndRestart.bat");
            Process process = pb.start();
            LOGGER.info("updateModsAndRestart.bat executed successfully: {}", process);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    public static void createUpdateModsBatFile(String latestRelease) {
        String batContent =
                "@echo off\r\n"
                        + "setlocal EnableDelayedExpansion\r\n"
                        + "REM Ensure required directories exist\r\n"
                        + "if not exist \"" + new File("").getAbsolutePath() + "\\mods\" mkdir \"" + new File("").getAbsolutePath() + "\\mods\"\r\n"
                        + "if not exist \"" + new File("").getAbsolutePath() + "\\config\" mkdir \"" + new File("").getAbsolutePath() + "\\config\"\r\n"
                        + "if not exist \"" + new File("").getAbsolutePath() + "\\mods-previous\" mkdir \"" + new File("").getAbsolutePath() + "\\mods-previous\"\r\n"
                        + "if not exist \"" + new File("").getAbsolutePath() + "\\mods-additional\" mkdir \"" + new File("").getAbsolutePath() + "\\mods-additional\"\r\n"
                        + "\r\n"
                        + "echo Copying the mods from \"" + new File("").getAbsolutePath() + "\\mods\" to \"" + new File("").getAbsolutePath() + "\\mods-previous\"...\r\n"
                        + "rmdir /S /Q \"" + new File("").getAbsolutePath() + "\\mods-previous\"\r\n"
                        + "mkdir \"" + new File("").getAbsolutePath() + "\\mods-previous\"\r\n"
                        + "xcopy /E /I /Y \"" + new File("").getAbsolutePath() + "\\mods\\*\" \"" + new File("").getAbsolutePath() + "\\mods-previous\\\"\r\n"
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
                        + "echo Populating mods and configs from \"" + new File("").getAbsolutePath() + "\\downloads\\unpacked\\latest-release\\*\"\r\n"
                        + "for /d %%D in (\"" + new File("").getAbsolutePath() + "\\downloads\\unpacked\\latest-release\\*\") do (\r\n"
                        + "   set \"source=%%D\"\r\n"
                        + "   if exist \"!source!\\mods\\\" xcopy /E /I /Y \"!source!\\mods\\*\" \"" + new File("").getAbsolutePath() + "\\mods\\\"\r\n"
                        + "   if exist \"!source!\\config\\\" xcopy /E /I /Y \"!source!\\config\\*\" \"" + new File("").getAbsolutePath() + "\\config\\\"\r\n"
                        + ")\r\n"
                        + "\r\n"
                        + "echo latest-release:" + latestRelease + " > \"" + new File("").getAbsolutePath() + "\\release.txt\"\r\n"
                        + "\r\n"
                        + "echo RESTART THE GAME\r\n"
                        + "\r\n"
                        + "echo This file will close in 5 seconds...\r\n"
                        + "timeout 5 > NUL\r\n"
                        + "exit 0\r\n";

        File batFile = new File("updateModsAndRestart.bat");
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

    public static File downloadURL(String url, File destination, String githubToken) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + githubToken);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            InputStream inputStream = connection.getInputStream();
            FileUtils.copyInputStreamToFile(inputStream, destination);

            LOGGER.info("Downloaded to: {}", destination.getAbsolutePath());
            return destination;
        } catch (IOException e) {
            LOGGER.info("Download failed: {}", e.getMessage());
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