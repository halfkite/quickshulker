package net.kyrptonaught.quickshulker.gui;

import net.kyrptonaught.quickshulker.container.CompactStorageMenu;
import net.kyrptonaught.quickshulker.gui.screen.BundleItemMenu;
import net.kyrptonaught.quickshulker.gui.screen.BundleItemScreen;
import net.kyrptonaught.quickshulker.gui.screen.CompactStorageScreen;
import net.kyrptonaught.quickshulker.gui.screen.PortableCrafterScreen;
import net.minecraft.world.inventory.CrafterMenu;

public class MenuScreens {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerMenuScreens() {
        // Vanilla's factory is invariant in its menu type. Dispatch with checked instanceof
        // branches, then erase only this registration boundary for the shared wire entry.
        net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor factory = (menu, inventory, title) -> {
            if (menu instanceof CompactStorageMenu compact) return new CompactStorageScreen(compact, inventory, title);
            if (menu instanceof CrafterMenu crafter) return new PortableCrafterScreen(crafter, inventory, title);
            if (menu instanceof BundleItemMenu bundle) return new BundleItemScreen(bundle, inventory, title);
            throw new IllegalArgumentException("Unknown QuickShulkerMore menu " + menu.getClass().getName());
        };
        net.minecraft.client.gui.screens.MenuScreens.register(MenuTypes.BUNDLE_ITEM, factory);
    }
}
