package net.kyrptonaught.quickshulker.gui;

import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.container.CompactStorageMenu;
import net.kyrptonaught.quickshulker.container.PortableCrafterMenu;
import net.kyrptonaught.quickshulker.gui.screen.BundleItemMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Retain the upstream registry shape. A negotiated envelope selects More's precise
 * menu before vanilla opens the established bundle_item wire entry.
 */
public class MenuTypes {
    private static int nextPortableKind;
    public static final MenuType<AbstractContainerMenu> BUNDLE_ITEM = Registry.register(
        BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(QuickShulkerMod.MOD_ID, "bundle_item"),
        new MenuType<>(MenuTypes::createClientMenu, FeatureFlagSet.of()));

    public static void preparePortableMenu(int kind) {
        nextPortableKind = kind >= 1 && kind <= 3 ? kind : 0;
    }
    public static void clearPendingMenu() { nextPortableKind = 0; }
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
