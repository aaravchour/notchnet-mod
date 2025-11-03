package aaravchour.notchnet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotchNet implements ModInitializer {

	private static final String MOD_ID = "notchnet";
	private static String API_BASE;



	@Override
	public void onInitialize() {
		loadProperties();
		NotchNetConfig.loadConfig();

		// Fabric 1.21+ uses v2 CommandRegistrationCallback with (dispatcher, registryAccess, environment)
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			registerCommand(dispatcher);
		});

		System.out.println("[NotchNet] Loaded successfully!");
	}

	private static void loadProperties() {
		Properties prop = new Properties();
		try (InputStream input = NotchNet.class.getClassLoader().getResourceAsStream("notchnet.properties")) {
			if (input == null) {
				System.err.println("[NotchNet] Could not find notchnet.properties in resources/");
				return;
			}
			prop.load(input);
			API_BASE = prop.getProperty("api.base.url");
			if (API_BASE == null || API_BASE.isEmpty()) {
				System.err.println("[NotchNet] Missing api.base.url in notchnet.properties");
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
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
		if (API_BASE == null || API_BASE.isBlank()) {
			throw new IOException("API base URL is not configured");
		}

		if (NotchNetConfig.token == null || NotchNetConfig.token.isEmpty()) {
			throw new IOException("You must be signed in to use NotchNet. Use /notchnetsignin to sign in.");
		}

		JsonObject json = new JsonObject();
		json.addProperty("question", question);
		json.addProperty("token", NotchNetConfig.token);

		HttpURLConnection conn = (HttpURLConnection) new URL(API_BASE + "/get-data").openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);
		conn.setConnectTimeout(10_000);
		conn.setReadTimeout(15_000);

		try (OutputStream os = conn.getOutputStream()) {
			os.write(new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
		}

		int code = conn.getResponseCode();
		String body = readResponse(conn);
		System.out.println("[NotchNet] /get-data response: " + code + " " + body);

		if (code != 200) {
			throw new IOException("Server returned " + code + ": " + body);
		}

		String answer = parseJsonField(body, "answer");
		if (answer == null) {
			throw new IOException("No 'answer' field in server response: " + body);
		}
		return answer;
	}

	private static String readResponse(HttpURLConnection conn) throws IOException {
		InputStream stream = (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300)
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

	private static String parseJsonField(String json, String field) {
		if (json == null) return null;

		Pattern strPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
		Matcher m = strPattern.matcher(json);
		if (m.find()) {
			return unescapeJsonString(m.group(1));
		}

		Pattern nonStrPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*([^,}\\]]+)");
		m = nonStrPattern.matcher(json);
		if (m.find()) {
			return m.group(1).trim();
		}
		return null;
	}

	private static String unescapeJsonString(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\' && i + 1 < s.length()) {
				char next = s.charAt(i + 1);
				switch (next) {
					case '"': sb.append('"'); i++; break;
					case '\\': sb.append('\\'); i++; break;
					case '/': sb.append('/'); i++; break;
					case 'b': sb.append('\b'); i++; break;
					case 'f': sb.append('\f'); i++; break;
					case 'n': sb.append('\n'); i++; break;
					case 'r': sb.append('\r'); i++; break;
					case 't': sb.append('\t'); i++; break;
					case 'u':
						if (i + 5 < s.length()) {
							String hex = s.substring(i + 2, i + 6);
							try {
								sb.append((char) Integer.parseInt(hex, 16));
								i += 5;
							} catch (NumberFormatException ex) {
								sb.append("\\u");
								i++;
							}
						} else {
							sb.append("\\u");
							i++;
						}
						break;
					default:
						sb.append(next);
						i++;
						break;
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
