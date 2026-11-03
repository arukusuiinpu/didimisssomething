package norivensuu.didimisssomething.common.providers;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ProviderGithub extends Provider {

    public ProviderGithub() {
        super("github");
    }


    @Override
    public boolean validateApi(String api) {
        return api.startsWith("https://api.github.com");
    }

    @Override
    public void authenticate(URLConnection connection, String token) {
        if (token != null && !token.isBlank()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
    }

    public JSONObject getLatestRelease(String apiUrl, String token, Pattern releasePattern, String minecraftVersionRange) throws IOException, ParseException {
        if (apiUrl.endsWith("/latest")) apiUrl = apiUrl.substring(0, apiUrl.lastIndexOf("latest")-1);

        HttpsURLConnection connection = (HttpsURLConnection) URI.create(apiUrl).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

        authenticate(connection, token);

        InputStream inputStream = connection.getInputStream();
        String response = new String(inputStream.readAllBytes());
        JSONParser parser = new JSONParser();
        JSONArray releases = (JSONArray) parser.parse(response);

        for (var release : releases) {
            Matcher matcher = releasePattern.matcher((String) ((JSONObject) release).get("tag_name"));

            if (matcher.find()) {
                if (matcher.group("minecraftVersion") == null || Provider.checkVersion(matcher.group("minecraftVersion"), minecraftVersionRange)) {
                    return (JSONObject) release;
                }
            }
        }

        return null;
    }

    @Override
    public String getLatestReleaseVersion(String apiUrl, String token, Pattern releasePattern, String minecraftVersionRange) throws IOException, ParseException {
        var release = getLatestRelease(apiUrl, token, releasePattern, minecraftVersionRange);

        return release != null ? release.getAsString("tag_name") : null;
    }

    @Override
    public String getLatestReleaseURL(String apiUrl, String token, Pattern releasePattern, String minecraftVersionRange) throws IOException, ParseException {
        var release = getLatestRelease(apiUrl, token, releasePattern, minecraftVersionRange);

        return release != null ? release.getAsString("zipball_url") : null;
    }

    @Override
    public String getLatestReleaseAssetURL(String apiUrl, String token, Pattern releasePattern, Pattern jarPattern, String minecraftVersionRange) throws IOException, ParseException {
        var release = getLatestRelease(apiUrl, token, releasePattern, minecraftVersionRange);

        JSONArray assets = (JSONArray) release.get("assets");

        if (assets == null || assets.isEmpty()) {
            return null;
        }

        JSONObject selectedAsset = null;

        for (Object obj : assets) {
            JSONObject asset = (JSONObject) obj;
            String name = asset.getAsString("name");

            if (name == null) continue;

            Matcher matcher = jarPattern.matcher(name);

            if (matcher.matches() && (matcher.group("minecraftVersion") == null || Provider.checkVersion(matcher.group("minecraftVersion"), minecraftVersionRange))) {
                selectedAsset = asset;
                break;
            }
        }

        if (selectedAsset == null) {
            selectedAsset = (JSONObject) assets.getFirst();
        }

        String downloadUrl = selectedAsset.getAsString("browser_download_url");

        if (downloadUrl == null) {
            return null;
        }

        downloadUrl = downloadUrl.replace("%2B", "+");
        downloadUrl = downloadUrl.replace("%5B", "[");
        downloadUrl = downloadUrl.replace("%5D", "]");

        return downloadUrl;
    }

    @Override
    public boolean unpackRelease(File file, String outputDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            String root = findCommonRoot(zipFile);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                String name = entry.getName();
                if (root != null) {
                    name = name.substring(root.length());
                    if (name.isEmpty()) {
                        continue;
                    }
                }

                File entryDestination = new File(outputDir, name)
                        .getCanonicalFile();

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
        }
    }

    private String findCommonRoot(ZipFile zipFile) {
        String root = null;
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.isEmpty()) {
                continue;
            }
            if (!name.contains("/")) {
                return null;
            }
            String candidate = name.substring(0, name.indexOf('/') + 1);
            if (root == null) {
                root = candidate;
            } else if (!root.equals(candidate)) {
                return null;
            }
        }
        return root;
    }
}
