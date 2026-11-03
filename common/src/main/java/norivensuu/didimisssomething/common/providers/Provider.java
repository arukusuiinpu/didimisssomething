package norivensuu.didimisssomething.common.providers;

import net.minidev.json.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    public static int compareVersions(String v1, String v2) {
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLen = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLen; i++) {
            int num1 = (i < parts1.length) ? Integer.parseInt(parts1[i]) : 0;
            int num2 = (i < parts2.length) ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    public static boolean checkVersion(String version, String versionRange) {
        Pattern pattern = Pattern.compile(
                    "^" +
                          "(?:(?<any>\\*)" +
                          "|(?<single>\\d+(?:\\.\\d+)*)" +
                          "|(?<lessOrEq>[(],(?<lessOrEqV>\\d+(?:\\.\\d+)*)[\\]])" +
                          "|(?<less>[(],(?<lessV>\\d+(?:\\.\\d+)*)[)])" +
                          "|(?<exact>\\[(?<exactV>\\d+(?:\\.\\d+)*)])" +
                          "|(?<moreOrEq>[\\[](?<moreOrEqV>\\d+(?:\\.\\d+)*),[)])" +
                          "|(?<more>[(](?<moreV>\\d+(?:\\.\\d+)*),[)])" +
                          "|(?<rangeLoose>[(](?<rangeLooseL>\\d+(?:\\.\\d+)*),(?<rangeLooseR>\\d+(?:\\.\\d+)*)[)])" +
                          "|(?<rangeStrict>[\\[](?<rangeStrictL>\\d+(?:\\.\\d+)*),(?<rangeStrictR>\\d+(?:\\.\\d+)*)[\\]])" +
                          ")$"
        );

        Matcher matcher = pattern.matcher(versionRange);

        if (matcher.matches()) {
            if (matcher.group("any") != null) return true;
            if (matcher.group("single") != null) return compareVersions(version, matcher.group("single")) >= 0;
            if (matcher.group("lessOrEq") != null) return compareVersions(version, matcher.group("lessOrEqV")) <= 0;
            if (matcher.group("less") != null) return compareVersions(version, matcher.group("lessV")) < 0;
            if (matcher.group("exact") != null) return compareVersions(version, matcher.group("exactV")) == 0;
            if (matcher.group("moreOrEq") != null) return compareVersions(version, matcher.group("moreOrEqV")) >= 0;
            if (matcher.group("more") != null) return compareVersions(version, matcher.group("moreV")) > 0;
            if (matcher.group("rangeLoose") != null) return compareVersions(version, matcher.group("rangeLooseL")) > 0 && compareVersions(version, matcher.group("rangeLooseR")) < 0;
            if (matcher.group("rangeStrict") != null) return compareVersions(version, matcher.group("rangeStrictL")) >= 0 && compareVersions(version, matcher.group("rangeStrictR")) <= 0;
        }

        return false;
    }

    public abstract boolean validateApi(String api);
    public abstract void authenticate(URLConnection connection, String token);

    public abstract String getLatestReleaseVersion(String apiUrl, String token, Pattern releasePattern, String minecraftVersionRange) throws IOException, ParseException;

    public abstract String getLatestReleaseURL(String apiUrl, String token, Pattern releasePattern, String minecraftVersionRange) throws IOException, ParseException;

    public abstract String getLatestReleaseAssetURL(String apiUrl, String token, Pattern releasePattern, Pattern jarPattern, String minecraftVersionRange) throws IOException, ParseException;

    public abstract boolean unpackRelease(File file, String outputDir) throws IOException, ParseException;
}

