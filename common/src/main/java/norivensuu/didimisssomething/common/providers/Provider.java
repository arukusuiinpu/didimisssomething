package norivensuu.didimisssomething.common.providers;

import net.minidev.json.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Provider {

    public String id;

    public Provider(String id) {
        this.id = id;
    }

    public ProviderApi normalizeApi(String rawApi) {
        return new ProviderApi(rawApi);
    }

    public abstract boolean validateApi(String api);
    public abstract void authenticate(URLConnection connection, String token);

    public abstract String getLatestReleaseVersion(String apiUrl, String token) throws IOException, ParseException;
    public String getLatestReleaseVersion(String apiUrl) throws IOException, ParseException { return getLatestReleaseVersion(apiUrl, null); }

    public abstract String getLatestReleaseURL(String apiUrl, String token) throws IOException, ParseException;
    public String getLatestReleaseURL(String apiUrl) throws IOException, ParseException { return getLatestReleaseURL(apiUrl, null); }

    public abstract String getLatestReleaseAssetURL(String apiUrl, String token) throws IOException, ParseException;
    public String getLatestReleaseAssetURL(String apiUrl) throws IOException, ParseException { return getLatestReleaseURL(apiUrl, null); }

    public abstract boolean unpackRelease(File file, String outputDir) throws IOException, ParseException;
}

