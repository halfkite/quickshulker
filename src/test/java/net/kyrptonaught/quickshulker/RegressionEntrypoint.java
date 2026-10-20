package net.kyrptonaught.quickshulker;

import net.fabricmc.api.ModInitializer;

/** Runs only in the separate development test mod, never in the distributable. */
public final class RegressionEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        try {
            if (Boolean.getBoolean("quickshulker.compatibility")) {
                var mod = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("quickshulker").orElseThrow();
                var origins = mod.getOrigin().getPaths();
                if (origins.isEmpty() || origins.stream().anyMatch(path -> !java.nio.file.Files.isRegularFile(path)
                        || !path.toString().endsWith(".jar"))) {
                    throw new AssertionError("Compatibility tests must load the external JAR, not development classes: " + origins);
                }
                if (!mod.getMetadata().getVersion().getFriendlyString().equals(
                        System.getProperty("quickshulker.compatibility.expectedVersion"))) {
                    throw new AssertionError("Loaded a different artifact version");
                }
                System.out.println("QuickShulkerMore external artifact verified: " + origins);
            }
            if (!Boolean.getBoolean("quickshulker.regression")) return;
            ContainerRegressionTest.main(new String[0]);
            System.exit(0);
        } catch (Throwable failure) {
            // Minecraft's server main catches startup failures and otherwise exits with zero.
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
