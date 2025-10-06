package com.example.happyghast;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public class Config {
    public double parkedSpeedMultiplier = 2.0;   // x2 al estar sentado y montado
    public double sprintMultiplier      = 1.25;  // boost adicional si el jinete hace sprint
    public double maxSpeed              = 1.60;  // tope de velocidad (longitud del vector)
    public boolean debugLogs = false;     // ← activar/desactivar logs
    public int debugEveryTicks = 20;
    /** Visual indicator while parked: "particles" | "glow" | "none" */
    public String visualMode = "particles";
    /** Particle theme for ground mark: "villager" | "soul" | "endrod" */
    public String particleTheme = "endrod";
    /** Particle style when visualMode = "particles": "ring" | "ground_mark" */
    public String particleStyle = "ring";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config load() {
        Path dir  = FabricLoader.getInstance().getConfigDir();
        Path file = dir.resolve("better-happy-ghast.json");
        Config cfg = new Config();
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    Config read = GSON.fromJson(r, Config.class);
                    if (read != null) cfg = read;
                }
            } else {
                // crear por primera vez
                try (Writer w = Files.newBufferedWriter(file)) {
                    GSON.toJson(cfg, w);
                }
            }
        } catch (IOException e) {
            System.err.println("[HappyGhastPark] Failed to load/save config: " + e.getMessage());
        }
        return cfg;
    }

    @Override public String toString() {
        return "{parkedSpeedMultiplier=" + parkedSpeedMultiplier +
               ", sprintMultiplier=" + sprintMultiplier +
               ", maxSpeed=" + maxSpeed + 
               ", debugLogs=" + debugLogs +
               ", debugEveryTicks=" + debugEveryTicks + 
               ", visualMode=" + visualMode + 
               ", particleTheme=" + particleTheme +
               ", particleStyle=" + particleStyle +
               "}";
    }
}
