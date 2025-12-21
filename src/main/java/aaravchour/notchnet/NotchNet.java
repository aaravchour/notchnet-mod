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
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotchNet implements ModInitializer {

	@Override
	public void onInitialize() {
		NotchNetConfig.loadConfig();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			registerCommands(dispatcher);
		});
		
		if (NotchNetConfig.autoScanMods) {
			CompletableFuture.runAsync(this::scanAndSendMods);
		}

		System.out.println("[NotchNet] Loaded successfully!");
	}
	
	private void scanAndSendMods() {
		try {
			java.util.Collection<net.fabricmc.loader.api.ModContainer> mods = net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods();
			com.google.gson.JsonArray modNames = new com.google.gson.JsonArray();
			for (net.fabricmc.loader.api.ModContainer mod : mods) {
				modNames.add(mod.getMetadata().getName());
			}
			
			JsonObject json = new JsonObject();
			json.add("mods", modNames);
			
			HttpURLConnection conn = (HttpURLConnection) new URL(NotchNetConfig.apiUrl + "/admin/detect-mods").openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);
			
			try (OutputStream os = conn.getOutputStream()) {
				os.write(new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
			}
			conn.getResponseCode();
		} catch (Exception e) {
			System.err.println("[NotchNet] Failed to auto-detect mods: " + e.getMessage());
		}
	}

	private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal("notchnet")
				.executes(ctx -> {
					ctx.getSource().sendFeedback(() -> Text.literal("§6[NotchNet]§r Use §7/notchnet help§r for commands."), false);
					return 1;
				})
				.then(CommandManager.literal("help")
					.executes(ctx -> {
						ServerCommandSource s = ctx.getSource();
						s.sendFeedback(() -> Text.literal("§6--- NotchNet Help ---"), false);
						s.sendFeedback(() -> Text.literal("§7/notchnet <question> §r- Ask the AI a question."), false);
						s.sendFeedback(() -> Text.literal("§7/notchnet status §r- Check backend connectivity."), false);
						s.sendFeedback(() -> Text.literal("§7/notchnet config §r- View/Change settings."), false);
						return 1;
					})
				)
				.then(CommandManager.literal("status")
					.executes(ctx -> {
						ServerCommandSource s = ctx.getSource();
						s.sendFeedback(() -> Text.literal("§6[NotchNet]§r Checking connection..."), false);
						CompletableFuture.runAsync(() -> {
							try {
								HttpURLConnection conn = (HttpURLConnection) new URL(NotchNetConfig.apiUrl + "/admin/reload-index").openConnection();
								conn.setRequestMethod("POST"); 
								conn.setConnectTimeout(2000);
								int code = conn.getResponseCode();
								s.sendFeedback(() -> Text.literal("§a✅ Connected to Backend! §7(Status: " + code + ")"), false);
								s.sendFeedback(() -> Text.literal("§7API URL: " + NotchNetConfig.apiUrl), false);
							} catch (Exception e) {
								s.sendFeedback(() -> Text.literal("§c❌ Connection Failed: §r" + e.getMessage()), false);
							}
						});
						return 1;
					})
				)
				.then(CommandManager.literal("config")
					.executes(ctx -> {
						ServerCommandSource s = ctx.getSource();
						s.sendFeedback(() -> Text.literal("§6--- NotchNet Config ---"), false);
						s.sendFeedback(() -> Text.literal("§7apiUrl: §r" + NotchNetConfig.apiUrl), false);
						s.sendFeedback(() -> Text.literal("§7autoScanMods: §r" + NotchNetConfig.autoScanMods), false);
						return 1;
					})
					.then(CommandManager.literal("apiUrl")
						.then(CommandManager.argument("url", StringArgumentType.string())
							.executes(ctx -> {
								String rawUrl = StringArgumentType.getString(ctx, "url");
								NotchNetConfig.apiUrl = NotchNetConfig.fixUrl(rawUrl);
								NotchNetConfig.saveConfig();
								ctx.getSource().sendFeedback(() -> Text.literal("§a✅ API URL updated to: §r" + NotchNetConfig.apiUrl), false);
								if (!NotchNetConfig.apiUrl.equals(rawUrl) && !NotchNetConfig.apiUrl.equals("http://" + rawUrl)) {
									ctx.getSource().sendFeedback(() -> Text.literal("§7(Automatically corrected for protocol/port/host)"), false);
								}
								return 1;
							})
						)
					)
					.then(CommandManager.literal("autoScanMods")
						.then(CommandManager.argument("value", StringArgumentType.string())
							.executes(ctx -> {
								String val = StringArgumentType.getString(ctx, "value");
								NotchNetConfig.autoScanMods = parseBoolean(val);
								NotchNetConfig.saveConfig();
								ctx.getSource().sendFeedback(() -> Text.literal("§a✅ AutoScanMods updated to: §r" + NotchNetConfig.autoScanMods), false);
								return 1;
							})
						)
					)
				)
				.then(CommandManager.argument("question", StringArgumentType.greedyString())
					.executes(ctx -> {
						String question = StringArgumentType.getString(ctx, "question");
						ServerCommandSource source = ctx.getSource();
						source.sendFeedback(() -> Text.literal("§6[NotchNet]§r Thinking..."), false);

						CompletableFuture.runAsync(() -> {
							try {
								String answer = askQuestion(question);
								answer = answer.replace("\\n", "\n");
								source.sendFeedback(() -> Text.literal("§b--- Answer ---"), false);
								for (String line : answer.split("\n")) {
									source.sendFeedback(() -> Text.literal("§f" + line), false);
								}
							} catch (Exception e) {
								source.sendFeedback(() -> Text.literal("§c⚠️ Error: §r" + e.getMessage()), false);
							}
						});
						return 1;
					})
				)
		);
	}

	private static String askQuestion(String question) throws IOException {
		JsonObject json = new JsonObject();
		json.addProperty("question", question);

		HttpURLConnection conn = (HttpURLConnection) new URL(NotchNetConfig.apiUrl + "/ask").openConnection();
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
		System.out.println("[NotchNet] /ask response: " + code + " " + body);

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

	private static boolean parseBoolean(String value) {
		if (value == null) return false;
		String v = value.toLowerCase().trim();
		return v.equals("true") || v.equals("1") || v.equals("on") || v.equals("yes");
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
