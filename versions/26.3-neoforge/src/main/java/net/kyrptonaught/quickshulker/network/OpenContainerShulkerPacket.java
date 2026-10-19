package net.kyrptonaught.quickshulker.network;
import net.kyrptonaught.quickshulker.api.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record OpenContainerShulkerPacket(int syncId, int slotId) implements CustomPacketPayload {
    public static final Identifier OPEN_CONTAINER_SHULKER_PACKET = Identifier.fromNamespaceAndPath("quickshulker", "open_container_shulker_packet");
    public static final Type<OpenContainerShulkerPacket> OPEN_CONTAINER_SHULKER_PACKET_ID = new Type<>(OPEN_CONTAINER_SHULKER_PACKET);
    public static final StreamCodec<FriendlyByteBuf, OpenContainerShulkerPacket> CODEC = StreamCodec.ofMember(
        (value, buf) -> { buf.writeInt(value.syncId); buf.writeInt(value.slotId); },
        buf -> new OpenContainerShulkerPacket(buf.readInt(), buf.readInt()));
    @SubscribeEvent public static void registerReceivePacket(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(OPEN_CONTAINER_SHULKER_PACKET_ID, CODEC,
            (payload, context) -> context.enqueueWork(() -> Util.openItemFromScreenSlot(context.player(), payload.syncId, payload.slotId)));
    }
    public static boolean canSend() {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(OPEN_CONTAINER_SHULKER_PACKET_ID);
    }
    public static void sendOpenPacket(int syncId, int slotId) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new OpenContainerShulkerPacket(syncId, slotId));
    }
    public Type<? extends CustomPacketPayload> type() { return OPEN_CONTAINER_SHULKER_PACKET_ID; }
}
