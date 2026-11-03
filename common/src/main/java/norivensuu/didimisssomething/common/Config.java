package norivensuu.didimisssomething.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static norivensuu.didimisssomething.common.Main.*;

public class Config {
    private static final File CONFIG_FILE = new File(projectPath + "/config/" + MOD_ID + ".json");
    private static Map<String, Object> configData = new HashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        load();
    }

    {
        load();
    }

    public static void load() {
        try {
            if (CONFIG_FILE.exists()) {
                String content = FileUtils.readFileToString(CONFIG_FILE, StandardCharsets.UTF_8);
                JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);

                JSONObject json = (JSONObject) parser.parse(content);
                configData.putAll(json);
            } else {
                CONFIG_FILE.getParentFile().mkdirs();
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
            configData = new JSONObject();
        }
    }

    private static void save() {
        try {
            String prettyJson = GSON.toJson(configData);

            FileUtils.writeStringToFile(CONFIG_FILE, prettyJson, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> T get(String key, T defaultValue) {
        if (!configData.containsKey(key)) {
            configData.put(key, defaultValue);
            save();
            return defaultValue;
        }

        Object value = configData.get(key);

        if (defaultValue instanceof String) {
            return (T) String.valueOf(value);
        }

        return (T) value;
    }

    public static final Map<String, Object> defaultRepositoryApiUrls = Map.of(
            "(github) https://api.github.com/repos/PLACE_YOUR_USERNAME_HERE/PLACE_YOUR_PROJECTNAME_HERE/releases", Map.of(
            "token", "PLACE_YOUR_GITHUB_TOKEN_HERE",
            "releaseRegex", ".*"
            ),
            "(gitlab) https://gitlab.com/api/v4/projects/PLACE_YOUR_PROJECTID_HERE/releases", Map.of(
            "token", "PLACE_YOUR_GITLAB_TOKEN_HERE",
            "releaseRegex", ".*"
            ),
            "(custom provider) PLACE_YOUR_CUSTOM_PROVIDER_API_HERE", Map.of(
            "token", "PLACE_YOUR_CUSTOM_PROVIDER_API_KEY_HERE",
            "releaseRegex", "PLACE_YOUR_CUSTOM_RELEASE_REGEX_HERE (ex: (?<modId>[^\\d]*-)?((?<minecraftVersion>[\\d.]+)-)?(?<modVersion>[\\d.]+))"
            )
    );

    public static Map<String, Object> getRepositoryApiUrls() {
        return get("repositoryApiUrls", defaultRepositoryApiUrls);
    }
}