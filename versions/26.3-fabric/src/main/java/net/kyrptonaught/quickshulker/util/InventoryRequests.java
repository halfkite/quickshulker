package net.kyrptonaught.quickshulker.util;
import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import java.util.List;
/** Runs on the server thread; never trusts packet indices or an expired source session. */
public final class InventoryRequests {
    private InventoryRequests() {}
    private static boolean allowed(Player player) {
        var tracked = (ItemInventoryContainer) player.containerMenu;
        return player.isCreative() && !player.isSpectator()
            && (tracked.getSourceInventory() == null || tracked.isSourceValid(player));
    }
    public static void bundle(Player player, int index, ItemStack inserted) {
        if (!allowed(player) || index < 0 || index >= player.getInventory().getContainerSize()) return;
        if (((ItemInventoryContainer) player.containerMenu).isSourceSlot(player.getInventory(), index)) return;
        BundleHelper.bundleItemIntoStack(player, player.getInventory().getItem(index), inserted, null);
    }
    private static Slot find(Player player, int index) {
        if (!allowed(player)) return null;
        var found = player.containerMenu.findSlot(player.getInventory(), index);
        if (found.isEmpty()) return null;
        Slot slot = player.containerMenu.getSlot(found.getAsInt());
        return slot.mayPickup(player) ? slot : null;
    }
    public static void bundleIntoHeld(Player player, int index, List<ItemStack> items) {
        if (items.size() != 2) return;
        Slot slot = find(player, index);
        if (slot != null) BundleHelper.bundleItemIntoStack(player, items.get(1), items.get(0), slot, null);
    }
    public static void unbundle(Player player, int index, ItemStack host) {
        Slot slot = find(player, index);
        if (slot != null) BundleHelper.unbundleItem(player, host, slot);
    }
}
