package net.kyrptonaught.quickshulker.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Sent immediately BEFORE vanilla OpenScreen and its contents, only to supporting clients. */
public record PortableMenuEnvelope(int kind) implements CustomPacketPayload {
    public static final Type<PortableMenuEnvelope> ID = new Type<>(Identifier.fromNamespaceAndPath("quickshulker", "portable_menu_v1"));
    public static final StreamCodec<FriendlyByteBuf, PortableMenuEnvelope> CODEC = StreamCodec.ofMember(
        (packet, buffer) -> buffer.writeByte(packet.kind), buffer -> new PortableMenuEnvelope(buffer.readUnsignedByte()));
    public static void register() { PayloadTypeRegistry.clientboundPlay().register(ID, CODEC); }
    public static boolean canSend(ServerPlayer player) { return ServerPlayNetworking.canSend(player, ID); }
    public static void prepare(ServerPlayer player, ContainerKind kind) {
        int wireKind = switch (kind) {
            case POT -> 1; case SHELF -> 2; case CRAFTER -> 3;
            default -> throw new IllegalArgumentException("Not an extended menu: " + kind);
        };
        if (!canSend(player)) throw new IllegalStateException("Client has not advertised portable menu support");
        ServerPlayNetworking.send(player, new PortableMenuEnvelope(wireKind));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
