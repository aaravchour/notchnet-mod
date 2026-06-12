package aaravchour.notchnet;

import aaravchour.notchnet.common.CoreConfig;
import aaravchour.notchnet.common.NotchNetCore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
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
        NotchNetConfig.loadConfig();
        System.out.println("[NotchNet 1.16.5 Forge] Loaded successfully!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    private void registerCommands(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            Commands.literal("notchnet")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(new StringTextComponent("§6[NotchNet]§r Use §7/notchnet help§r for commands."), false);
                    return 1;
                })
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        CommandSource s = ctx.getSource();
                        s.sendSuccess(new StringTextComponent("§6--- NotchNet Help ---"), false);
                        s.sendSuccess(new StringTextComponent("§7/notchnet <question> §r- Ask the AI a question."), false);
                        s.sendSuccess(new StringTextComponent("§7/notchnet status §r- Check backend connectivity."), false);
                        s.sendSuccess(new StringTextComponent("§7/notchnet config §r- View/Change settings."), false);
                        return 1;
                    })
                )
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        CommandSource s = ctx.getSource();
                        s.sendSuccess(new StringTextComponent("§6[NotchNet]§r Checking connection..."), false);
                        NotchNetCore.submit(() -> {
                            try {
                                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(CoreConfig.apiUrl + "/admin/reload-index").openConnection();
                                conn.setRequestMethod("POST");
                                conn.setConnectTimeout(2000);
                                int code = conn.getResponseCode();
                                s.getServer().submit(() -> s.sendSuccess(new StringTextComponent("§a✅ Connected to Backend! §7(Status: " + code + ")"), false));
                            } catch (Exception e) {
                                s.getServer().submit(() -> s.sendSuccess(new StringTextComponent("§c❌ Connection Failed: §r" + e.getMessage()), false));
                            }
                        });
                        return 1;
                    })
                )
                .then(Commands.literal("config")
                    .executes(ctx -> {
                        CommandSource s = ctx.getSource();
                        s.sendSuccess(new StringTextComponent("§6--- NotchNet Config ---"), false);
                        s.sendSuccess(new StringTextComponent("§7apiUrl: §r" + NotchNetConfig.apiUrl), false);
                        s.sendSuccess(new StringTextComponent("§7autoScanMods: §r" + NotchNetConfig.autoScanMods), false);
                        return 1;
                    })
                    .then(Commands.literal("apiUrl")
                        .then(Commands.argument("url", StringArgumentType.string())
                            .executes(ctx -> {
                                String rawUrl = StringArgumentType.getString(ctx, "url");
                                NotchNetConfig.apiUrl = NotchNetConfig.fixUrl(rawUrl);
                                CoreConfig.apiUrl = NotchNetConfig.apiUrl;
                                NotchNetConfig.saveConfig();
                                ctx.getSource().sendSuccess(new StringTextComponent("§a✅ API URL updated to: §r" + NotchNetConfig.apiUrl), false);
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.argument("question", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String question = StringArgumentType.getString(ctx, "question");
                        CommandSource source = ctx.getSource();
                        source.sendSuccess(new StringTextComponent("§6[NotchNet]§r Thinking..."), false);

                        NotchNetCore.submit(() -> {
                            try {
                                String answer = NotchNetCore.askQuestion(question);
                                source.getServer().submit(() -> {
                                    source.sendSuccess(new StringTextComponent("§b--- Answer ---"), false);
                                    for (String line : answer.split("\n")) {
                                        source.sendSuccess(new StringTextComponent("§f" + line), false);
                                    }
                                });
                            } catch (IOException e) {
                                source.getServer().submit(() ->
                                    source.sendSuccess(new StringTextComponent("§c⚠️ Error: §r" + e.getMessage()), false)
                                );
                            }
                        });
                        return 1;
                    })
                )
        );
    }
}
