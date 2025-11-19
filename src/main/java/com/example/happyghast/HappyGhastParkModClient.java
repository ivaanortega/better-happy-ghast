package com.example.happyghast;

import net.fabricmc.api.ClientModInitializer;

public class HappyGhastParkModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This is the correct place to register keybindings
        ModKeyBindings.register();
    }
}