package norivensuu.didimisssomething.common;

import net.minidev.json.JSONObject;
import norivensuu.didimisssomething.common.providers.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class ProvidersBenchmark {

    private static final int SAMPLE_BYTES = 256 * 1024;
    private static final int TIMEOUT_MS = 5000;

    public record PingResult(String url, String entryKey, String providerId, long latencyMs, double kbps, boolean ok) {}

    public static Map<String, Object> pingProviders(Map<String, Object> repositoryApiUrls) {
        List<PingResult> results = new ArrayList<>();
        for (var entry : repositoryApiUrls.entrySet()) {
            String apiURL = entry.getKey();
            String token = ((JSONObject) entry.getValue()).getOrDefault("token", "").toString();

            var provider = ProvidersRegistry.matching(apiURL);
            if (provider == null) {
                continue;
            }
            try {
                results.add(ping(provider, apiURL, provider.normalizeApi(apiURL).url, token));
            } catch (Exception e) {
                results.add(new PingResult(provider.normalizeApi(apiURL).url, apiURL, provider.id, Long.MAX_VALUE, 0, false));
            }
        }

        results.sort(Comparator
                .comparing((PingResult r) -> !r.ok())
                .thenComparing(r -> -r.kbps())
                .thenComparingLong(PingResult::latencyMs));

        Map<String, Object> orderedRepositoryApiUrls = new HashMap<>();

        for (var pingResult : results) {
            orderedRepositoryApiUrls.put(pingResult.entryKey, repositoryApiUrls.get(pingResult.entryKey));
        }

        return orderedRepositoryApiUrls;
    }

    private static PingResult ping(Provider provider, String entryKey, String url, String token) {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);

            provider.setupRequest(connection);
            provider.authenticate(connection, token);

            long t0 = System.nanoTime();
            int code = connection.getResponseCode();
            long ttfb = (System.nanoTime() - t0) / 1_000_000;
            if (code != 200) return new PingResult(url, entryKey, provider.id, ttfb, 0, false);

            long bytes = 0;
            long tRead = System.nanoTime();
            try (InputStream in = connection.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while (bytes < SAMPLE_BYTES && (n = in.read(buf)) != -1) bytes += n;
            }

            double seconds = (System.nanoTime() - tRead) / 1e9;
            double kbps = seconds > 0 ? (bytes * 8 / 1000.0) / seconds : 0;

            return new PingResult(url, entryKey, provider.id, ttfb, kbps, true);
        } catch (Exception e) {
            return new PingResult(url, entryKey, provider.id, Long.MAX_VALUE, 0, false);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
