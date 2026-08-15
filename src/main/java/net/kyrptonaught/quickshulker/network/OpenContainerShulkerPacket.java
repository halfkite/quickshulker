package net.kyrptonaught.quickshulker.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.api.Util;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Versioned channel for opening a shulker from a non-player inventory slot. */
public record OpenContainerShulkerPacket(int syncId, int slotId) implements CustomPayload {
    public static final Identifier OPEN_CONTAINER_SHULKER_PACKET = Identifier.of(QuickShulkerMod.MOD_ID, "open_container_shulker_packet");
    public static final Id<OpenContainerShulkerPacket> OPEN_CONTAINER_SHULKER_PACKET_ID = new Id<>(OPEN_CONTAINER_SHULKER_PACKET);
    public static final PacketCodec<PacketByteBuf, OpenContainerShulkerPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.syncId);
                buf.writeInt(value.slotId);
            },
            buf -> new OpenContainerShulkerPacket(buf.readInt(), buf.readInt())
    );

    public static void registerReceivePacket() {
        PayloadTypeRegistry.playC2S().register(OPEN_CONTAINER_SHULKER_PACKET_ID, CODEC);
        PayloadTypeRegistry.playS2C().register(OPEN_CONTAINER_SHULKER_PACKET_ID, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OPEN_CONTAINER_SHULKER_PACKET_ID,
                (payload, context) -> context.server().execute(() ->
                        Util.openItemFromScreenSlot(context.player(), payload.syncId, payload.slotId)));
    }

    @Environment(EnvType.CLIENT)
    public static boolean canSend() {
        return ClientPlayNetworking.canSend(OPEN_CONTAINER_SHULKER_PACKET_ID);
    }

    @Environment(EnvType.CLIENT)
    public static void sendOpenPacket(int syncId, int slotId) {
        ClientPlayNetworking.send(new OpenContainerShulkerPacket(syncId, slotId));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return OPEN_CONTAINER_SHULKER_PACKET_ID;
    }
}
