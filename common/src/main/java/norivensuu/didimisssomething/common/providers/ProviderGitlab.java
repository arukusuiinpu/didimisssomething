package norivensuu.didimisssomething.common.providers;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ProviderGitlab extends Provider {

    public ProviderGitlab() {
        super("gitlab");
    }

    @Override
    public boolean validateApi(String api) {
        return api.startsWith("https://gitlab.com/api");
    }

    @Override
    public void authenticate(URLConnection connection, String token) {
        if (token != null && !token.isBlank()) {
            connection.setRequestProperty("PRIVATE-TOKEN", token);
        }
    }

    public JSONObject getLatestRelease(String apiUrl, String token, Pattern releasePattern, String minecraftVersionRange) throws IOException, ParseException {
        HttpsURLConnection connection = (HttpsURLConnection) URI.create(apiUrl).toURL().openConnection();
        connection.setRequestMethod("GET");

        authenticate(connection, token);

        InputStream inputStream = connection.getInputStream();
        String response = new String(inputStream.readAllBytes());
        JSONParser parser = new JSONParser();
        JSONArray releases = (JSONArray) parser.parse(response);

        for (var release : releases) {
            Matcher matcher = releasePattern.matcher((String) ((JSONObject) release).get("tag_name"));

            if (matcher.matches()) {
                try {
                    if (matcher.group("minecraftVersion") == null || Provider.checkVersion(matcher.group("minecraftVersion"), minecraftVersionRange)) {
                        return (JSONObject) release;
                    }
                }
                catch (Exception e) {
                    return (JSONObject) release;
                }
                break;
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

        if (release != null) {
            JSONObject assets = (JSONObject) release.get("assets");
            JSONArray sources = (JSONArray) assets.get("sources");
            for (Object sourceObject : sources) {
                JSONObject source = (JSONObject) sourceObject;

                if (source.get("format").equals("zip")) {
                    return (String) source.get("url");
                }
            }
        }

        return null;
    }

    @Override
    public String getLatestReleaseAssetURL(String apiUrl, String token, Pattern releasePattern, Pattern jarPattern, String minecraftVersionRange) throws IOException, ParseException {
        var release = getLatestRelease(apiUrl, token, releasePattern, minecraftVersionRange);

        JSONObject assets = (JSONObject) release.get("assets");
        JSONArray sources = (JSONArray) assets.get("sources");

        if (sources == null || sources.isEmpty()) {
            return null;
        }

        JSONObject selectedAsset = null;

        for (Object obj : sources) {
            JSONObject asset = (JSONObject) obj;
            String url = asset.getAsString("url");

            if (url == null) continue;

            Matcher matcher = jarPattern.matcher(url.substring(url.lastIndexOf("/")+1));

            if (matcher.matches()) {
                try {
                    if (matcher.group("minecraftVersion") == null || Provider.checkVersion(matcher.group("minecraftVersion"), minecraftVersionRange)) {
                        selectedAsset = asset;
                    }
                }
                catch (Exception e) {
                    selectedAsset = asset;
                }
                break;
            }
        }

        if (selectedAsset == null) {
            selectedAsset = (JSONObject) sources.getFirst();
        }

        String downloadUrl = selectedAsset.getAsString("url");

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

            if (root != null && root.equals("latest-release"))
                root = null;

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
