package ladysnake.requiem.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ladysnake.requiem.Requiem;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Asynchronous chat translation for Requiem speech bubbles.
 *
 * Inherent / built-in translation requires zero setup or external services out of the box,
 * featuring redundant fallback endpoints (Google Chrome Translation API, Google Web, and MyMemory).
 * Servers can also optionally point at a private LibreTranslate instance in config/requiem.json.
 */
public final class SpeechTranslationService {
    private static final String BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(
        2, 2, 30L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64),
        runnable -> {
            Thread thread = new Thread(runnable, "Requiem-Speech-Translation");
            thread.setDaemon(true);
            return thread;
        },
        new ThreadPoolExecutor.AbortPolicy()
    );

    private static final Map<String, String> CACHE = new LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 512;
        }
    };
    private static final Map<String, CompletableFuture<String>> IN_FLIGHT = new ConcurrentHashMap<>();
    private static final AtomicLong LAST_FAILURE_LOG = new AtomicLong();

    private SpeechTranslationService() { }

    public static CompletableFuture<String> translate(String text, String requestedTargetLanguage) {
        if (text == null || text.isBlank()) return CompletableFuture.completedFuture(text == null ? "" : text);
        if (!RequiemConfig.speech.translateBubbles) return CompletableFuture.completedFuture(text);

        String target = normalizeTargetLanguage(requestedTargetLanguage);
        String provider = "libretranslate".equalsIgnoreCase(RequiemConfig.speech.translationProvider)
            ? "libretranslate" : "google";
        String endpoint = provider.equals("libretranslate")
            ? safeEndpointKey(RequiemConfig.speech.libreTranslateUrl) : "google";
        String key = provider + "|" + endpoint + "|" + target + "|" + text;

        synchronized (CACHE) {
            String cached = CACHE.get(key);
            if (cached != null) return CompletableFuture.completedFuture(cached);
        }

        CompletableFuture<String> inFlight = IN_FLIGHT.get(key);
        if (inFlight != null) return inFlight;

        CompletableFuture<String> created;
        try {
            created = CompletableFuture.supplyAsync(() -> translateBlocking(text, target, provider), WORKERS);
        } catch (RejectedExecutionException rejected) {
            // Never run HTTP work on the server thread when the bounded queue is full.
            return CompletableFuture.completedFuture(text);
        }

        CompletableFuture<String> previous = IN_FLIGHT.putIfAbsent(key, created);
        if (previous != null) {
            created.cancel(false);
            return previous;
        }

        created.whenComplete((translated, error) -> {
            if (error == null && translated != null && !translated.isBlank() && !translated.equals(text)) {
                synchronized (CACHE) {
                    CACHE.put(key, translated);
                }
            }
            IN_FLIGHT.remove(key, created);
        });
        return created;
    }

    public static String normalizeTargetLanguage(String language) {
        if (language == null || language.isBlank()) return "en";
        String normalized = language.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        int regionSeparator = normalized.indexOf('-');
        if (regionSeparator >= 0) normalized = normalized.substring(0, regionSeparator);
        if (!normalized.matches("[a-z]{2,3}")) return "en";
        return switch (normalized) {
            case "iw" -> "he";
            case "in" -> "id";
            case "ji" -> "yi";
            default -> normalized;
        };
    }

    private static String translateBlocking(String text, String target, String provider) {
        try {
            int timeout = Math.max(2, Math.min(15, RequiemConfig.speech.timeoutSeconds));
            String translated = provider.equals("libretranslate")
                ? translateLibreTranslate(text, target, timeout)
                : translateGoogle(text, target, timeout);
            return translated == null || translated.isBlank() ? text : translated.strip();
        } catch (Exception exception) {
            long now = System.currentTimeMillis();
            long previous = LAST_FAILURE_LOG.get();
            if (now - previous > 60_000L && LAST_FAILURE_LOG.compareAndSet(previous, now)) {
                Requiem.LOGGER.warn("[Requiem] Speech translation provider '{}' failed: {} ({})",
                    provider, exception.getMessage(), exception.getClass().getSimpleName());
            }
            return text;
        }
    }

    private static String translateGoogle(String text, String target, int timeoutSeconds) throws IOException, InterruptedException {
        // Stage 1: Google Chrome official extension translation API
        try {
            return translateClients5(text, target, timeoutSeconds);
        } catch (Exception primaryError) {
            // Stage 2: Google web translation API
            try {
                return translateGoogleWeb(text, target, timeoutSeconds);
            } catch (Exception secondaryError) {
                // Stage 3: MyMemory free translation API
                return translateMyMemory(text, target, timeoutSeconds);
            }
        }
    }

    private static String translateClients5(String text, String target, int timeoutSeconds) throws IOException, InterruptedException {
        String query = "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=auto&tl="
            + URLEncoder.encode(target, StandardCharsets.UTF_8)
            + "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(query))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "*/*")
            .GET()
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Clients5 translation HTTP " + response.statusCode());
        }
        return parseClients5Response(response.body());
    }

    static String parseClients5Response(String body) throws IOException {
        try {
            JsonElement root = JsonParser.parseString(body);
            if (root.isJsonArray()) {
                JsonArray outer = root.getAsJsonArray();
                if (!outer.isEmpty() && outer.get(0).isJsonArray()) {
                    JsonArray inner = outer.get(0).getAsJsonArray();
                    if (!inner.isEmpty() && inner.get(0).isJsonPrimitive()) {
                        String res = inner.get(0).getAsString().strip();
                        if (!res.isEmpty()) return res;
                    }
                } else if (!outer.isEmpty() && outer.get(0).isJsonPrimitive()) {
                    String res = outer.get(0).getAsString().strip();
                    if (!res.isEmpty()) return res;
                }
            } else if (root.isJsonPrimitive()) {
                String res = root.getAsString().strip();
                if (!res.isEmpty()) return res;
            }
            throw new IOException("Unexpected clients5 format");
        } catch (Exception e) {
            throw new IOException("Malformed clients5 translation response", e);
        }
    }

    private static String translateGoogleWeb(String text, String target, int timeoutSeconds) throws IOException, InterruptedException {
        String query = "https://translate.googleapis.com/translate_a/single"
            + "?client=gtx&sl=auto&tl=" + URLEncoder.encode(target, StandardCharsets.UTF_8)
            + "&dt=t&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(query))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "application/json")
            .GET()
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Google translation HTTP " + response.statusCode());
        }
        return parseGoogleResponse(response.body());
    }

    static String parseGoogleResponse(String body) throws IOException {
        try {
            JsonElement rootElement = JsonParser.parseString(body);
            if (!rootElement.isJsonArray() || rootElement.getAsJsonArray().isEmpty()) {
                throw new IOException("Unexpected Google translation response");
            }
            JsonElement chunksElement = rootElement.getAsJsonArray().get(0);
            if (!chunksElement.isJsonArray()) throw new IOException("Missing translation chunks");

            StringBuilder result = new StringBuilder();
            JsonArray chunks = chunksElement.getAsJsonArray();
            for (JsonElement chunk : chunks) {
                if (!chunk.isJsonArray() || chunk.getAsJsonArray().isEmpty()) continue;
                JsonElement translation = chunk.getAsJsonArray().get(0);
                if (translation != null && translation.isJsonPrimitive()) {
                    result.append(translation.getAsString());
                }
            }
            String translated = result.toString().strip();
            if (translated.isEmpty()) throw new IOException("Translation was empty");
            return translated;
        } catch (IllegalStateException | UnsupportedOperationException exception) {
            throw new IOException("Malformed Google translation response", exception);
        }
    }

    private static String translateMyMemory(String text, String target, int timeoutSeconds) throws IOException, InterruptedException {
        String query = "https://api.mymemory.translated.net/get?langpair=autodetect|"
            + URLEncoder.encode(target, StandardCharsets.UTF_8)
            + "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(query))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "application/json")
            .GET()
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("MyMemory translation HTTP " + response.statusCode());
        }
        return parseMyMemoryResponse(response.body());
    }

    static String parseMyMemoryResponse(String body) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonObject responseData = root.getAsJsonObject("responseData");
            if (responseData != null && responseData.has("translatedText")) {
                String translated = responseData.get("translatedText").getAsString().strip();
                if (!translated.isEmpty()) return translated;
            }
            throw new IOException("Missing translatedText in MyMemory response");
        } catch (Exception e) {
            throw new IOException("Malformed MyMemory translation response", e);
        }
    }

    private static String translateLibreTranslate(String text, String target, int timeoutSeconds)
            throws IOException, InterruptedException {
        String base = RequiemConfig.speech.libreTranslateUrl == null
            ? "" : RequiemConfig.speech.libreTranslateUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.isBlank()) throw new IOException("LibreTranslate URL is empty");
        String endpoint = base.endsWith("/translate") ? base : base + "/translate";
        URI uri = URI.create(endpoint);
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("LibreTranslate URL must use HTTP or HTTPS");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("q", text);
        payload.addProperty("source", "auto");
        payload.addProperty("target", target);
        payload.addProperty("format", "text");
        String apiKey = RequiemConfig.speech.libreTranslateApiKey;
        if (apiKey != null && !apiKey.isBlank()) payload.addProperty("api_key", apiKey);

        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("LibreTranslate HTTP " + response.statusCode());
        }
        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String translated = json.get("translatedText").getAsString();
            if (translated == null || translated.isBlank()) throw new IOException("LibreTranslate returned no text");
            return translated;
        } catch (IllegalStateException | NullPointerException exception) {
            throw new IOException("Malformed LibreTranslate response", exception);
        }
    }

    private static String safeEndpointKey(String endpoint) {
        if (endpoint == null) return "";
        return endpoint.trim();
    }
}
