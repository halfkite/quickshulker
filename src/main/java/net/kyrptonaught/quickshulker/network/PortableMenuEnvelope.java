package net.kyrptonaught.quickshulker.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;

/** Sent immediately BEFORE vanilla OpenScreen and its contents, only to supporting clients. */
public record PortableMenuEnvelope(int kind) implements CustomPayload {
    public static final Id<PortableMenuEnvelope> ID = new Id<>(Identifier.of("quickshulker", "portable_menu_v1"));
    public static final PacketCodec<PacketByteBuf, PortableMenuEnvelope> CODEC = PacketCodec.of(
        (packet, buffer) -> buffer.writeByte(packet.kind), buffer -> new PortableMenuEnvelope(buffer.readUnsignedByte()));
    private static int pending;
    public static void prepareClient(int kind) { pending = kind == 1 || kind == 3 || kind == 4 ? kind : 0; }
    public static void prepareBundle(ServerPlayerEntity player) {
        if(!canSend(player))throw new IllegalStateException("Client has not advertised bundle UI support");
        ServerPlayNetworking.send(player,new PortableMenuEnvelope(4));
    }
    public static int takePending() { int kind=pending; pending=0; return kind; }
    public static void clearPending() { pending=0; }
    public static void register() { PayloadTypeRegistry.playS2C().register(ID, CODEC); }
    public static boolean canSend(ServerPlayerEntity player) { return ServerPlayNetworking.canSend(player, ID); }
    public static void prepare(ServerPlayerEntity player, ContainerKind kind) {
        int wireKind = switch (kind) {
            case POT -> 1; case CRAFTER -> 3;
            default -> throw new IllegalArgumentException("Not an extended menu: " + kind);
        };
        if (!canSend(player)) throw new IllegalStateException("Client has not advertised portable menu support");
        ServerPlayNetworking.send(player, new PortableMenuEnvelope(wireKind));
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
