package net.kyrptonaught.quickshulker.api;

import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.network.OpenInventoryPacket;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.kyrptonaught.shulkerutils.ShulkerUtils;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

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
        if (player.isSpectator() || !sourceHandler.canUse(player)) return;
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
        if (player.isSpectator()) return;
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
        int storedSlots = (int) stack.getOrDefault(net.minecraft.component.DataComponentTypes.CONTAINER,
            net.minecraft.component.type.ContainerComponent.DEFAULT).stream().count();
        if (stack.getItem() instanceof net.minecraft.item.BlockItem blockItem
                && blockItem.getBlock().getClass() == net.minecraft.block.ShulkerBoxBlock.class && storedSlots > 54) {
            player.sendMessage(net.minecraft.text.Text.literal("QuickShulker: unsupported shulker capacity; contents preserved."), true);
            return;
        }
        if (PortableContainers.supports(stack) && !PortableContainers.allowed(player, stack)) {
            player.sendMessage(net.minecraft.text.Text.translatable("quickshulker.container.denied"), true);
            return;
        }
        QuickShulkerData qsData = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsData != null || PortableContainers.supports(stack)) {
            if (PortableContainers.supports(stack)) PortableContainers.open(player, stack);
            else qsData.openConsumer.accept(player, stack);
            if (player.currentScreenHandler == sourceHandler) {
                return;
            }
            ItemInventoryContainer openedContainer = (ItemInventoryContainer) player.currentScreenHandler;
            openedContainer.setUsedSlot(playerInvIndex);
            openedContainer.setSourceSlot(player, sourceInventory, sourceInventorySlot, stack);
            for (Slot slot : player.currentScreenHandler.slots) {
                if (slot.inventory instanceof net.kyrptonaught.shulkerutils.ItemStackInventory contents) {
                    contents.bindSourceSession(openedContainer);
                    contents.setWriteAllowed(() -> openedContainer.isSourceValid(player));
                }
            }
            if (qsData != null && QuickShulkerMod.getConfig().playSound) playSound(player, qsData.getSound(stack, true));
            player.currentScreenHandler.addListener(forceCloseScreenIfNotPresent(player, sourceInventory, sourceInventorySlot, stack));
        }
    }

    public static Boolean isOpenableItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (PortableContainers.supports(stack)) return stack.getCount() == 1;
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
        if (PortableContainers.supports(stack)) return true;
        QuickShulkerData qsData = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsData != null) {
            return qsData.canOpenInHand;
        }
        return false;
    }

    public static void playOpenSound(PlayerEntity player, int selectedInvIndex){
        ItemStack stack = player.getInventory().getStack(selectedInvIndex);
        QuickShulkerData qsData = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsData != null) {
            playSound(player, qsData.getSound(stack, true));
        }
    }

    public static void playCloseSound(PlayerEntity player, int selectedInvIndex){
        ItemStack stack = player.getInventory().getStack(selectedInvIndex);
        QuickShulkerData qsData = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsData != null) {
            playSound(player, qsData.getSound(stack, false));
        }
    }

    public static void playSound(PlayerEntity player, SoundEvent sound){
        if(sound != null){
//            player.getWorld().playSound(null, player.getBlockPos(), sound, player.getSoundCategory());
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundCategory.BLOCKS);
        }
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
