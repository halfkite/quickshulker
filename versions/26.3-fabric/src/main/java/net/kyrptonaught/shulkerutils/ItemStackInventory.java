package net.kyrptonaught.shulkerutils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

import java.util.function.BooleanSupplier;


public class ItemStackInventory extends SimpleContainer {
    protected final ItemStack itemStack;
    protected final int SIZE;
    private BooleanSupplier writeAllowed = () -> true;
    private net.kyrptonaught.quickshulker.api.ItemInventoryContainer sourceSession;

    public void bindSourceSession(net.kyrptonaught.quickshulker.api.ItemInventoryContainer session) {
        sourceSession = session;
    }

    public boolean isProtectedSource(net.minecraft.world.Container inventory, int slot) {
        return sourceSession != null && sourceSession.isSourceSlot(inventory, slot);
    }

    public void setWriteAllowed(BooleanSupplier writeAllowed) {
        this.writeAllowed = writeAllowed;
    }

    @Override
    public boolean stillValid(Player player) {
        return writeAllowed.getAsBoolean();
    }

    public ItemStackInventory(ItemStack stack, int SIZE) {
        this(stack, resolveSize(stack, SIZE), true);
    }

    private ItemStackInventory(ItemStack stack, int resolvedSize, boolean ignored) {
        super(getStacks(stack, resolvedSize).toArray(new ItemStack[resolvedSize]));
        itemStack = stack;
        this.SIZE = resolvedSize;
    }

    private static int resolveSize(ItemStack stack, int requestedSize) {
        ItemContainerContents contents = stack.getComponents().get(DataComponents.CONTAINER);
        if (contents == null) {
            return requestedSize;
        }
        return Math.max(requestedSize, Math.toIntExact(contents.itemCopies().count()));
    }

    public static NonNullList<ItemStack> getStacks(ItemStack usedStack, int SIZE) {
        NonNullList<ItemStack> itemStacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        usedStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(itemStacks);
        return itemStacks;
    }

    @Override
    public void setChanged() {
        if (itemStack == null || !writeAllowed.getAsBoolean()) return;
        super.setChanged();
        NonNullList<ItemStack> itemStacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < getContainerSize(); i++) {
            itemStacks.set(i, getItem(i));
        }
        itemStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(itemStacks));
    }

    @Override
    public void stopOpen(net.minecraft.world.entity.ContainerUser user) {
        if (itemStack.getCount() > 1) {
            int count = itemStack.getCount();
            itemStack.setCount(1);
            ((Player) user).addItem(new ItemStack(itemStack.getItem(), count - 1));
        }
        setChanged();
    }
}
