package net.kyrptonaught.quickshulker.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.api.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Versioned channel for opening a shulker from a non-player inventory slot. */
public record OpenContainerShulkerPacket(int syncId, int slotId) implements CustomPacketPayload {
    public static final Identifier OPEN_CONTAINER_SHULKER_PACKET = Identifier.fromNamespaceAndPath(QuickShulkerMod.MOD_ID, "open_container_shulker_packet");
    public static final Type<OpenContainerShulkerPacket> OPEN_CONTAINER_SHULKER_PACKET_ID = new Type<>(OPEN_CONTAINER_SHULKER_PACKET);
    public static final StreamCodec<FriendlyByteBuf, OpenContainerShulkerPacket> CODEC = StreamCodec.ofMember(
            (value, buf) -> {
                buf.writeInt(value.syncId);
                buf.writeInt(value.slotId);
            },
            buf -> new OpenContainerShulkerPacket(buf.readInt(), buf.readInt())
    );

    public static void registerReceivePacket() {
        PayloadTypeRegistry.serverboundPlay().register(OPEN_CONTAINER_SHULKER_PACKET_ID, CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OPEN_CONTAINER_SHULKER_PACKET_ID, CODEC);
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
    public Type<? extends CustomPacketPayload> type() {
        return OPEN_CONTAINER_SHULKER_PACKET_ID;
    }
}
