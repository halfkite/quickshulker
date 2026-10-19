package net.kyrptonaught.quickshulker.compat.ams;

import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class AMSCompat {
    private static final String MOD_ID = "carpet-ams-addition";
    private static final RuleAccess RULE = findRule();
    private static final RuleAccess ENDER_RULE = findFieldRule("largeEnderChest");
    private static boolean warned;

    private AMSCompat() {
    }

    public static boolean isLargeShulkerBoxEnabled() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return false;
        }
        try {
            if (RULE != null) return RULE.isEnabled();
            warnUnavailable();
            return false;
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!warned) {
                warned = true;
                QuickShulkerMod.LOGGER.warn("Unable to read Carpet AMS Addition largeShulkerBox rule; using 27 slots", exception);
            }
            return false;
        }
    }

    public static boolean isLargeEnderChestEnabled() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) return false;
        try {
            if (ENDER_RULE != null) return ENDER_RULE.isEnabled();
            warnUnavailable();
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!warned) {
                warned = true;
                QuickShulkerMod.LOGGER.warn("Cannot read AMS largeEnderChest; preserving actual inventory capacity", exception);
            }
        }
        return false;
    }

    private static void warnUnavailable() {
        if (!warned) {
            warned = true;
            QuickShulkerMod.LOGGER.warn("AMS rule API unavailable; existing container contents will be preserved");
        }
    }

    public static int getEffectiveShulkerInventorySize(ItemStack stack, int defaultSize) {
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().getClass() == ShulkerBoxBlock.class
                && isLargeShulkerBoxEnabled()) {
            return Math.max(defaultSize, 54);
        }
        return defaultSize;
    }

    private static RuleAccess findRule() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return null;
        }

        for (String className : new String[]{
                "carpetamsaddition.CarpetAMSAdditionLazySettings",
                "club.mcams.carpet.AmsServerLazySettings"
        }) {
            RuleAccess access = LazyRuleAccess.create(className);
            if (access != null) {
                return access;
            }
        }

        return findFieldRule("largeShulkerBox");
    }

    private static RuleAccess findFieldRule(String name) {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) return null;
        for (String className : new String[]{"carpetamsaddition.CarpetAMSAdditionSettings", "club.mcams.carpet.AmsServerSettings"}) {
            RuleAccess access = FieldRuleAccess.create(className, name);
            if (access != null) return access;
        }
        return null;
    }

    private interface RuleAccess {
        boolean isEnabled() throws ReflectiveOperationException;
    }

    private record LazyRuleAccess(Method method, Object rule) implements RuleAccess {
        @SuppressWarnings({"rawtypes", "unchecked"})
        private static LazyRuleAccess create(String className) {
            try {
                ClassLoader loader = AMSCompat.class.getClassLoader();
                Class<?> settingsClass = Class.forName(className, false, loader);
                Class<? extends Enum> ruleClass = Class.forName(className + "$Rule", false, loader).asSubclass(Enum.class);
                Object rule = Enum.valueOf(ruleClass, "LARGE_SHULKER_BOX");
                return new LazyRuleAccess(settingsClass.getMethod("isEnabled", ruleClass), rule);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        }

        @Override
        public boolean isEnabled() throws ReflectiveOperationException {
            return Boolean.TRUE.equals(method.invoke(null, rule));
        }
    }

    private record FieldRuleAccess(Field field) implements RuleAccess {
        private static FieldRuleAccess create(String className, String fieldName) {
            try {
                Class<?> settings = Class.forName(className, false, AMSCompat.class.getClassLoader());
                return new FieldRuleAccess(settings.getField(fieldName));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
        }

        @Override
        public boolean isEnabled() throws IllegalAccessException {
            return field.getBoolean(null);
        }
    }
}
