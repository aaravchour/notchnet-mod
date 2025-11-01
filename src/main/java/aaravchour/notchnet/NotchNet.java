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
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class NotchNet implements ModInitializer {

	private static final String MOD_ID = "notchnet";
	private static String API_BASE;
	private static final String MOD_API_KEY = "a_secret_key_for_your_mod"; // This should match the key in your auth server

	@Override
	public void onInitialize() {
		loadProperties();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				register(dispatcher)
		);
		System.out.println("[NotchNet] Loaded successfully!");
	}

	private void loadProperties() {
		Properties prop = new Properties();
		try (InputStream input = NotchNet.class.getClassLoader().getResourceAsStream("notchnet.properties")) {
			if (input == null) {
				System.out.println("Sorry, unable to find notchnet.properties");
				return;
			}
			prop.load(input);
			API_BASE = prop.getProperty("api.base.url");

			if (API_BASE == null) {
				System.out.println("Missing api.base.url in notchnet.properties");
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		}
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

											answer = answer.replace("\\n", "\n");

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
		String json = "{\"question\":\"" + question.replace("\"", "\\\"") + "\"}";

		HttpURLConnection conn = (HttpURLConnection) new URL(API_BASE + "/get-data").openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("X-Mod-Api-Key", MOD_API_KEY);
		conn.setDoOutput(true);

		try (OutputStream os = conn.getOutputStream()) {
			os.write(json.getBytes(StandardCharsets.UTF_8));
		}

		int code = conn.getResponseCode();
		String body = readResponse(conn);
		System.out.println("[NotchNet] /get-data response: " + code + " " + body);

		if (code != 200) {
			throw new IOException("Server returned " + code + ": " + body);
		}

		return parseJsonField(body, "answer");
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

	private static String parseJsonField(String json, String field) {
		try {
			String search = "\"" + field + "\":";
			int start = json.indexOf(search);
			if (start == -1) return null;
			int pos = start + search.length();

			while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;

			if (json.charAt(pos) == '"') {
				int firstQuote = pos + 1;
				int secondQuote = json.indexOf("\"", firstQuote);
				return json.substring(firstQuote, secondQuote);
			} else {
				int end = pos;
				while (end < json.length() && ",}]".indexOf(json.charAt(end)) == -1) end++;
				return json.substring(pos, end).trim();
			}
		} catch (Exception e) {
			return null;
		}
	}
}
