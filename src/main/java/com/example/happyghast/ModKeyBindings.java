package com.example.happyghast;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {

    private static final String CATEGORY = "Better Happy Ghast Lite";
    private static KeyBinding sprintKey;
    private static boolean wasSprinting = false;

    public static void register() {
        Category category = new Category(Identifier.of(HappyGhastParkMod.MOD_ID, "controls"));
        sprintKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.betterhappyghast.sprint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, category)
        );

        ClientTickEvents.END_CLIENT_TICK.register(ModKeyBindings::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        if (sprintKey == null) return;

        boolean isSprintingNow = sprintKey.isPressed();
        if (isSprintingNow != wasSprinting) {
            ClientPlayNetworking.send(new GhastSprintPayload(isSprintingNow));
            wasSprinting = isSprintingNow;
        }
    }
}