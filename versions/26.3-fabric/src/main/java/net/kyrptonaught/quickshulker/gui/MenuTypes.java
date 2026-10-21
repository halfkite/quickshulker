package net.kyrptonaught.quickshulker.gui;

import net.kyrptonaught.quickshulker.container.CompactStorageMenu;
import net.kyrptonaught.quickshulker.container.PortableCrafterMenu;
import net.kyrptonaught.quickshulker.gui.screen.BundleItemMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Use a vanilla wire type so joining never requires a client-side registry entry.
 * A negotiated envelope selects the precise menu before vanilla opens the screen.
 */
public class MenuTypes {
    private static int nextPortableKind;
    public static final MenuType<?> BUNDLE_ITEM = MenuType.GENERIC_9x3;

    public static void preparePortableMenu(int kind) {
        nextPortableKind = kind >= 1 && kind <= 4 ? kind : 0;
    }
    public static void clearPendingMenu() { nextPortableKind = 0; }
    public static int takePendingMenu() {
        int kind = nextPortableKind;
        clearPendingMenu();
        return kind;
    }
    public static AbstractContainerMenu createClientMenu(int id, Inventory inventory) {
        int kind = nextPortableKind;
        clearPendingMenu();
        return switch (kind) {
            case 1 -> new CompactStorageMenu(id, inventory, 1);
            case 2 -> new CompactStorageMenu(id, inventory, 3);
            case 3 -> new PortableCrafterMenu(id, inventory);
            default -> new BundleItemMenu(id, inventory);
        };
    }
    public static MenuType<?> pot() { return BUNDLE_ITEM; }
    public static MenuType<?> shelf() { return BUNDLE_ITEM; }
    public static MenuType<?> crafter() { return BUNDLE_ITEM; }
    public static void registerMenuTypes() {}
}
