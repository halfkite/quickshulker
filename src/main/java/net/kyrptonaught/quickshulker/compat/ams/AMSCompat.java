package net.kyrptonaught.quickshulker.compat.ams;

import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

/** Optional, direct public-field integration. No reflection or AMS mixins. */
public final class AMSCompat {
    private static final String MOD_ID = "carpet-ams-addition";
    private static boolean warned;
    private AMSCompat() {}

    private enum SettingsApi { ABSENT, LEGACY, MODERN, UNSUPPORTED }
    private static SettingsApi settingsApi() {
        var ams = FabricLoader.getInstance().getModContainer(MOD_ID);
        if (ams.isEmpty()) return SettingsApi.ABSENT;
        if (ams.get().findPath("club/mcams/carpet/AmsServerSettings.class").isPresent()) return SettingsApi.MODERN;
        if (ams.get().findPath("carpetamsaddition/CarpetAMSAdditionSettings.class").isPresent()) return SettingsApi.LEGACY;
        warnUnavailable("Unsupported AMS settings API; preserving existing inventory capacity", null);
        return SettingsApi.UNSUPPORTED;
    }
    public static boolean isLargeShulkerBoxEnabled() {
        try { return switch (settingsApi()) {
            case MODERN -> ModernSettings.largeShulkerBox();
            case LEGACY -> DirectSettings.largeShulkerBox();
            default -> false;
        }; }
        catch (NoClassDefFoundError | NoSuchFieldError error) {
            warnUnavailable("AMS public largeShulkerBox API differs from the supported signature", error);
            return false;
        }
    }
    public static boolean isLargeEnderChestEnabled() {
        try { return switch (settingsApi()) {
            case MODERN -> ModernSettings.largeEnderChest();
            case LEGACY -> DirectSettings.largeEnderChest();
            default -> false;
        }; }
        catch (NoClassDefFoundError | NoSuchFieldError error) {
            warnUnavailable("AMS public largeEnderChest API differs from the supported signature", error);
            return false;
        }
    }
    private static void warnUnavailable(String message, LinkageError error) {
        if (warned) return;
        warned = true;
        if (error == null) QuickShulkerMod.LOGGER.warn(message);
        else QuickShulkerMod.LOGGER.warn(message, error);
    }
    public static int getEffectiveShulkerInventorySize(ItemStack stack, int defaultSize) {
        if (stack.getItem() instanceof BlockItem blockItem
            && blockItem.getBlock().getClass() == ShulkerBoxBlock.class && isLargeShulkerBoxEnabled())
            return Math.max(defaultSize, 54);
        return defaultSize;
    }
    // Resolved only after loader metadata confirms that the optional AMS class is present.
    private static final class DirectSettings {
        static boolean largeShulkerBox() { return carpetamsaddition.CarpetAMSAdditionSettings.largeShulkerBox; }
        static boolean largeEnderChest() { return carpetamsaddition.CarpetAMSAdditionSettings.largeEnderChest; }
    }
    private static final class ModernSettings {
        static boolean largeShulkerBox() { return club.mcams.carpet.AmsServerSettings.largeShulkerBox; }
        static boolean largeEnderChest() { return club.mcams.carpet.AmsServerSettings.largeEnderChest; }
    }
}
