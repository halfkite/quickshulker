package net.kyrptonaught.quickshulker.mixin;
import net.kyrptonaught.quickshulker.container.*;
import net.kyrptonaught.quickshulker.gui.screen.*;
import net.kyrptonaught.quickshulker.network.PortableMenuEnvelope;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayNetworkHandler.class)
public final class PortableScreenMixin {
    @Inject(method="onOpenScreen",at=@At("HEAD"),cancellable=true)
    private void openNegotiatedMenu(OpenScreenS2CPacket packet,CallbackInfo callback){
        var client=MinecraftClient.getInstance();
        if(!client.isOnThread())return; // Let vanilla schedule this packet before handling its UI.
        int kind=PortableMenuEnvelope.takePending();
        if(kind==0||packet.getScreenHandlerType()!=ScreenHandlerType.GENERIC_9X3||client.player==null)return;
        if(kind==1){
            var menu=new CompactStorageMenu(packet.getSyncId(),client.player.getInventory(),1);
            client.player.currentScreenHandler=menu;client.setScreen(new CompactStorageScreen(menu,client.player.getInventory(),packet.getName()));
        }else if(kind==3){
            var menu=new PortableCrafterMenu(packet.getSyncId(),client.player.getInventory());
            client.player.currentScreenHandler=menu;client.setScreen(new PortableCrafterScreen(menu,client.player.getInventory(),packet.getName()));
        }else if(kind==4){
            var menu=new PortableBundleMenu(packet.getSyncId(),client.player.getInventory());
            client.player.currentScreenHandler=menu;client.setScreen(new PortableBundleScreen(menu,client.player.getInventory(),packet.getName()));
        }else return;
        callback.cancel();
    }
}
