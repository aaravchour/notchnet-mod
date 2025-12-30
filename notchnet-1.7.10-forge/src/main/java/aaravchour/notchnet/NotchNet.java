package aaravchour.notchnet;

import aaravchour.notchnet.common.NotchNetCore;
import aaravchour.notchnet.common.CoreConfig;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.io.IOException;

@Mod(modid = "notchnet", name = "NotchNet", version = "1.0.0", useMetadata = true)
public class NotchNet {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NotchNetConfig.loadConfig();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.out.println("[NotchNet 1.7.10] Loaded successfully!");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandNotchNet());
    }

    public static class CommandNotchNet extends CommandBase {
        @Override
        public String getCommandName() { return "notchnet"; }

        @Override
        public String getCommandUsage(ICommandSender sender) { return "/notchnet <question>"; }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length == 0) {
                sender.addChatMessage(new ChatComponentText("§6[NotchNet]§r Use §7/notchnet <question>§r."));
                return;
            }

            final String question = "";
            StringBuilder sb = new StringBuilder();
            for(String s : args) sb.append(s).append(" ");
            final String q = sb.toString().trim();
            
            sender.addChatMessage(new ChatComponentText("§6[NotchNet]§r Thinking..."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String answer = NotchNetCore.askQuestion(q);
                        String[] lines = answer.split("\n");
                        sender.addChatMessage(new ChatComponentText("§b--- Answer ---"));
                        for (String line : lines) {
                            sender.addChatMessage(new ChatComponentText("§f" + line));
                        }
                    } catch (IOException e) {
                        sender.addChatMessage(new ChatComponentText("§c⚠️ Error: §r" + e.getMessage()));
                    }
                }
            }).start();
        }
        
        @Override
        public int getRequiredPermissionLevel() { return 0; }
    }
}
