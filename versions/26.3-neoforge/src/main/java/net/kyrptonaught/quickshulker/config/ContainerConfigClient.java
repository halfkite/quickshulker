package net.kyrptonaught.quickshulker.config;
import net.kyrptonaught.quickshulker.QuickShulker;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.kyrptonaught.quickshulker.network.ContainerPolicyUpdatePacket;
import net.minecraft.client.Minecraft;
public final class ContainerConfigClient{
    private ContainerConfigClient(){}
    public static void saved(){
        QuickShulker.config.save();
        var client=Minecraft.getInstance();
        if(client.getConnection()!=null && client.getConnection().hasChannel(ContainerPolicyUpdatePacket.ID)){
            var c=QuickShulker.getConfig();
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new ContainerPolicyUpdatePacket(c.quickContainers,c.quickContainersCreativeOnly,ContainerKind.mask(c)));
        }
    }
}
