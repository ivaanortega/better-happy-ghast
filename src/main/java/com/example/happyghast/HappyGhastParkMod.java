package com.example.happyghast;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HappyGhastParkMod implements ModInitializer {
    public static final String MODID = "happyghastpark";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public static Config CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = Config.load();
        System.out.println("[HappyGhastPark] Loaded config: " + CONFIG);
        LOGGER.info("[HappyGhastPark] Mod inicializado: puedes 'aparcar' ghasts con Blaze Rod.");
    }
}
