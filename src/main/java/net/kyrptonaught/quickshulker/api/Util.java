package net.kyrptonaught.quickshulker.api;

import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.network.OpenInventoryPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.kyrptonaught.shulkerutils.ShulkerUtils;

public class Util {

    public static void openItem(PlayerEntity player, int invSlot) {
        openItemFromScreenSlot(player, player.currentScreenHandler.syncId, invSlot);
    }

    public static void openItem(PlayerEntity player, int invSlot, int playerInvIndex) {
        Inventory inventory = player.getInventory();
        if (playerInvIndex < 0 || playerInvIndex >= inventory.size()) {
            QuickShulkerMod.LOGGER.warn("Rejected request to open invalid player inventory slot {}", playerInvIndex);
            return;
        }
        openItem(player, inventory, playerInvIndex, inventory.getStack(playerInvIndex), playerInvIndex);
    }

    public static void openItemFromScreenSlot(PlayerEntity player, int syncId, int screenSlotId) {
        ScreenHandler sourceHandler = player.currentScreenHandler;
        if (sourceHandler.syncId != syncId || screenSlotId < 0 || screenSlotId >= sourceHandler.slots.size()) {
            return;
        }
        if (!sourceHandler.getCursorStack().isEmpty()) {
            return;
        }

        Slot sourceSlot = sourceHandler.getSlot(screenSlotId);
        ItemStack stack = sourceSlot.getStack();
        Inventory sourceInventory = sourceSlot.inventory;
        int sourceInventorySlot = sourceSlot.getIndex();
        int playerInvIndex = sourceInventory instanceof net.minecraft.entity.player.PlayerInventory ? sourceInventorySlot : -1;

        if (playerInvIndex < 0) {
            if (!QuickShulkerMod.getConfig().rightClickContainerShulker
                    || stack.getCount() != 1
                    || !ShulkerUtils.isShulkerItem(stack)
                    || !sourceSlot.canTakeItems(player)
                    || !sourceSlot.canInsert(stack)) {
                return;
            }
        }

        openItem(player, sourceInventory, sourceInventorySlot, stack, playerInvIndex);
    }

    private static void openItem(PlayerEntity player, Inventory sourceInventory, int sourceInventorySlot, ItemStack stack, int playerInvIndex) {
        ScreenHandler sourceHandler = player.currentScreenHandler;
        ItemInventoryContainer currentContainer = (ItemInventoryContainer) player.currentScreenHandler;
        if (QuickShulkerMod.getConfig().rightClickClose && currentContainer.isSourceSlot(sourceInventory, sourceInventorySlot)) {
            ((ServerPlayerEntity) player).closeHandledScreen();
            OpenInventoryPacket.send((ServerPlayerEntity) player);
            return;
        }

        if (!isOpenableItem(stack)) {
            return;
        }
        QuickShulkerData qsData = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsData != null) {
            qsData.openConsumer.accept(player, stack);
            if (player.currentScreenHandler == sourceHandler) {
                return;
            }
            ItemInventoryContainer openedContainer = (ItemInventoryContainer) player.currentScreenHandler;
            openedContainer.setUsedSlot(playerInvIndex);
            openedContainer.setSourceSlot(player, sourceInventory, sourceInventorySlot, stack);
            player.currentScreenHandler.addListener(forceCloseScreenIfNotPresent(player, sourceInventory, sourceInventorySlot, stack));
        }
    }

    public static Boolean isOpenableItem(ItemStack stack) {
        QuickShulkerData qsdata = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsdata == null) return false;
        return qsdata.ignoreSingleStackCheck || stack.getCount() <= 1;
    }

    public static Inventory getQuickItemInventory(PlayerEntity player, ItemStack stack) {
        QuickShulkerData qsData = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsData != null) {
            if (qsData.supportsBundleing)
                return qsData.getInventory(player, stack);
        }
        return null;
    }

    public static boolean canOpenInHand(ItemStack stack) {
        QuickShulkerData qsData = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsData != null) {
            return qsData.canOpenInHand;
        }
        return false;
    }

    public static boolean areItemsEqualExactly(ItemStack stack, ItemStack otherStack) {
//        return ItemStack.areItemsEqual(stack, otherStack) && ItemStack.areEqual(stack, otherStack) && stack.getCount() == otherStack.getCount();
        return ItemStack.areEqual(stack, otherStack);
    }

    public static boolean areItemsEqualOnlyType(ItemStack stack, ItemStack otherStack){
        return stack == otherStack || ItemStack.areItemsEqual(stack, otherStack);
    }

    public static ScreenHandlerListener forceCloseScreenIfNotPresent(PlayerEntity player, Inventory sourceInventory, int slotID, ItemStack stack) {
        return new ScreenHandlerListener() {
            @Override
            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
                isValid();
            }

            @Override
            public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
                isValid();
            }

            public void isValid() {
                sourceInventory.markDirty();
                ItemStack stackInSlot = slotID >= 0 && slotID < sourceInventory.size() ? sourceInventory.getStack(slotID) : ItemStack.EMPTY;
                if (stackInSlot != stack || !isOpenableItem(stackInSlot)) {
                    ((ServerPlayerEntity) player).closeHandledScreen();
                }
            }
        };
    }
}
