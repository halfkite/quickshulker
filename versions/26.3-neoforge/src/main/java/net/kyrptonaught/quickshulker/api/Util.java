package net.kyrptonaught.quickshulker.api;

import net.kyrptonaught.quickshulker.QuickShulker;
import net.kyrptonaught.quickshulker.network.OpenInventoryPacket;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer;
import net.kyrptonaught.shulkerutils.ShulkerUtils;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;

public class Util {

    public static void openItem(Player player, int invSlot) {
        openItemFromScreenSlot(player, player.containerMenu.containerId, invSlot);
    }

    public static void openItem(Player player, int invSlot, int playerInvIndex) {
        Container inventory = player.getInventory();
        if (playerInvIndex < 0 || playerInvIndex >= inventory.getContainerSize()) {
            QuickShulker.LOGGER.warn("Rejected request to open invalid player inventory slot {}", playerInvIndex);
            return;
        }
        openItem(player, inventory, playerInvIndex, inventory.getItem(playerInvIndex), playerInvIndex);
    }

    public static void openItemFromScreenSlot(Player player, int syncId, int screenSlotId) {
        AbstractContainerMenu sourceHandler = player.containerMenu;
        if (player.isSpectator() || !sourceHandler.stillValid(player)) return;
        if (sourceHandler.containerId != syncId || screenSlotId < 0 || screenSlotId >= sourceHandler.slots.size()) {
            return;
        }
        if (!sourceHandler.getCarried().isEmpty()) {
            return;
        }

        Slot sourceSlot = sourceHandler.getSlot(screenSlotId);
        ItemStack stack = sourceSlot.getItem();
        Container sourceInventory = sourceSlot.container;
        int sourceInventorySlot = sourceSlot.getContainerSlot();
        int playerInvIndex = sourceInventory == player.getInventory() ? sourceInventorySlot : -1;

        if (playerInvIndex < 0) {
            if (!QuickShulker.getConfig().rightClickContainerShulker
                    || stack.getCount() != 1
                    || !ShulkerUtils.isShulkerItem(stack)
                    || !sourceSlot.mayPickup(player)
                    || !sourceSlot.mayPlace(stack)) {
                return;
            }
        }

        openItem(player, sourceInventory, sourceInventorySlot, stack, playerInvIndex);
    }

    private static void openItem(Player player, Container sourceInventory, int sourceInventorySlot, ItemStack stack, int playerInvIndex) {
        if (player.isSpectator()) return;
        AbstractContainerMenu sourceHandler = player.containerMenu;
        ItemInventoryContainer currentContainer = (ItemInventoryContainer) player.containerMenu;
        if (QuickShulker.getConfig().rightClickClose && currentContainer.isSourceSlot(sourceInventory, sourceInventorySlot)) {
            ((ServerPlayer) player).closeContainer();
            OpenInventoryPacket.send((ServerPlayer) player);
            return;
        }

        if (!isOpenableItem(stack)) {
            return;
        }
        int storedSlots = (int) stack.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
            net.minecraft.world.item.component.ItemContainerContents.EMPTY).itemCopies().count();
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && blockItem.getBlock().getClass() == net.minecraft.world.level.block.ShulkerBoxBlock.class && storedSlots > 54) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("QuickShulker: unsupported shulker capacity; contents preserved."));
            return;
        }
        if (PortableContainers.supports(stack) && !PortableContainers.allowed(player)) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("quickshulker.container.denied"));
            return;
        }
        QuickShulkerData qsData = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsData != null || PortableContainers.supports(stack)) {
            if (PortableContainers.supports(stack)) PortableContainers.open(player, stack);
            else qsData.openConsumer.accept(player, stack);
            if (player.containerMenu == sourceHandler) {
                return;
            }
            ItemInventoryContainer openedContainer = (ItemInventoryContainer) player.containerMenu;
            openedContainer.setUsedSlot(playerInvIndex);
            openedContainer.setSourceSlot(player, sourceInventory, sourceInventorySlot, stack);
            for (Slot slot : player.containerMenu.slots) {
                if (slot.container instanceof net.kyrptonaught.shulkerutils.ItemStackInventory contents) {
                    contents.bindSourceSession(openedContainer);
                    contents.setWriteAllowed(() -> openedContainer.isSourceValid(player));
                }
            }
            if (qsData != null && QuickShulker.getConfig().playSound) playSound(player, qsData.getSound(stack, true));
            player.containerMenu.addSlotListener(forceCloseScreenIfNotPresent(player, sourceInventory, sourceInventorySlot, stack));
        }
    }

    public static Boolean isOpenableItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (PortableContainers.supports(stack)) return stack.getCount() == 1;
        QuickShulkerData qsdata = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsdata == null) return false;
        return qsdata.ignoreSingleStackCheck || stack.getCount() <= 1;
    }

    public static Container getQuickItemInventory(Player player, ItemStack stack) {
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

    public static void playOpenSound(Player player, int selectedInvIndex){
        ItemStack stack = player.getInventory().getItem(selectedInvIndex);
        QuickShulkerData qsData = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsData != null) {
            playSound(player, qsData.getSound(stack, true));
        }
    }

    public static void playCloseSound(Player player, int selectedInvIndex){
        ItemStack stack = player.getInventory().getItem(selectedInvIndex);
        QuickShulkerData qsData = QuickOpenableRegistry.getQuickie(stack.getItem());
        if (qsData != null) {
            playSound(player, qsData.getSound(stack, false));
        }
    }

    public static void playSound(Player player, SoundEvent sound){
        if(sound != null){
//            player.level().playSound(null, player.getBlockPos(), sound, player.getSoundCategory());
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.BLOCKS);
        }
    }

    public static boolean areItemsEqualExactly(ItemStack stack, ItemStack otherStack) {
//        return ItemStack.isSameItem(stack, otherStack) && ItemStack.isSameItemSameComponents(stack, otherStack) && stack.getCount() == otherStack.getCount();
        return ItemStack.isSameItemSameComponents(stack, otherStack);
    }

    public static boolean areItemsEqualOnlyType(ItemStack stack, ItemStack otherStack){
        return stack == otherStack || ItemStack.isSameItem(stack, otherStack);
    }

    public static ContainerListener forceCloseScreenIfNotPresent(Player player, Container sourceInventory, int slotID, ItemStack stack) {
        return new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu handler, int slotId, ItemStack stack) {
                isValid();
            }

            @Override
            public void dataChanged(AbstractContainerMenu handler, int property, int value) {
                isValid();
            }

            public void isValid() {
                sourceInventory.setChanged();
                ItemStack stackInSlot = slotID >= 0 && slotID < sourceInventory.getContainerSize() ? sourceInventory.getItem(slotID) : ItemStack.EMPTY;
                if (stackInSlot != stack || !isOpenableItem(stackInSlot)) {
                    ((ServerPlayer) player).closeContainer();
                }
            }
        };
    }
}
