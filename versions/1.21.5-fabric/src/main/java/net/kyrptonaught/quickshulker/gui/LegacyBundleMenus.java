package net.kyrptonaught.quickshulker.gui;
import net.kyrptonaught.quickshulker.container.PortableBundleMenu;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
/** Preserve upstream 1.21.2's existing registry ID; new portable menus add no registry IDs. */
public final class LegacyBundleMenus {
    public static final ScreenHandlerType<PortableBundleMenu> BUNDLE_ITEM = Registry.register(
        Registries.SCREEN_HANDLER,Identifier.of("quickshulker","bundle_item"),
        new ScreenHandlerType<>(PortableBundleMenu::new,net.minecraft.resource.featuretoggle.FeatureSet.empty()));
    public static void register() {}
}
