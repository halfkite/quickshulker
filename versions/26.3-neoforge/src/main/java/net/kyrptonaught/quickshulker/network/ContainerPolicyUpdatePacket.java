package net.kyrptonaught.quickshulker.network;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.server.level.ServerPlayer;
import net.kyrptonaught.quickshulker.QuickShulker;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
public record ContainerPolicyUpdatePacket(boolean enabled,boolean creativeOnly,int mask) implements CustomPacketPayload{
    public static final Type<ContainerPolicyUpdatePacket> ID=new Type<>(Identifier.fromNamespaceAndPath("quickshulker","update_container_policy_v2"));
    public static final StreamCodec<FriendlyByteBuf,ContainerPolicyUpdatePacket> CODEC=StreamCodec.ofMember(
        (p,b)->{b.writeBoolean(p.enabled);b.writeBoolean(p.creativeOnly);b.writeInt(p.mask);},
        b->new ContainerPolicyUpdatePacket(b.readBoolean(),b.readBoolean(),b.readInt()));
    public static void register(RegisterPayloadHandlersEvent event){
        event.registrar("1").optional().playToServer(ID,CODEC,(p,context)->context.enqueueWork(()->{
            var player=(ServerPlayer)context.player();
            if(!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)){DetailedContainerPolicyPacket.send(player);return;}
            if((p.mask & ~ContainerKind.allMask())!=0)return;
            var c=QuickShulker.getConfig();c.quickContainers=p.enabled;c.quickContainersCreativeOnly=p.creativeOnly;
            ContainerKind.applyMask(c,p.mask);QuickShulker.config.save();PortableContainers.refresh(player.level().getServer());
        }));
    }
    public Type<? extends CustomPacketPayload> type(){return ID;}
}
