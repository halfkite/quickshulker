package net.kyrptonaught.quickshulker.mixin;

import net.kyrptonaught.quickshulker.gui.MenuTypes;
import net.kyrptonaught.quickshulker.gui.screen.BundleItemMenu;
import net.kyrptonaught.quickshulker.gui.screen.BundleItemScreen;
import net.kyrptonaught.quickshulker.gui.screen.CompactStorageScreen;
import net.kyrptonaught.quickshulker.gui.screen.PortableCrafterScreen;
import net.kyrptonaught.quickshulker.container.CompactStorageMenu;
import net.kyrptonaught.quickshulker.container.PortableCrafterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public final class PortableScreenMixin {
    @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
    private void openNegotiatedMenu(ClientboundOpenScreenPacket packet, CallbackInfo callback) {
        var client = Minecraft.getInstance();
        if (!client.isSameThread()) return;
        int kind = MenuTypes.takePendingMenu();
        if (kind == 0 || packet.getType() != MenuType.GENERIC_9x3 || client.player == null) return;
        var inventory = client.player.getInventory();
        MenuTypes.preparePortableMenu(kind);
        var menu = MenuTypes.createClientMenu(packet.getContainerId(), inventory);
        client.player.containerMenu = menu;
        if (menu instanceof CompactStorageMenu compact)
            client.setScreen(new CompactStorageScreen(compact, inventory, packet.getTitle()));
        else if (menu instanceof PortableCrafterMenu crafter)
            client.setScreen(new PortableCrafterScreen(crafter, inventory, packet.getTitle()));
        else if (menu instanceof BundleItemMenu bundle)
            client.setScreen(new BundleItemScreen(bundle, inventory, packet.getTitle()));
        callback.cancel();
    }
}
