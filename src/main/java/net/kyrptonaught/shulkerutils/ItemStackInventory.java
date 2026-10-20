package net.kyrptonaught.shulkerutils;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.function.BooleanSupplier;


public class ItemStackInventory extends SimpleInventory {
    protected final ItemStack itemStack;
    protected final int SIZE;
    private BooleanSupplier writeAllowed = () -> true;
    private net.kyrptonaught.quickshulker.api.ItemInventoryContainer sourceSession;
    public void bindSourceSession(net.kyrptonaught.quickshulker.api.ItemInventoryContainer session) { sourceSession=session; }
    public boolean isProtectedSource(net.minecraft.inventory.Inventory inventory,int slot) {
        return sourceSession!=null && sourceSession.isSourceSlot(inventory,slot);
    }

    public void setWriteAllowed(BooleanSupplier writeAllowed) {
        this.writeAllowed = writeAllowed;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
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
        ContainerComponent contents = stack.getComponents().get(DataComponentTypes.CONTAINER);
        if (contents == null) {
            return requestedSize;
        }
        return Math.max(requestedSize, Math.toIntExact(contents.stream().count()));
    }

    public static DefaultedList<ItemStack> getStacks(ItemStack usedStack, int SIZE) {
        DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        usedStack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(itemStacks);
        return itemStacks;
    }

    @Override
    public void markDirty() {
        if (itemStack == null || !writeAllowed.getAsBoolean()) return;
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
