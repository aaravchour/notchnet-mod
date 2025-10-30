package aaravchour.notchnet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class NotchNet implements ModInitializer {

	private static final String MOD_ID = "notchnet";
	private static final String API_BASE = "http://localhost:8000"; // Flask server
	private static final String ENCODED_KEY = "TVlfUkVBTF9BUElfS0VZ"; // Base64 of your real API key

	// Cached token info
	private static String cachedToken = null;
	private static Instant tokenExpiry = Instant.EPOCH;

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				register(dispatcher)
		);
		System.out.println("[NotchNet] Loaded successfully!");
	}

	private void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				CommandManager.literal("notchnet")
						.then(CommandManager.argument("question", StringArgumentType.greedyString())
								.executes(ctx -> {
									String question = StringArgumentType.getString(ctx, "question");
									ServerCommandSource source = ctx.getSource();

									source.sendFeedback(() -> Text.literal("🤖 NotchNet is thinking..."), false);

									CompletableFuture.runAsync(() -> {
										try {
											String answer = askQuestion(question);

											// Replace escaped \n with actual newlines
											answer = answer.replace("\\n", "\n");

											// Send each line separately for proper Minecraft chat display
											for (String line : answer.split("\n")) {
												source.sendFeedback(() -> Text.literal("🧠 " + line), false);
											}
										} catch (Exception e) {
											source.sendFeedback(() -> Text.literal("⚠️ Error: " + e.getMessage()), false);
											e.printStackTrace();
										}
									});

									return 1;
								})
						)
		);
	}

	private static String askQuestion(String question) throws IOException {
		String token = getValidToken();
		if (token == null) throw new IOException("Failed to obtain token");

		String json = "{\"question\":\"" + question.replace("\"", "\\\"") + "\"}";

		HttpURLConnection conn = (HttpURLConnection) new URL(API_BASE + "/ask").openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + token);
		conn.setDoOutput(true);

		try (OutputStream os = conn.getOutputStream()) {
			os.write(json.getBytes(StandardCharsets.UTF_8));
		}

		int code = conn.getResponseCode();
		String body = readResponse(conn);
		System.out.println("[NotchNet] /ask response: " + code + " " + body);

		if (code != 200) {
			throw new IOException("Server returned " + code + ": " + body);
		}

		return parseJsonField(body, "answer");
	}

	private static String getValidToken() throws IOException {
		if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
			return cachedToken;
		}

		String apiKey = new String(Base64.getDecoder().decode(ENCODED_KEY), StandardCharsets.UTF_8);
		System.out.println("[NotchNet] Using API key: " + apiKey);

		HttpURLConnection conn = (HttpURLConnection) new URL(API_BASE + "/get_token").openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("X-API-Key", apiKey);
		conn.setDoOutput(true);
		conn.getOutputStream().close(); // Empty POST

		int code = conn.getResponseCode();
		String body = readResponse(conn);
		System.out.println("[NotchNet] /get_token response: " + code + " " + body);

		if (code != 200) {
			throw new IOException("Failed to get token: " + body);
		}

		String tokenStr = parseJsonField(body, "token");
		String expiresStr = parseJsonField(body, "expires_in");
		if (tokenStr == null || expiresStr == null) {
			throw new IOException("Invalid token response: " + body);
		}

		cachedToken = tokenStr;
		int expires = Integer.parseInt(expiresStr.replaceAll("\\D", "")); // safe parse
		tokenExpiry = Instant.now().plusSeconds(expires - 10);
		System.out.println("[NotchNet] Cached token valid for " + expires + "s");

		return cachedToken;
	}

	private static String readResponse(HttpURLConnection conn) throws IOException {
		InputStream stream = (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300)
				? conn.getInputStream()
				: conn.getErrorStream();

		if (stream == null) return "";
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			return response.toString();
		}
	}

	// ✅ Fixed parser that supports quoted and unquoted JSON fields
	private static String parseJsonField(String json, String field) {
		try {
			String search = "\"" + field + "\":";
			int start = json.indexOf(search);
			if (start == -1) return null;
			int pos = start + search.length();

			// Skip whitespace
			while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;

			// Handle quoted string values
			if (json.charAt(pos) == '"') {
				int firstQuote = pos + 1;
				int secondQuote = json.indexOf("\"", firstQuote);
				return json.substring(firstQuote, secondQuote);
			} else {
				// Handle numeric / boolean / null
				int end = pos;
				while (end < json.length() && ",}]".indexOf(json.charAt(end)) == -1) end++;
				return json.substring(pos, end).trim();
			}
		} catch (Exception e) {
			return null;
		}
	}
}
