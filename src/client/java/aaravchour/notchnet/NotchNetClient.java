package aaravchour.notchnet;

import aaravchour.notchnet.client.SignInScreen;
import aaravchour.notchnet.client.SignUpScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;

public class NotchNetClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("notchnetsignin")
                    .executes(context -> {
                        MinecraftClient.getInstance().execute(() -> {
                            MinecraftClient.getInstance().setScreen(new SignInScreen());
                        });
                        return 1;
                    })
            );
            dispatcher.register(
                ClientCommandManager.literal("notchnetsignup")
                    .executes(context -> {
                        MinecraftClient.getInstance().execute(() -> {
                            MinecraftClient.getInstance().setScreen(new SignUpScreen());
                        });
                        return 1;
                    })
            );
        });
    }
}
