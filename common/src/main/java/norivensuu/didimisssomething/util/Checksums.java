package norivensuu.didimisssomething.util;

import norivensuu.didimisssomething.DidIMissSomething;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static norivensuu.didimisssomething.DidIMissSomething.*;

public final class Checksums {
    public static boolean isDevelopmentEnvironment() {
        try {
            URL location = DidIMissSomething.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation();

            if (location == null) return true;

            String path = new File(location.toURI()).getPath().toLowerCase();
            return !path.endsWith(".jar") || path.contains("/build/") || path.contains("\\build\\") || path.contains("/temp/") || path.contains("\\temp\\");
        } catch (Exception e) {
            return true;
        }
    }

    public static String readSelfHash() {
        try (InputStream in = DidIMissSomething.class.getResourceAsStream("/didimisssomething.sha256")) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            LOGGER.error("Failed to read self hash", e);
            return null;
        }
    }

    public static boolean verifySelfJar() {
        if (isDevelopmentEnvironment()) {
            LOGGER.info("Skipping self-verification in dev environment");
            return true;
        }

        try {
            URL location = DidIMissSomething.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation();

            File jarFile = new File(location.toURI());
//            LOGGER.info(jarFile.getAbsolutePath());
            String actual = mesJarExcludingSelfHash(jarFile);
            String expected = readSelfHash();

            if (expected == null) {
                LOGGER.error("Missing /didimisssomething.sha256 resource");
                return false;
            }

            if (!actual.equalsIgnoreCase(expected)) {
                LOGGER.error("Jar hash mismatch: {} != {}", actual, expected);
                return false;
            }

            LOGGER.info("Jar hash matches expected!");
            return true;
        } catch (Exception e) {
            LOGGER.error("Self verification failed", e);
            return false;
        }
    }

    private static String mesJarExcludingSelfHash(File jarFile) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");

        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            List<JarEntry> entries = Collections.list(jar.entries()).stream()
                    .filter(e -> !e.getName().equals("didimisssomething.sha256"))
                    .sorted(Comparator.comparing(JarEntry::getName))
                    .toList();

//            LOGGER.info(String.format("[%s]", String.join(", ", entries.stream().map(ZipEntry::toString).toList())));

            byte[] buffer = new byte[8192];
            for (JarEntry entry : entries) {
                digest.update(entry.getName().getBytes(StandardCharsets.UTF_8));

//                LOGGER.info(entry.getName());

                try (InputStream in = jar.getInputStream(entry)) {
                    int read;
                    while ((read = in.read(buffer)) != -1) {
//                        var d = (MessageDigest) digest.clone();
//
//                        byte[] out = d.digest();
//                        StringBuilder sb = new StringBuilder();
//                        for (byte b : out) sb.append(String.format("%02x", b));
//
//                        LOGGER.info(sb.toString());
                        digest.update(buffer, 0, read);
                    }
                }
            }
        }

        byte[] out = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : out) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String sha256(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file.toPath())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(String hex, String expectedHex) {
        return hex.equalsIgnoreCase(expectedHex);
    }
}