package aaravchour.notchnet;

import aaravchour.notchnet.common.NotchNetCore;
import aaravchour.notchnet.common.CoreConfig;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

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
                .then(Commands.argument("question", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String question = StringArgumentType.getString(ctx, "question");
                        CommandSourceStack source = ctx.getSource();
                        source.sendSystemMessage(Component.literal("§6[NotchNet]§r Thinking..."));

                        new Thread(() -> {
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
                        }).start();
                        return 1;
                    })
                )
        );
    }
}
