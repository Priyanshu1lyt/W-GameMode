package com.notheartyt.easygamemode;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class EasyGamemodeClient implements ClientModInitializer {
    public static final String MOD_ID = "easygamemode";
    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.easygamemode.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new EasyGamemodeScreen());
                }
            }
        });

        // Server feedback (used to show a simple error if a command is rejected).
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> GamemodeController.onGameMessage(message, overlay));

        // Forget any pending request when we leave a server.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> GamemodeController.reset());
    }
}
