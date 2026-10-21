package net.kyrptonaught.quickshulker.logintest;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyrptonaught.quickshulker.api.Util;
import net.kyrptonaught.quickshulker.network.*;
import net.kyrptonaught.quickshulker.util.EnderChestSyncHandler;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class LoginServer implements DedicatedServerModInitializer {
    private static final java.util.Set<Boolean> completed = new java.util.HashSet<>();
    private static int shutdownTicks = 100;
    private static int vanillaStep, vanillaTicks;
    private static boolean vanillaComplete;
    @Override public void onInitializeServer() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (completed.size() == 2 && --shutdownTicks == 0) server.halt(false);
            var vanilla = server.getPlayerList().getPlayers().stream()
                .filter(player -> player.getGameProfile().name().equals("VanillaProbe")).findFirst();
            if (!vanillaComplete && vanilla.isPresent() && ++vanillaTicks >= 40) {
                vanillaTicks = 0;
                run(vanilla.get(), vanillaStep, false);
                if (vanillaStep++ == 9) {
                    vanillaComplete = true;
                    System.out.println("QS_VANILLA_SUCCESS: all 10 steps completed without Fabric or More on the client");
                }
            }
            if (vanillaComplete && ++vanillaTicks == 100) server.halt(false);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(
            Commands.literal("qsprobe").then(Commands.argument("step", IntegerArgumentType.integer(0, 9))
                .then(Commands.argument("more", BoolArgumentType.bool()).executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    int step = IntegerArgumentType.getInteger(context, "step");
                    boolean more = BoolArgumentType.getBool(context, "more");
                    try { run(player, step, more); }
                    catch (Throwable failure) {
                        failure.printStackTrace();
                        player.sendSystemMessage(Component.literal("QS_FAIL:" + failure));
                        return 0;
                    }
                    return 1;
                })))));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void run(ServerPlayer player, int step, boolean more) {
        var server = player.level().getServer();
        switch (step) {
            case 0 -> {
                check(BuiltInRegistries.MENU.keySet().stream().noneMatch(id -> id.getNamespace().equals("quickshulker")),
                    "No required custom menu registration");
                check(PortableMenuEnvelope.canSend(player) == more, "Negotiated extended menu capability");
                check(ServerPlayNetworking.canSend(player, EnderChestS2CSyncPacket.S2CEChestContentPacket.S2C_ECHEST_CONTENT_PACKET_ID) == more,
                    "Negotiated ender synchronization capability");
                EnderChestSyncHandler.syncEnderChestContent(player);
                EnderChestS2CSyncPacket.S2CEChestSlotPacket.send(player, 0, ItemStack.EMPTY);
                OpenInventoryPacket.send(player);
            }
            case 1 -> player.teleportTo(server.getLevel(Level.NETHER), 0, 100, 0, java.util.Set.of(), 0, 0, false);
            case 2 -> player = server.getPlayerList().respawn(player, true, Entity.RemovalReason.KILLED);
            case 3 -> {
                player.getEnderChestInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
                var opener = player;
                player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                    ChestMenu.threeRows(id, inventory, opener.getEnderChestInventory()), Component.literal("Login probe ender chest")));
            }
            case 4, 5, 6, 7 -> {
                player.closeContainer();
                player.getInventory().setItem(0, new ItemStack(switch (step) {
                    case 4 -> Items.DECORATED_POT; case 5 -> Items.OAK_SHELF;
                    case 6 -> Items.CRAFTER; default -> Items.BUNDLE;
                }));
                Util.openItem(player, 0, 0);
                check(more || player.containerMenu == player.inventoryMenu, "Unsupported client is not sent an extended menu");
            }
            case 8 -> {
                player.closeContainer();
                player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                    ChestMenu.threeRows(id, inventory), Component.literal("Ordinary chest after extended menus")));
            }
            case 9 -> { player.closeContainer(); completed.add(more); }
        }
        System.out.println("QS_SERVER_PASS: step=" + step + " more=" + more);
        player.sendSystemMessage(Component.literal("QS_PROBE:" + step));
    }
}
