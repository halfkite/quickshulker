package net.kyrptonaught.quickshulker.config;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.kyrptonaught.quickshulker.network.ContainerPolicyUpdatePacket;
import net.minecraft.client.MinecraftClient;
public final class ContainerConfigClient{
    private ContainerConfigClient(){}
    public static void saved(){
        QuickShulkerMod.config.save();
        var client=MinecraftClient.getInstance();
        if(client.player!=null && net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(ContainerPolicyUpdatePacket.ID)){
            var c=QuickShulkerMod.getConfig();
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new ContainerPolicyUpdatePacket(c.quickContainers,c.quickContainersCreativeOnly,ContainerKind.mask(c)));
        }
    }
}
