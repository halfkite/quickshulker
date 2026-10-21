package net.kyrptonaught.quickshulker.logintest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** Uses a real game connection; the plain run has no QuickShulker classes or mod loaded. */
public final class LoginClient implements ClientModInitializer {
    private final boolean more = Boolean.getBoolean("quickshulker.login.more");
    private final long started = System.nanoTime();
    private boolean connecting, requested;
    private int step, acknowledged = -1, delay;
    @Override public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("quickshulker") != more)
            throw new AssertionError("Wrong mod set for this login test");
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            if (text.startsWith("QS_FAIL:")) fail(text);
            if (text.startsWith("QS_PROBE:")) acknowledged = Integer.parseInt(text.substring(9));
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private void tick(Minecraft client) {
        if (System.nanoTime() - started > java.time.Duration.ofMinutes(3).toNanos()) fail("Login test timed out at step " + step);
        if (client.gui.screen() instanceof DisconnectedScreen) fail("Disconnected at step " + step);
        if (!connecting && client.gui.screen() != null && client.gui.overlay() == null) {
            connecting = true;
            String address = System.getProperty("quickshulker.login.address");
            ConnectScreen.startConnecting(new TitleScreen(), client, ServerAddress.parseString(address),
                new ServerData("QuickShulker login regression", address, ServerData.Type.OTHER), false, null);
        }
        if (client.player == null || client.level == null || client.getConnection() == null) return;
        if (++delay < 30) return;
        if (!requested) {
            client.getConnection().sendCommand("qsprobe " + step + " " + more);
            requested = true; delay = 0;
        } else if (acknowledged == step) {
            checkMenu(client);
            System.out.println("QS_CLIENT_PASS: step=" + step + " more=" + more);
            if (step == 9) {
                System.out.println("QS_LOGIN_SUCCESS: more=" + more);
                System.exit(0);
            }
            step++; requested = false; delay = 0;
        }
    }
    private void checkMenu(Minecraft client) {
        var menu = client.player.containerMenu;
        int expected = switch (step) {
            case 3, 8 -> 63;
            case 4 -> more ? 37 : 46;
            case 5 -> more ? 39 : 46;
            case 6 -> 46;
            case 7 -> more ? 100 : 46;
            default -> -1;
        };
        if (expected >= 0 && menu.slots.size() != expected)
            fail("Step " + step + " expected " + expected + " slots, got " + menu.slots.size());
        if (step == 3 && menu.getSlot(0).getItem().getCount() != 7) fail("Ender contents were not synchronized");
        if (more && step >= 4 && step <= 7) {
            String expectedClass = switch (step) {
                case 4, 5 -> "CompactStorageMenu"; case 6 -> "PortableCrafterMenu"; default -> "BundleItemMenu";
            };
            if (!menu.getClass().getSimpleName().equals(expectedClass)) fail("Wrong extended menu at step " + step);
        }
        if (step == 8 && !menu.getClass().getSimpleName().equals("ChestMenu")) fail("Stale envelope replaced ordinary chest");
    }
    private static void fail(String message) {
        System.err.println("QS_LOGIN_FAILURE: " + message);
        System.exit(1);
    }
}
