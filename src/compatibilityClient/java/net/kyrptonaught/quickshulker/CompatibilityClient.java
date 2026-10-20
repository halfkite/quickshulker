package net.kyrptonaught.quickshulker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Client startup and screen Mixin linkage smoke; never included in the release JAR. */
public final class CompatibilityClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        try {
            var mod = FabricLoader.getInstance().getModContainer("quickshulker").orElseThrow();
            var paths = mod.getOrigin().getPaths();
            if (paths.isEmpty() || paths.stream().anyMatch(path -> !java.nio.file.Files.isRegularFile(path)
                    || !path.toString().endsWith(".jar"))) {
                throw new AssertionError("Client smoke must load the external artifact: " + paths);
            }
            if (!mod.getMetadata().getVersion().getFriendlyString().equals(
                    System.getProperty("quickshulker.compatibility.expectedVersion"))) {
                throw new AssertionError("Client loaded a different artifact version");
            }
            // Static type references force these client Mixin targets through the loader.
            var types = java.util.List.of(
                    net.minecraft.client.gui.screen.ingame.HandledScreen.class,
                    net.minecraft.client.gui.screen.ingame.InventoryScreen.class,
                    net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.class,
                    net.minecraft.client.Mouse.class,
                    net.kyrptonaught.kyrptconfig.config.screen.ConfigScreen.class,
                    net.kyrptonaught.quickshulker.config.ModConfigMenu.class);
            System.out.println("QuickShulkerMore shared JAR client smoke passed: " + types + " from " + paths);
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
