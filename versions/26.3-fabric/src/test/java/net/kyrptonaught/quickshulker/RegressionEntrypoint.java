package net.kyrptonaught.quickshulker;
public final class RegressionEntrypoint implements net.fabricmc.api.ModInitializer {
    public void onInitialize() {
        try {
            if (Boolean.getBoolean("quickshulker.compatibility")) {
                var mod = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("quickshulker").orElseThrow();
                var origins = mod.getOrigin().getPaths();
                if (origins.isEmpty() || origins.stream().anyMatch(path -> !java.nio.file.Files.isRegularFile(path)
                        || !path.toString().endsWith(".jar")))
                    throw new AssertionError("Compatibility tests must load the external JAR: " + origins);
                if (!mod.getMetadata().getVersion().getFriendlyString().equals(
                        System.getProperty("quickshulker.compatibility.expectedVersion")))
                    throw new AssertionError("Loaded a different artifact version");
                System.out.println("QuickShulkerMore external artifact verified: " + origins);
            }
            if (!Boolean.getBoolean("quickshulker.regression")) return;
            ContainerRegressionTest.main(new String[0]);
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
