package norivensuu.didimisssomething.common;

import norivensuu.didimisssomething.common.providers.Provider;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ProvidersRegistry {
    private static Map<String, Provider> providers;

    static {
        if (providers == null) {
            providers = new HashMap<>();
        }
    }

    public static <T extends Provider> void tryRegister(T provider) {
        if (providers.containsKey(provider.id))
            throw new RuntimeException(String.format("Could not register duplicate provider %s", provider.id));

        register(provider.id, provider);
    }
    private static <T extends Provider> void register(String id, T provider) {
        providers.put(id, provider);
    }

    public static <T extends Provider> T get(String id) {
        return (T) providers.get(id);
    }

    public static <T extends Provider> T matching(String api) {
        var providerApi = new ProvidersApi(api);

        for (var entry : providers.entrySet().stream().filter((entry) -> providerApi.id == null || providerApi.id.isBlank() || entry.getKey().equals(providerApi.id)).collect(Collectors.toSet())) {
            Provider provider = entry.getValue();

            if (provider.validateApi(providerApi.url)) return (T) provider;
        }

        throw new RuntimeException(String.format("No providers matched the api string %s %s", api, providers));
    }
}
