package net.kyrptonaught.quickshulker.config;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
/** Stable policy bit order; append new kinds rather than reordering existing entries. */
public enum ContainerKind {
    CHEST("quickChest", 27, c -> c.quickChest, (c,v) -> c.quickChest=v),
    TRAPPED_CHEST("quickTrappedChest", 27, c -> c.quickTrappedChest, (c,v) -> c.quickTrappedChest=v),
    COPPER_CHEST("quickCopperChest", 27, c -> c.quickCopperChest, (c,v) -> c.quickCopperChest=v),
    BARREL("quickBarrel", 27, c -> c.quickBarrel, (c,v) -> c.quickBarrel=v),
    SHELF("quickShelf", 3, c -> c.quickShelf, (c,v) -> c.quickShelf=v),
    HOPPER("quickHopper", 5, c -> c.quickHopper, (c,v) -> c.quickHopper=v),
    DROPPER("quickDropper", 9, c -> c.quickDropper, (c,v) -> c.quickDropper=v),
    DISPENSER("quickDispenser", 9, c -> c.quickDispenser, (c,v) -> c.quickDispenser=v),
    POT("quickDecoratedPot", 1, c -> c.quickDecoratedPot, (c,v) -> c.quickDecoratedPot=v),
    CRAFTER("quickCrafter", 9, c -> c.quickCrafter, (c,v) -> c.quickCrafter=v),
    FURNACE("quickFurnace", 3, c -> c.quickFurnace, (c,v) -> c.quickFurnace=v),
    SMOKER("quickSmoker", 3, c -> c.quickSmoker, (c,v) -> c.quickSmoker=v),
    BLAST_FURNACE("quickBlastFurnace", 3, c -> c.quickBlastFurnace, (c,v) -> c.quickBlastFurnace=v),
    BREWING_STAND("quickBrewingStand", 5, c -> c.quickBrewingStand, (c,v) -> c.quickBrewingStand=v);
    public final String key;
    public final int slots;
    private final Predicate<ConfigOptions> getter;
    private final BiConsumer<ConfigOptions,Boolean> setter;
    ContainerKind(String key, int slots, Predicate<ConfigOptions> getter, BiConsumer<ConfigOptions,Boolean> setter) {
        this.key=key; this.slots=slots; this.getter=getter; this.setter=setter;
    }
    public boolean enabled(ConfigOptions config) { return getter.test(config); }
    public void set(ConfigOptions config, boolean value) { setter.accept(config,value); }
    public boolean creativeOnly(ConfigOptions config) { return (config.containerCreativeMask & bit()) != 0; }
    public int mode(ConfigOptions config) { return !enabled(config) ? 2 : creativeOnly(config) ? 0 : 1; }
    public void setMode(ConfigOptions config, int mode) {
        if (mode < 0 || mode > 2) throw new IllegalArgumentException("Invalid container mode");
        set(config, mode != 2);
        config.containerCreativeMask = mode == 0 ? config.containerCreativeMask | bit() : config.containerCreativeMask & ~bit();
    }
    public int bit() { return 1 << ordinal(); }
    public static int allMask() { return (1 << values().length)-1; }
    public static int mask(ConfigOptions config) {
        int result=0; for (var kind: values()) if(kind.enabled(config)) result|=kind.bit(); return result;
    }
    public static void applyMask(ConfigOptions config, int mask) {
        for (var kind: values()) kind.set(config,(mask & kind.bit())!=0);
    }
}
