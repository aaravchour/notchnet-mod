package aaravchour.notchnet;

import aaravchour.notchnet.common.NotchNetCore;
import aaravchour.notchnet.common.CoreConfig;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

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
                .then(Commands.argument("question", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String question = StringArgumentType.getString(ctx, "question");
                        CommandSource source = ctx.getSource();
                        source.sendSuccess(new StringTextComponent("§6[NotchNet]§r Thinking..."), false);

                        new Thread(() -> {
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
                        }).start();
                        return 1;
                    })
                )
        );
    }
}
