package net.kyrptonaught.quickshulker.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public record ContainerPolicyPacket(boolean enabled, boolean creativeOnly) implements CustomPayload {
    public static final Id<ContainerPolicyPacket> ID = new Id<>(Identifier.of("quickshulker", "container_policy"));
    public static final PacketCodec<PacketByteBuf, ContainerPolicyPacket> CODEC = PacketCodec.of(
        (value, buf) -> { buf.writeBoolean(value.enabled); buf.writeBoolean(value.creativeOnly); },
        buf -> new ContainerPolicyPacket(buf.readBoolean(), buf.readBoolean()));
    public static void register() { PayloadTypeRegistry.playS2C().register(ID, CODEC); }
    public static void send(ServerPlayerEntity player) {
        DetailedContainerPolicyPacket.send(player);
        if (ServerPlayNetworking.canSend(player, ID)) ServerPlayNetworking.send(player,
            new ContainerPolicyPacket(QuickShulkerMod.getConfig().quickContainers,
                QuickShulkerMod.getConfig().quickContainersCreativeOnly));
    }
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
