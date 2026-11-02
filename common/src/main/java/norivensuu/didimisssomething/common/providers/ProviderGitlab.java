package norivensuu.didimisssomething.common.providers;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
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

    public JSONObject getLatestRelease(String apiUrl, String token) throws IOException, ParseException {
        URL url = URI.create(apiUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        authenticate(connection, token);

        InputStream responseStream = connection.getInputStream();

        String json = IOUtils.toString(responseStream, StandardCharsets.UTF_8);
        responseStream.close();

        JSONArray releases = (JSONArray) new JSONParser().parse(json);
        if (!releases.isEmpty()) {
            return (JSONObject) releases.getFirst();
        }
        return null;
    }

    @Override
    public String getLatestReleaseVersion(String apiUrl, String token) throws IOException, ParseException {
        var release = getLatestRelease(apiUrl, token);

        return release != null ? release.getAsString("tag_name") : null;
    }

    @Override
    public String getLatestReleaseURL(String apiUrl, String token) throws IOException, ParseException {
        var release = getLatestRelease(apiUrl, token);

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
    public String getLatestReleaseAssetURL(String apiUrl, String token) {
        throw new RuntimeException(String.format("Unimplemented getLatestReleaseAssetURL provider for %s. Please contact the developer to implement gitlab asset download support!", apiUrl));
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
