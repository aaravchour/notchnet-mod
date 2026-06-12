package aaravchour.notchnet.common;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NotchNetCore {

    private static final Gson GSON = new Gson();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(NotchNetCore::shutdownExecutor));
    }

    // Retry configuration
    private static final int MAX_RETRIES = 1;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int STREAM_READ_TIMEOUT_MS = 120_000; // cap from infinite

    public static void submit(Runnable task) {
        EXECUTOR.submit(task);
    }

    public static void shutdownExecutor() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static String askQuestion(String question) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("question", question);
        String requestBody = GSON.toJson(json);

        IOException lastError = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                long backoff = INITIAL_BACKOFF_MS * attempt;
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }
            }

            HttpURLConnection conn = null;
            try {
                conn = openConnection(CoreConfig.apiUrl + "/ask", false);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                String body = readResponse(conn);

                if (code == 200) {
                    JsonObject resp = JsonParser.parseString(body).getAsJsonObject();
                    JsonElement answerEl = resp.get("answer");
                    if (answerEl == null || answerEl.isJsonNull()) {
                        throw new IOException("No 'answer' field in server response: " + body);
                    }
                    return answerEl.getAsString().replace("\\n", "\n");
                }

                if (shouldRetry(code)) {
                    lastError = new IOException("Server returned " + code + ": " + body);
                    continue;
                }
                throw new IOException("Server returned " + code + ": " + body);
            } catch (SocketTimeoutException e) {
                lastError = new IOException("Connection timed out", e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        throw lastError != null ? lastError : new IOException("Request failed after retries");
    }

    private static HttpURLConnection openConnection(String urlString, boolean isStream) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(isStream ? STREAM_READ_TIMEOUT_MS : READ_TIMEOUT_MS);
        return conn;
    }

    private static boolean shouldRetry(int code) {
        return code == 429 || code == 502 || code == 503 || code == 504;
    }

    public static String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public interface StreamCallback {
        void onToken(String token);
        void onError(String error);
        void onDone();
    }

    public static void askQuestionStream(String question, StreamCallback callback) {
        EXECUTOR.submit(() -> {
            try {
                processStream(question, callback);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    private static void processStream(String question, StreamCallback callback) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("question", question);
        String requestBody = GSON.toJson(json);

        IOException lastError = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                long backoff = INITIAL_BACKOFF_MS * attempt;
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }
            }

            HttpURLConnection conn = null;
            try {
                conn = openConnection(CoreConfig.apiUrl + "/ask/stream", true);
                conn.setRequestProperty("Accept", "text/event-stream");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code != 200) {
                    if (shouldRetry(code)) {
                        lastError = new IOException("Server returned " + code);
                        continue;
                    }
                    callback.onError("Server returned " + code);
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) {
                                callback.onDone();
                                return;
                            }

                            JsonObject payload = JsonParser.parseString(data).getAsJsonObject();
                            JsonElement answerEl = payload.get("answer");
                            if (answerEl != null && !answerEl.isJsonNull()) {
                                callback.onToken(answerEl.getAsString().replace("\\n", "\n"));
                            } else {
                                JsonElement errorEl = payload.get("error");
                                if (errorEl != null && !errorEl.isJsonNull()) {
                                    callback.onError(errorEl.getAsString());
                                    return;
                                }
                            }
                        }
                    }
                }
                callback.onDone();
                return;
            } catch (SocketTimeoutException e) {
                lastError = new IOException("Stream timed out", e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        callback.onError(lastError != null ? lastError.getMessage() : "Request failed after retries");
    }
}
