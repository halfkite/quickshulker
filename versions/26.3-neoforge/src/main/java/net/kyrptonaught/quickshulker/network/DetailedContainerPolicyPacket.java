package net.kyrptonaught.quickshulker.network;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.kyrptonaught.quickshulker.QuickShulker;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
/** Versioned independently from the two-boolean legacy policy. */
public record DetailedContainerPolicyPacket(boolean enabled,boolean creativeOnly,int mask) implements CustomPacketPayload{
    public static final Type<DetailedContainerPolicyPacket> ID=new Type<>(Identifier.fromNamespaceAndPath("quickshulker","container_policy_v2"));
    public static final StreamCodec<FriendlyByteBuf,DetailedContainerPolicyPacket> CODEC=StreamCodec.ofMember(
        (p,b)->{b.writeBoolean(p.enabled);b.writeBoolean(p.creativeOnly);b.writeInt(p.mask);},
        b->new DetailedContainerPolicyPacket(b.readBoolean(),b.readBoolean(),b.readInt()));
    public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").optional().playToClient(ID,CODEC);}
    public static boolean canSend(ServerPlayer player){return player.connection.hasChannel(ID);}
    public static void send(ServerPlayer player){
        var c=QuickShulker.getConfig();if(canSend(player))PacketDistributor.sendToPlayer(player,new DetailedContainerPolicyPacket(c.quickContainers,c.quickContainersCreativeOnly,ContainerKind.mask(c)));
    }
    public Type<? extends CustomPacketPayload> type(){return ID;}
}
