package aaravchour.notchnet;

import aaravchour.notchnet.common.NotchNetCore;
import aaravchour.notchnet.common.CoreConfig;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import java.io.IOException;

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
                sender.sendMessage(new TextComponentString("§6[NotchNet]§r Use §7/notchnet <question>§r."));
                return;
            }

            final String question = String.join(" ", args);
            sender.sendMessage(new TextComponentString("§6[NotchNet]§r Thinking..."));

            new Thread(() -> {
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
            }).start();
        }
    }
}
