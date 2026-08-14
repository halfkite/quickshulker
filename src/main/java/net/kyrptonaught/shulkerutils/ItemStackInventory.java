package net.kyrptonaught.shulkerutils;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.Objects;


public class ItemStackInventory extends SimpleInventory {
    protected final ItemStack itemStack;
    protected final int SIZE;

    public ItemStackInventory(ItemStack stack, int SIZE) {
        this(stack, resolveSize(stack, SIZE), true);
    }

    private ItemStackInventory(ItemStack stack, int resolvedSize, boolean ignored) {
        super(getStacks(stack, resolvedSize).toArray(new ItemStack[resolvedSize]));
        itemStack = stack;
        this.SIZE = resolvedSize;
    }

    private static int resolveSize(ItemStack stack, int requestedSize) {
        ContainerComponent contents = stack.getComponents().get(DataComponentTypes.CONTAINER);
        if (contents == null) {
            return requestedSize;
        }
        return Math.max(requestedSize, Math.toIntExact(contents.stream().count()));
    }

    public static DefaultedList<ItemStack> getStacks(ItemStack usedStack, int SIZE) {
        DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        Objects.requireNonNull(usedStack.getComponents().get(DataComponentTypes.CONTAINER)).copyTo(itemStacks);
        return itemStacks;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < size(); i++) {
            itemStacks.set(i, getStack(i));
        }
        itemStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(itemStacks));
    }

    @Override
    public void onClose(PlayerEntity playerEntity) {
        if (itemStack.getCount() > 1) {
            int count = itemStack.getCount();
            itemStack.setCount(1);
            playerEntity.giveItemStack(new ItemStack(itemStack.getItem(), count - 1));
        }
        markDirty();
    }
}
