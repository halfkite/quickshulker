package net.kyrptonaught.quickshulker.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface ItemInventoryContainer {

    int getUsedSlotInPlayerInv();

    default boolean hasItem() {
        return getUsedSlotInPlayerInv() >= 0;
    }

    void setUsedSlot(int playerInvSlotID);

    default void setSourceSlot(Player player, Container inventory, int inventorySlot, ItemStack stack) {
    }

    default Container getSourceInventory() {
        return null;
    }

    default int getSourceInventorySlot() {
        return -1;
    }

    default ItemStack getSourceStack() {
        return ItemStack.EMPTY;
    }

    default boolean isSourceSlot(Container inventory, int inventorySlot) {
        return (getSourceInventory() == inventory && getSourceInventorySlot() == inventorySlot)
            || (getSourceInventory() instanceof net.kyrptonaught.shulkerutils.ItemStackInventory parent
                && parent.isProtectedSource(inventory, inventorySlot));
    }

    default boolean isSourceValid(Player player) {
        Container source = getSourceInventory();
        ItemStack stack = getSourceStack();
        return source != null && getSourceInventorySlot() >= 0 && getSourceInventorySlot() < source.getContainerSize()
                && source.getItem(getSourceInventorySlot()) == stack && !stack.isEmpty()
                && Util.isOpenableItem(stack) && source.stillValid(player)
                && (!net.kyrptonaught.quickshulker.container.PortableContainers.supports(stack)
                    || net.kyrptonaught.quickshulker.container.PortableContainers.allowed(player));
    }

}
