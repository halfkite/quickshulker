package net.kyrptonaught.quickshulker.gui;

import net.kyrptonaught.quickshulker.QuickShulker;
import net.kyrptonaught.quickshulker.gui.screen.BundleItemMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class MenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            BuiltInRegistries.MENU,
            QuickShulker.MOD_ID
    );

    public static final Supplier<MenuType<BundleItemMenu>> BUNDLE_ITEM = MENU_TYPES.register("bundle_item", () -> new MenuType<>(BundleItemMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<net.kyrptonaught.quickshulker.container.CompactStorageMenu>> POT = MENU_TYPES.register("portable_pot", () -> new MenuType<>((id, inv) -> new net.kyrptonaught.quickshulker.container.CompactStorageMenu(id, inv, 1), FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<net.kyrptonaught.quickshulker.container.CompactStorageMenu>> SHELF = MENU_TYPES.register("portable_shelf", () -> new MenuType<>((id, inv) -> new net.kyrptonaught.quickshulker.container.CompactStorageMenu(id, inv, 3), FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<MenuType<net.minecraft.world.inventory.CrafterMenu>> PORTABLE_CRAFTER = MENU_TYPES.register("portable_crafter", () -> new MenuType<>(net.kyrptonaught.quickshulker.container.PortableCrafterMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static MenuType<net.kyrptonaught.quickshulker.container.CompactStorageMenu> pot() { return POT.get(); }
    public static MenuType<net.kyrptonaught.quickshulker.container.CompactStorageMenu> shelf() { return SHELF.get(); }
    public static MenuType<net.minecraft.world.inventory.CrafterMenu> crafter() { return PORTABLE_CRAFTER.get(); }
}
