package net.kyrptonaught.quickshulker.network;
import net.fabricmc.fabric.api.networking.v1.*;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
/** Versioned independently from the two-boolean legacy policy. */
public record DetailedContainerPolicyPacket(boolean enabled,boolean creativeOnly,int mask,int creativeMask) implements CustomPayload{
    public DetailedContainerPolicyPacket(boolean enabled,boolean creativeOnly,int mask){this(enabled,creativeOnly,mask,0);}
    public static final Id<DetailedContainerPolicyPacket> ID=new Id<>(Identifier.of("quickshulker","container_policy_v3"));
    public static final PacketCodec<PacketByteBuf,DetailedContainerPolicyPacket> CODEC=PacketCodec.of(
        (p,b)->{b.writeBoolean(p.enabled);b.writeBoolean(p.creativeOnly);b.writeInt(p.mask);b.writeInt(p.creativeMask);},
        b->new DetailedContainerPolicyPacket(b.readBoolean(),b.readBoolean(),b.readInt(),b.readInt()));
    public static void register(){PayloadTypeRegistry.playS2C().register(ID,CODEC);}
    public static boolean canSend(ServerPlayerEntity player){return ServerPlayNetworking.canSend(player,ID);}
    public static void send(ServerPlayerEntity player){
        var c=QuickShulkerMod.getConfig();if(canSend(player))ServerPlayNetworking.send(player,new DetailedContainerPolicyPacket(c.quickContainers,c.quickContainersCreativeOnly,ContainerKind.mask(c),c.containerCreativeMask & ContainerKind.allMask()));
    }
    public Id<? extends CustomPayload> getId(){return ID;}
}
