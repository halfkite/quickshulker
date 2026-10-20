package net.kyrptonaught.quickshulker.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.kyrptonaught.quickshulker.QuickShulker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;

public record ContainerPolicyPacket(boolean enabled, boolean creativeOnly) implements CustomPacketPayload {
    public static final Type<ContainerPolicyPacket> ID = new Type<>(Identifier.fromNamespaceAndPath("quickshulker", "container_policy"));
    public static final StreamCodec<FriendlyByteBuf, ContainerPolicyPacket> CODEC = StreamCodec.ofMember(
        (value, buf) -> { buf.writeBoolean(value.enabled); buf.writeBoolean(value.creativeOnly); },
        buf -> new ContainerPolicyPacket(buf.readBoolean(), buf.readBoolean()));
    public static void register(RegisterPayloadHandlersEvent event) { event.registrar("1").optional().playToClient(ID, CODEC); }
    public static void send(ServerPlayer player) {
        DetailedContainerPolicyPacket.send(player);
        if (player.connection.hasChannel(ID)) PacketDistributor.sendToPlayer(player,
            new ContainerPolicyPacket(QuickShulker.getConfig().quickContainers,
                QuickShulker.getConfig().quickContainersCreativeOnly));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
