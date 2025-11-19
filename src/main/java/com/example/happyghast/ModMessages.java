package com.example.happyghast;

import com.example.happyghast.HappyGhastSprintable;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ModMessages {
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(GhastSprintPayload.ID, (payload, context) -> {
            if (context.player() != null && context.player().getVehicle() instanceof HappyGhastSprintable ghast) {
                ghast.setCustomSprinting(payload.isSprinting());
            }
        });
    }
}