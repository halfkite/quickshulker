package net.kyrptonaught.quickshulker.container;

import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import java.util.Arrays;

/** Vanilla menu wire layouts, with storage-only slot restrictions on the server. */
public final class PortableStorageHandler extends ScreenHandler {
    private final ItemStackInventory contents;
    private final int menuSlots;

    public PortableStorageHandler(int id, PlayerInventory player, ItemStack host, PortableContainers.Layout layout) {
        super(layout.type(), id);
        this.contents = new ItemStackInventory(host, layout.storageSlots());
        this.menuSlots = layout.menuSlots();
        NbtComponent data = host.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        int[] disabled = host.isOf(Blocks.CRAFTER.asItem()) && data != null
            ? data.copyNbt().getIntArray("disabled_slots") : new int[0];
        int columns = menuSlots >= 27 ? 9 : menuSlots == 9 ? 3 : 5;
        for (int i = 0; i < menuSlots; i++) {
            final int index = i;
            addSlot(new Slot(contents, i, 8 + i % columns * 18, 18 + i / columns * 18) {
                private boolean active() {
                    return index < layout.storageSlots() && Arrays.stream(disabled).noneMatch(x -> x == index);
                }
                @Override public boolean canInsert(ItemStack stack) {
                    // Protect this exact host; other containers of the same item type are valid storage.
                    return active() && stack != host
                        && (!net.kyrptonaught.shulkerutils.ShulkerUtils.isShulkerItem(host)
                            || !net.kyrptonaught.shulkerutils.ShulkerUtils.isShulkerItem(stack));
                }
                @Override public boolean canTakeItems(PlayerEntity player) { return active(); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(player, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 142));
    }

    @Override public boolean canUse(PlayerEntity player) {
        return ((ItemInventoryContainer) (Object) this).isSourceValid(player);
    }

    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        if (index < 0 || index >= slots.size() || !canUse(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasStack() || !slot.canTakeItems(player)) return ItemStack.EMPTY;
        ItemStack moving = slot.getStack();
        ItemStack copy = moving.copy();
        if (index < menuSlots) {
            if (!insertItem(moving, menuSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!insertItem(moving, 0, menuSlots, false)) return ItemStack.EMPTY;
        if (moving.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        return copy;
    }

    @Override public void onClosed(PlayerEntity player) {
        contents.markDirty();
        super.onClosed(player);
    }
}
