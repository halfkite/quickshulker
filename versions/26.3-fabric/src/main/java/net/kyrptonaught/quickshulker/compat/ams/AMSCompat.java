package net.kyrptonaught.quickshulker.compat.ams;

import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Optional, direct public-field integration. No reflection or AMS mixins. */
public final class AMSCompat {
    private static final String MOD_ID = "carpet-ams-addition";
    private static boolean warned;
    private AMSCompat() {}

    private static boolean supported() {
        var ams = FabricLoader.getInstance().getModContainer(MOD_ID);
        if (ams.isEmpty()) return false;
        if (ams.get().findPath("carpetamsaddition/CarpetAMSAdditionSettings.class").isPresent()) return true;
        warnUnavailable("Unsupported AMS settings API; preserving existing inventory capacity", null);
        return false;
    }
    public static boolean isLargeShulkerBoxEnabled() {
        if (!supported()) return false;
        try { return DirectSettings.largeShulkerBox(); }
        catch (NoClassDefFoundError | NoSuchFieldError error) {
            warnUnavailable("AMS public largeShulkerBox API differs from the supported signature", error);
            return false;
        }
    }
    public static boolean isLargeEnderChestEnabled() {
        if (!supported()) return false;
        try { return DirectSettings.largeEnderChest(); }
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
}
