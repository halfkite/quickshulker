package net.kyrptonaught.quickshulker.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public interface ItemInventoryContainer {

    int getUsedSlotInPlayerInv();

    default boolean hasItem() {
        return getUsedSlotInPlayerInv() >= 0;
    }

    void setUsedSlot(int playerInvSlotID);

    default void setSourceSlot(PlayerEntity player, Inventory inventory, int inventorySlot, ItemStack stack) {
    }

    default Inventory getSourceInventory() {
        return null;
    }

    default int getSourceInventorySlot() {
        return -1;
    }

    default ItemStack getSourceStack() {
        return ItemStack.EMPTY;
    }

    default boolean isSourceSlot(Inventory inventory, int inventorySlot) {
        return (getSourceInventory() == inventory && getSourceInventorySlot() == inventorySlot)
            || (getSourceInventory() instanceof net.kyrptonaught.shulkerutils.ItemStackInventory parent && parent.isProtectedSource(inventory,inventorySlot));
    }

    default boolean isSourceValid(PlayerEntity player) {
        Inventory source = getSourceInventory();
        ItemStack stack = getSourceStack();
        return source != null && getSourceInventorySlot() >= 0 && getSourceInventorySlot() < source.size()
                && source.getStack(getSourceInventorySlot()) == stack && !stack.isEmpty()
                && Util.isOpenableItem(stack) && source.canPlayerUse(player)
                && (!net.kyrptonaught.quickshulker.container.PortableContainers.supports(stack)
                    || net.kyrptonaught.quickshulker.container.PortableContainers.allowed(player, stack));
    }

}
