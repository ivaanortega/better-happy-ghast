package com.example.happyghast;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GhastSprintPayload(boolean isSprinting) implements CustomPayload {

    public static final CustomPayload.Id<GhastSprintPayload> ID = new CustomPayload.Id<>(Identifier.of(HappyGhastParkMod.MOD_ID, "sprint_payload"));
    public static final PacketCodec<RegistryByteBuf, GhastSprintPayload> CODEC = PacketCodec.of(GhastSprintPayload::write, GhastSprintPayload::new);

    public GhastSprintPayload(RegistryByteBuf buf) {
        this(buf.readBoolean());
    }

    public void write(RegistryByteBuf buf) {
        buf.writeBoolean(this.isSprinting);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}