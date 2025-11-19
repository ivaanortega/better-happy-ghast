package com.example.happyghast;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HappyGhastParkMod implements ModInitializer {
    public static final String MOD_ID = "happyghastpark";
    public static final String MODID = "happyghastpark";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public static Config CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = Config.load();

        // Register payload type for C2S (Client-to-Server) communication
        PayloadTypeRegistry.playC2S().register(GhastSprintPayload.ID, GhastSprintPayload.CODEC);

        // Register server-side handlers
        ModMessages.register();

        LOGGER.info("[HappyGhastPark] Mod initialized successfully.");
    }
}