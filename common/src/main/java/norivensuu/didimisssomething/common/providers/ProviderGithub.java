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

    public JSONObject getLatestRelease(String apiUrl, String token) throws IOException, ParseException {
        HttpsURLConnection connection = (HttpsURLConnection) URI.create(apiUrl).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

        authenticate(connection, token);

        InputStream inputStream = connection.getInputStream();
        String response = new String(inputStream.readAllBytes());
        JSONParser parser = new JSONParser();
        JSONObject release = (JSONObject) parser.parse(response);

        return release;
    }

    @Override
    public String getLatestReleaseVersion(String apiUrl, String token) throws IOException, ParseException {
        var release = getLatestRelease(apiUrl, token);

        return release != null ? release.getAsString("tag_name") : null;
    }

    @Override
    public String getLatestReleaseURL(String apiUrl, String token) throws IOException, ParseException {
        var release = getLatestRelease(apiUrl, token);

        return release != null ? release.getAsString("zipball_url") : null;
    }

    @Override
    public String getLatestReleaseAssetURL(String apiUrl, String token) throws IOException, ParseException {
        var release = getLatestRelease(apiUrl, token);

        JSONArray assets = (JSONArray) release.get("assets");

        if (assets == null || assets.isEmpty()) {
            return null;
        }

        JSONObject selectedAsset = null;

        for (Object obj : assets) {
            JSONObject asset = (JSONObject) obj;
            String name = asset.getAsString("name");

            if (name == null) continue;

            if (name.endsWith(".jar") && !name.endsWith("-sources.jar")) {
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
