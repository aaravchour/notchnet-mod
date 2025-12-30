package aaravchour.notchnet;

import aaravchour.notchnet.common.NotchNetCore;
import aaravchour.notchnet.common.CoreConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

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

		System.out.println("[NotchNet 1.20.1] Loaded successfully!");
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
			
			HttpURLConnection conn = (HttpURLConnection) new URL(CoreConfig.apiUrl + "/admin/detect-mods").openConnection();
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
								HttpURLConnection conn = (HttpURLConnection) new URL(CoreConfig.apiUrl + "/admin/reload-index").openConnection();
								conn.setRequestMethod("POST"); 
								conn.setConnectTimeout(2000);
								int code = conn.getResponseCode();
								s.sendFeedback(() -> Text.literal("§a✅ Connected to Backend! §7(Status: " + code + ")"), false);
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
								CoreConfig.apiUrl = NotchNetConfig.apiUrl;
								NotchNetConfig.saveConfig();
								ctx.getSource().sendFeedback(() -> Text.literal("§a✅ API URL updated to: §r" + NotchNetConfig.apiUrl), false);
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
								String answer = NotchNetCore.askQuestion(question);
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
}
