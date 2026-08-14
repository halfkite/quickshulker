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

public record OpenShulkerPacket(int syncId, int slotId) implements CustomPayload {

    public static final Identifier OPEN_SHULKER_PACKET = Identifier.of(QuickShulkerMod.MOD_ID, "open_shulker_packet");

    public static final Id<OpenShulkerPacket> OPEN_SHULKER_PACKET_ID = new Id<>(OPEN_SHULKER_PACKET);

    public static final PacketCodec<PacketByteBuf, OpenShulkerPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.syncId);
                buf.writeInt(value.slotId);
            },
            buf -> new OpenShulkerPacket(buf.readInt(), buf.readInt())
    );

    public static void registerReceivePacket() {
        PayloadTypeRegistry.playC2S().register(OpenShulkerPacket.OPEN_SHULKER_PACKET_ID, OpenShulkerPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenShulkerPacket.OPEN_SHULKER_PACKET_ID, OpenShulkerPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenShulkerPacket.OPEN_SHULKER_PACKET_ID, (payload, context) -> context.server().execute(() -> Util.openItemFromScreenSlot(context.player(), payload.syncId, payload.slotId)));
    }

    @Environment(EnvType.CLIENT)
    public static void sendOpenPacket(int invSlot) {
        net.minecraft.client.network.ClientPlayerEntity player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player != null) {
            ClientPlayNetworking.send(new OpenShulkerPacket(player.currentScreenHandler.syncId, invSlot));
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return OPEN_SHULKER_PACKET_ID;
    }
}
