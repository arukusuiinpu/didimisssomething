package norivensuu.didimisssomething.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProvidersApi {
    public String id;
    public String url;

    public ProvidersApi(String rawApi) {
        normalizeApi(rawApi);
    }

    public ProvidersApi(String id, String url) {
        this.id = id;
        this.url = url;
    }

    public void normalizeApi(String rawApi) {
        Pattern pattern = Pattern.compile("(\\(?(?<id>[^()]*)?\\)? +)?(?<api>.+)");
        Matcher matcher = pattern.matcher(rawApi);

        String idString;
        String apiString;

        if (matcher.find()) {
            if (matcher.groupCount() > 1) {
                idString = matcher.group("id");
                apiString = matcher.group("api");
            }
            else {
                idString = "";
                apiString = matcher.group("api");
            }
        }
        else {
            throw new RuntimeException(String.format("Invalid api string: %s!", rawApi));
        }

        this.id = idString;
        this.url = apiString;
    }
}
