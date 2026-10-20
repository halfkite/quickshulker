package net.kyrptonaught.quickshulker.network;
import net.fabricmc.fabric.api.networking.v1.*;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ContainerPolicyUpdatePacket(boolean enabled,boolean creativeOnly,int mask) implements CustomPayload{
    public static final Id<ContainerPolicyUpdatePacket> ID=new Id<>(Identifier.of("quickshulker","update_container_policy_v2"));
    public static final PacketCodec<PacketByteBuf,ContainerPolicyUpdatePacket> CODEC=PacketCodec.of(
        (p,b)->{b.writeBoolean(p.enabled);b.writeBoolean(p.creativeOnly);b.writeInt(p.mask);},
        b->new ContainerPolicyUpdatePacket(b.readBoolean(),b.readBoolean(),b.readInt()));
    public static void register(){
        PayloadTypeRegistry.playC2S().register(ID,CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ID,(p,context)->context.server().execute(()->{
            var player=context.player();
            if(!net.minecraft.server.command.CommandManager.GAMEMASTERS_CHECK.allows(player.getPermissions())){DetailedContainerPolicyPacket.send(player);return;}
            if((p.mask & ~ContainerKind.allMask())!=0)return;
            var c=QuickShulkerMod.getConfig();c.quickContainers=p.enabled;c.quickContainersCreativeOnly=p.creativeOnly;
            ContainerKind.applyMask(c,p.mask);QuickShulkerMod.config.save();PortableContainers.refresh(context.server());
        }));
    }
    public Id<? extends CustomPayload> getId(){return ID;}
}
