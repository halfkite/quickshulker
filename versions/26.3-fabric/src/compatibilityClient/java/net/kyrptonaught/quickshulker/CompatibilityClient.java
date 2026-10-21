package net.kyrptonaught.quickshulker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Official-name counterpart of the Yarn runtime proof; excluded from distribution. */
public final class CompatibilityClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        try {
            var mod = FabricLoader.getInstance().getModContainer("quickshulker").orElseThrow();
            var paths = mod.getOrigin().getPaths();
            if (paths.isEmpty() || paths.stream().anyMatch(path -> !java.nio.file.Files.isRegularFile(path)
                    || !path.toString().endsWith(".jar"))) throw new AssertionError("Expected external artifact: " + paths);
            if (!mod.getMetadata().getVersion().getFriendlyString().equals(
                    System.getProperty("quickshulker.compatibility.expectedVersion")))
                throw new AssertionError("Unexpected artifact version");
            var types = java.util.List.of(
                net.minecraft.client.multiplayer.ClientPacketListener.class,
                net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class,
                net.minecraft.client.gui.screens.inventory.InventoryScreen.class,
                net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class,
                net.minecraft.client.MouseHandler.class,
                net.kyrptonaught.quickshulker.gui.screen.BundleItemScreen.class,
                net.kyrptonaught.quickshulker.gui.screen.CompactStorageScreen.class,
                net.kyrptonaught.quickshulker.gui.screen.PortableCrafterScreen.class);
            System.out.println("QuickShulkerMore shared JAR client smoke passed: " + types + " from " + paths);
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
