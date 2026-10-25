package net.kyrptonaught.quickshulker.network;
import net.fabricmc.fabric.api.networking.v1.*;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
public record ContainerPolicyUpdatePacket(boolean enabled,boolean creativeOnly,int mask,int creativeMask) implements CustomPacketPayload{
    public ContainerPolicyUpdatePacket(boolean enabled,boolean creativeOnly,int mask){this(enabled,creativeOnly,mask,0);}
    public static final Type<ContainerPolicyUpdatePacket> ID=new Type<>(Identifier.fromNamespaceAndPath("quickshulker","update_container_policy_v3"));
    public static final StreamCodec<FriendlyByteBuf,ContainerPolicyUpdatePacket> CODEC=StreamCodec.ofMember(
        (p,b)->{b.writeBoolean(p.enabled);b.writeBoolean(p.creativeOnly);b.writeInt(p.mask);b.writeInt(p.creativeMask);},
        b->new ContainerPolicyUpdatePacket(b.readBoolean(),b.readBoolean(),b.readInt(),b.readInt()));
    public static void register(){
        PayloadTypeRegistry.serverboundPlay().register(ID,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID,(p,context)->context.server().execute(()->{
            var player=context.player();
            if(!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)){DetailedContainerPolicyPacket.send(player);return;}
            if(((p.mask | p.creativeMask) & ~ContainerKind.allMask())!=0)return;
            var c=QuickShulkerMod.getConfig();c.quickContainers=p.enabled;c.quickContainersCreativeOnly=p.creativeOnly;
            ContainerKind.applyMask(c,p.mask);c.containerCreativeMask=p.creativeMask;QuickShulkerMod.config.save();PortableContainers.refresh(context.server());
        }));
    }
    public Type<? extends CustomPacketPayload> type(){return ID;}
}
