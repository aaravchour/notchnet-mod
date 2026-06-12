package aaravchour.notchnet;

import aaravchour.notchnet.common.CoreConfig;
import aaravchour.notchnet.common.NotchNetCore;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import java.io.IOException;
import java.util.Arrays;

@Mod(modid = "notchnet", name = "NotchNet", version = "1.0.0", useMetadata = true)
public class NotchNet {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NotchNetConfig.loadConfig();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.out.println("[NotchNet 1.12.2] Loaded successfully!");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandNotchNet());
    }

    public static class CommandNotchNet extends CommandBase {
        @Override
        public String getName() { return "notchnet"; }

        @Override
        public String getUsage(ICommandSender sender) { return "/notchnet <question>"; }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            if (args.length == 0) {
                sender.sendMessage(new TextComponentString("§6[NotchNet]§r Use §7/notchnet help§r for commands."));
                return;
            }

            String sub = args[0].toLowerCase();
            if (sub.equals("help")) {
                sender.sendMessage(new TextComponentString("§6--- NotchNet Help ---"));
                sender.sendMessage(new TextComponentString("§7/notchnet <question> §r- Ask the AI a question."));
                sender.sendMessage(new TextComponentString("§7/notchnet status §r- Check backend connectivity."));
                sender.sendMessage(new TextComponentString("§7/notchnet config §r- View/Change settings."));
                return;
            }

            if (sub.equals("status")) {
                sender.sendMessage(new TextComponentString("§6[NotchNet]§r Checking connection..."));
                NotchNetCore.submit(() -> {
                    try {
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(CoreConfig.apiUrl + "/admin/reload-index").openConnection();
                        conn.setRequestMethod("POST");
                        conn.setConnectTimeout(2000);
                        int code = conn.getResponseCode();
                        server.addScheduledTask(() -> sender.sendMessage(new TextComponentString("§a✅ Connected to Backend! §7(Status: " + code + ")")));
                    } catch (Exception e) {
                        server.addScheduledTask(() -> sender.sendMessage(new TextComponentString("§c❌ Connection Failed: §r" + e.getMessage())));
                    }
                });
                return;
            }

            if (sub.equals("config")) {
                if (args.length > 2 && args[1].equalsIgnoreCase("apiUrl")) {
                    NotchNetConfig.apiUrl = NotchNetConfig.fixUrl(args[2]);
                    CoreConfig.apiUrl = NotchNetConfig.apiUrl;
                    NotchNetConfig.saveConfig();
                    sender.sendMessage(new TextComponentString("§a✅ API URL updated to: §r" + NotchNetConfig.apiUrl));
                    return;
                }
                sender.sendMessage(new TextComponentString("§6--- NotchNet Config ---"));
                sender.sendMessage(new TextComponentString("§7apiUrl: §r" + NotchNetConfig.apiUrl));
                sender.sendMessage(new TextComponentString("§7autoScanMods: §r" + NotchNetConfig.autoScanMods));
                return;
            }

            // Treat as question
            final String question = String.join(" ", args);
            sender.sendMessage(new TextComponentString("§6[NotchNet]§r Thinking..."));

            NotchNetCore.submit(() -> {
                try {
                    String answer = NotchNetCore.askQuestion(question);
                    server.addScheduledTask(() -> {
                        sender.sendMessage(new TextComponentString("§b--- Answer ---"));
                        for (String line : answer.split("\n")) {
                            sender.sendMessage(new TextComponentString("§f" + line));
                        }
                    });
                } catch (IOException e) {
                    server.addScheduledTask(() ->
                        sender.sendMessage(new TextComponentString("§c⚠️ Error: §r" + e.getMessage()))
                    );
                }
            });
        }
    }
}
