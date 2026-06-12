package aaravchour.notchnet;

import aaravchour.notchnet.common.CoreConfig;
import aaravchour.notchnet.common.NotchNetCore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.IOException;

@Mod("notchnet")
public class NotchNet {

    public NotchNet() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NotchNetConfig::loadConfig);
        System.out.println("[NotchNet 1.20.1 Forge] Loaded successfully!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("notchnet")
                .executes(ctx -> {
                    ctx.getSource().sendSystemMessage(Component.literal("§6[NotchNet]§r Use §7/notchnet help§r for commands."));
                    return 1;
                })
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        CommandSourceStack s = ctx.getSource();
                        s.sendSystemMessage(Component.literal("§6--- NotchNet Help ---"));
                        s.sendSystemMessage(Component.literal("§7/notchnet <question> §r- Ask the AI a question."));
                        s.sendSystemMessage(Component.literal("§7/notchnet status §r- Check backend connectivity."));
                        s.sendSystemMessage(Component.literal("§7/notchnet config §r- View/Change settings."));
                        return 1;
                    })
                )
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        CommandSourceStack s = ctx.getSource();
                        s.sendSystemMessage(Component.literal("§6[NotchNet]§r Checking connection..."));
                        NotchNetCore.submit(() -> {
                            try {
                                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(CoreConfig.apiUrl + "/admin/reload-index").openConnection();
                                conn.setRequestMethod("POST");
                                conn.setConnectTimeout(2000);
                                int code = conn.getResponseCode();
                                s.getServer().execute(() -> s.sendSystemMessage(Component.literal("§a✅ Connected to Backend! §7(Status: " + code + ")")));
                            } catch (Exception e) {
                                s.getServer().execute(() -> s.sendSystemMessage(Component.literal("§c❌ Connection Failed: §r" + e.getMessage())));
                            }
                        });
                        return 1;
                    })
                )
                .then(Commands.literal("config")
                    .executes(ctx -> {
                        CommandSourceStack s = ctx.getSource();
                        s.sendSystemMessage(Component.literal("§6--- NotchNet Config ---"));
                        s.sendSystemMessage(Component.literal("§7apiUrl: §r" + NotchNetConfig.apiUrl));
                        s.sendSystemMessage(Component.literal("§7autoScanMods: §r" + NotchNetConfig.autoScanMods));
                        return 1;
                    })
                    .then(Commands.literal("apiUrl")
                        .then(Commands.argument("url", StringArgumentType.string())
                            .executes(ctx -> {
                                String rawUrl = StringArgumentType.getString(ctx, "url");
                                NotchNetConfig.apiUrl = NotchNetConfig.fixUrl(rawUrl);
                                CoreConfig.apiUrl = NotchNetConfig.apiUrl;
                                NotchNetConfig.saveConfig();
                                ctx.getSource().sendSystemMessage(Component.literal("§a✅ API URL updated to: §r" + NotchNetConfig.apiUrl));
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.argument("question", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String question = StringArgumentType.getString(ctx, "question");
                        CommandSourceStack source = ctx.getSource();
                        source.sendSystemMessage(Component.literal("§6[NotchNet]§r Thinking..."));

                        NotchNetCore.submit(() -> {
                            try {
                                String answer = NotchNetCore.askQuestion(question);
                                source.getServer().execute(() -> {
                                    source.sendSystemMessage(Component.literal("§b--- Answer ---"));
                                    for (String line : answer.split("\n")) {
                                        source.sendSystemMessage(Component.literal("§f" + line));
                                    }
                                });
                            } catch (IOException e) {
                                source.getServer().execute(() ->
                                    source.sendSystemMessage(Component.literal("§c⚠️ Error: §r" + e.getMessage()))
                                );
                            }
                        });
                        return 1;
                    })
                )
        );
    }
}
