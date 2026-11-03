package norivensuu.didimisssomething;

import net.minidev.json.JSONObject;
import norivensuu.didimisssomething.common.Config;
import norivensuu.didimisssomething.common.Main;
import norivensuu.didimisssomething.common.ProvidersBenchmark;
import norivensuu.didimisssomething.common.providers.ProviderGithub;
import norivensuu.didimisssomething.common.providers.ProviderGitlab;
import norivensuu.didimisssomething.common.ProvidersRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

import static norivensuu.didimisssomething.util.Checksums.*;
import static norivensuu.didimisssomething.common.Main.*;

public class DidIMissSomething {

    public static final String MOD_ID = Main.MOD_ID;

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void initialize() {

        LOGGER.info("Haiiii my little meow meows!");

        ProvidersRegistry.tryRegister(new ProviderGithub());
        ProvidersRegistry.tryRegister(new ProviderGitlab());

        Config.get("useModsMetadataToUpdate", false);
        didIMissSomething(Config.getRepositoryApiUrls());
    }

    public static boolean didIMissSomething(Map<String, Object> repositoryApiUrls) {
        if (!verifySelfJar()) {
            LOGGER.error("JAR INTEGRITY CHECK FAILED. ABORTING. MAKE SURE THE MOD IS DOWNLOADED FROM A RELIABLE SOURCE!");
            System.exit(0);
        }

        boolean properApis = !repositoryApiUrls.equals(Config.defaultRepositoryApiUrls);

        if (!properApis) {
            LOGGER.warn("Please specify your repositoryApiUrls in {}\\config\\\\{}.json!", new File(projectPath).getAbsolutePath(), MOD_ID);
            return false;
        }

        for (var entry : ProvidersBenchmark.pingProviders(repositoryApiUrls).entrySet())
        {
            try {
                String apiURL = entry.getKey();
                String token = ((JSONObject) entry.getValue()).getOrDefault("token", "").toString();

                Pattern releasePattern = Pattern.compile(((JSONObject) entry.getValue()).getOrDefault("releaseRegex", ".*").toString());
                String minecraftVersionRange = Config.get("minecraftVersionRange", "*");

                if (apiURL != null) {

                    var provider = ProvidersRegistry.matching(apiURL);

                    LOGGER.info("Testing provider: {}", provider.id);

                    if (provider != null) {
                        File folder = new File(projectPath + "/didimisssomething/");
                        if (!folder.exists()) folder.mkdir();

                        File releaseFile = new File(projectPath + "/didimisssomething/release.txt");
                        if (!releaseFile.exists()) releaseFile.createNewFile();

                        List<String> lines = Files.readAllLines(releaseFile.toPath());
                        boolean allUpToDate = true;

                        for (String line : lines) {
                            if (line.startsWith("latest-release;")) {
                                String currentRelease = line.split(";")[1].trim();
                                String latestRelease = provider.getLatestReleaseVersion(provider.normalizeApi(apiURL).url, token, releasePattern, minecraftVersionRange);
                                LOGGER.info(latestRelease);
                                if (!currentRelease.equals(latestRelease)) {
                                    allUpToDate = false;
                                    break;
                                }
                            } else if (!line.isBlank() && line.contains(";") && Config.get("useModsMetadataToUpdate", false)) {
                                String[] parts = line.split(";");
                                if (parts.length >= 3) {
                                    String modName = parts[0].trim();
                                    String currentVersion = parts[1].trim();
                                    String modApiUrl = parts[2].trim();
                                    String modToken = "";
                                    if (parts.length >= 4) modToken = parts[3].trim();

                                    String latestModRelease = provider.getLatestReleaseVersion(provider.normalizeApi(modApiUrl).url, modToken, releasePattern, minecraftVersionRange);

                                    LOGGER.info("{} {} vs {}", modName, latestModRelease, currentVersion);

                                    if (!currentVersion.equals(latestModRelease)) {
                                        allUpToDate = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (lines.isEmpty() || !lines.getFirst().startsWith("latest-release;")) allUpToDate = false;

                        if (!allUpToDate || !checkModsAdditionalFolder()) {
                            checkModsAdditionalFolder();
                            restartGame();
                        }

                        if (allUpToDate) {
                            LOGGER.info("The latest releases are already in the file.");
                            break;
                        }
                    }
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
                    LOGGER.error("Unable to locate the autoupdater file (autoupdater.jar)");
                    System.exit(0);
                }
                updaterJar.getParentFile().mkdirs();
                Files.copy(in, updaterJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

            ProcessBuilder pb = new ProcessBuilder(javaBin, "-jar", updaterJar.getAbsolutePath());
            pb.directory(new File(projectPath + "/didimisssomething"));
            pb.inheritIO();
            LOGGER.info("Launching autoupdater.jar...");
            pb.start();
            LOGGER.info("DO NOT START MINECRAFT UNTIL THE UPDATER IS DONE!");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}