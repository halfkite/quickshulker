package net.kyrptonaught.quickshulker.client;

import net.kyrptonaught.quickshulker.api.Util;
import net.kyrptonaught.quickshulker.mixin.CreativeSlotMixin;
import net.kyrptonaught.quickshulker.network.OpenShulkerPacket;
import net.kyrptonaught.quickshulker.network.OpenContainerShulkerPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ClientUtil {

    public static boolean CheckAndSend(ItemStack stack, int slot) {
        return CheckAndSend(stack, slot, false);
    }

    public static boolean CheckAndSend(ItemStack stack, int slot, boolean containerSlot) {
        if (Util.isOpenableItem(stack)) {
            if(stack.getItem() instanceof net.minecraft.item.BundleItem && !net.kyrptonaught.quickshulker.container.PortableContainers.hasDetailedPolicy())return false;
            if (net.kyrptonaught.quickshulker.container.PortableContainers.supports(stack)
                    && !net.kyrptonaught.quickshulker.container.PortableContainers.allowed(MinecraftClient.getInstance().player,stack)) return false;
            if (containerSlot || net.kyrptonaught.quickshulker.container.PortableContainers.supports(stack)) {
                if (!OpenContainerShulkerPacket.canSend()) {
                    return false;
                }
                if(net.kyrptonaught.quickshulker.container.PortableContainers.custom(net.kyrptonaught.quickshulker.container.PortableContainers.kind(stack))
                    && !net.kyrptonaught.quickshulker.container.PortableContainers.hasDetailedPolicy())return false;
                OpenContainerShulkerPacket.sendOpenPacket(MinecraftClient.getInstance().player.currentScreenHandler.syncId, slot);
            } else {
                SendOpenPacket(slot);
            }
            return true;
        }
        return false;
    }

    private static void SendOpenPacket(int slot) {
        OpenShulkerPacket.sendOpenPacket(slot);
    }

    public static boolean isCreativeScreen(PlayerEntity player) {
        return player.currentScreenHandler instanceof CreativeInventoryScreen.CreativeScreenHandler;

    }

    public static int getSlotId(ScreenHandler handler, Slot slot) {
        if (handler instanceof CreativeInventoryScreen.CreativeScreenHandler) {
            if (((CreativeInventoryScreen) MinecraftClient.getInstance().currentScreen).isInventoryTabSelected() && slot instanceof CreativeInventoryScreen.CreativeSlot) {
                return ((CreativeSlotMixin) slot).getSlot().id;
            } else {
                return slot.id - 9;
            }
        }
        return slot.id;
    }

    public static int getPlayerInvSlot(ScreenHandler handler, Slot slot) {
        if (handler instanceof CreativeInventoryScreen.CreativeScreenHandler) {
            if (((CreativeInventoryScreen) MinecraftClient.getInstance().currentScreen).isInventoryTabSelected() && slot instanceof CreativeInventoryScreen.CreativeSlot) {
                return ((CreativeSlotMixin) slot).getSlot().getIndex();
            }
        }
        return slot.getIndex();
    }
}
