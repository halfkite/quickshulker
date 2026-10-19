package net.kyrptonaught.quickshulker;

import net.fabricmc.api.ModInitializer;

/** Runs only in the separate development test mod, never in the distributable. */
public final class RegressionEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean("quickshulker.regression")) return;
        try {
            ContainerRegressionTest.main(new String[0]);
            System.exit(0);
        } catch (Throwable failure) {
            // Minecraft's server main catches startup failures and otherwise exits with zero.
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
