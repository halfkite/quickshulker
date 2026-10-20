package net.kyrptonaught.quickshulker;

import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.kyrptonaught.quickshulker.container.*;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EphemeralTestServerProvider.class)
public final class PortableNativeMenuTest {
    @org.junit.jupiter.api.BeforeAll
    static void loadTestWorld(MinecraftServer server) throws Exception {
        server.submit(() -> {
            try {
                // The official provider loads registries/recipes, but intentionally creates no levels.
                var create = MinecraftServer.class.getDeclaredMethod("createLevels");
                create.setAccessible(true);
                create.invoke(server);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Could not initialize the real test world", exception);
            }
        }).get(60, java.util.concurrent.TimeUnit.SECONDS);
    }

    @org.junit.jupiter.api.AfterAll
    static void closeTestWorld(MinecraftServer server) throws Exception {
        server.submit(() -> {
            try {
                for (var level : server.getAllLevels()) level.close();
                var levels = MinecraftServer.class.getDeclaredField("levels");
                levels.setAccessible(true);
                ((java.util.Map<?,?>) levels.get(server)).clear();
            } catch (ReflectiveOperationException | java.io.IOException exception) {
                throw new AssertionError("Could not close the temporary test world", exception);
            }
        }).get(60, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test void nativeSlotRolesAndPermissions(MinecraftServer server) throws Exception {
        server.submit(() -> nativeSlotRolesAndPermissionsOnServer(server)).get(60, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test void craftingLocksRemaindersAndAtomicRefusal(MinecraftServer server) throws Exception {
        server.submit(() -> craftingLocksRemaindersAndAtomicRefusalOnServer(server)).get(60, java.util.concurrent.TimeUnit.SECONDS);
    }

    private static AbstractContainerMenu bind(ServerPlayer player, ItemStack host) {
        player.getInventory().clearContent();
        player.getInventory().setItem(0, host);
        var menu = PortableContainers.create(1, player.getInventory(), host, PortableContainers.kind(host));
        var session = (ItemInventoryContainer)menu;
        session.setUsedSlot(0);
        session.setSourceSlot(player, player.getInventory(), 0, host);
        for (var slot : menu.slots) if (slot.container instanceof ItemStackInventory contents) {
            contents.bindSourceSession(session);
            contents.setWriteAllowed(() -> session.isSourceValid(player));
        }
        player.containerMenu = menu;
        return menu;
    }

    private void nativeSlotRolesAndPermissionsOnServer(MinecraftServer server) {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        var config = QuickShulker.getConfig();
        boolean oldEnabled = config.quickContainers, oldCreative = config.quickContainersCreativeOnly;
        try {
            config.quickContainers = true;
            config.quickContainersCreativeOnly = false;
            Item[] furnaces = {Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER};
            MenuType<?>[] types = {MenuType.FURNACE, MenuType.BLAST_FURNACE, MenuType.SMOKER};
            for (int i = 0; i < furnaces.length; i++) {
                var host = new ItemStack(furnaces[i]);
                var menu = bind(player, host);
                assertEquals(types[i], menu.getType());
                assertEquals(39, menu.slots.size());
                assertEquals(56, menu.getSlot(0).x);
                assertEquals(17, menu.getSlot(0).y);
                assertEquals(56, menu.getSlot(1).x);
                assertEquals(53, menu.getSlot(1).y);
                assertTrue(menu.getSlot(1).mayPlace(new ItemStack(Items.COAL)));
                assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)));
                assertFalse(menu.getSlot(2).mayPlace(new ItemStack(Items.DIAMOND)));
                menu.getSlot(0).set(new ItemStack(Items.COBBLESTONE, 3));
                assertEquals(3, new ItemStackInventory(host, 3).getItem(0).getCount());
                var hostSlot = menu.slots.stream().filter(s -> s.container == player.getInventory() && s.getContainerSlot() == 0).findFirst().orElseThrow();
                assertFalse(hostSlot.mayPickup(player));
                assertFalse(hostSlot.mayPlace(new ItemStack(furnaces[i])));
            }
            var brewing = bind(player, new ItemStack(Items.BREWING_STAND));
            assertEquals(MenuType.BREWING_STAND, brewing.getType());
            assertEquals(41, brewing.slots.size());
            assertTrue(brewing.getSlot(0).mayPlace(new ItemStack(Items.GLASS_BOTTLE)));
            assertFalse(brewing.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)));
            assertTrue(brewing.getSlot(3).mayPlace(new ItemStack(Items.NETHER_WART)));
            assertTrue(brewing.getSlot(4).mayPlace(new ItemStack(Items.BLAZE_POWDER)));
            assertFalse(brewing.getSlot(4).mayPlace(new ItemStack(Items.COAL)));
            config.quickBrewingStand = false;
            assertFalse(brewing.stillValid(player), "Child policy immediately invalidates an open menu");
            config.quickBrewingStand = true;
            config.quickContainers = false;
            assertFalse(PortableContainers.allowed(player, new ItemStack(Items.CHEST)));
            assertTrue(net.kyrptonaught.quickshulker.api.Util.isOpenableItem(new ItemStack(Items.LOOM, 8)), "Loom is stackable and independent from container master");
            config.quickContainers = true;
            config.quickContainersCreativeOnly = true;
            assertFalse(PortableContainers.allowed(player, new ItemStack(Items.CHEST)), "Creative-only refuses survival");
        } finally {
            config.quickContainers = oldEnabled; config.quickContainersCreativeOnly = oldCreative;
            player.containerMenu = player.inventoryMenu; player.getInventory().clearContent();
        }
    }

    private void craftingLocksRemaindersAndAtomicRefusalOnServer(MinecraftServer server) {
        var player = FakePlayerFactory.getMinecraft(server.overworld());
        var config = QuickShulker.getConfig();
        boolean oldEnabled = config.quickContainers, oldCreative = config.quickContainersCreativeOnly;
        try {
            config.quickContainers = true; config.quickContainersCreativeOnly = false;
            var host = new ItemStack(Items.CRAFTER);
            var menu = (PortableCrafterMenu)bind(player, host);
            assertEquals(46, menu.slots.size(), "Nine inputs plus inventory plus noninteractive preview");
            assertTrue(menu.clickMenuButton(player, PortableCrafterMenu.DISABLE + 4));
            assertTrue(menu.isSlotDisabled(4));
            assertFalse(menu.getSlot(4).mayPlace(new ItemStack(Items.STONE)));
            assertArrayEquals(new int[]{4}, host.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId().getIntArray("disabled_slots").orElseThrow());
            assertTrue(menu.clickMenuButton(player, PortableCrafterMenu.ENABLE + 4));
            assertFalse(menu.isSlotDisabled(4));
            menu.getSlot(0).set(new ItemStack(Items.OAK_LOG, 2));
            assertFalse(menu.clickMenuButton(player, PortableCrafterMenu.DISABLE));
            assertFalse(menu.isSlotDisabled(0), "Occupied slots cannot be locked");
            assertTrue(menu.clickMenuButton(player, PortableCrafterMenu.CRAFT));
            assertEquals(1, menu.getSlot(0).getItem().getCount());
            assertEquals(4, count(player, Items.OAK_PLANKS));
            assertSame(host, player.getInventory().getItem(0), "Craft result never replaces its host");
            assertEquals(1, new ItemStackInventory(host, 9).getItem(0).getCount(), "Persisted host cannot roll back consumed ingredients");
            assertFalse(menu.getSlot(45).mayPickup(player), "Preview cannot duplicate output");

            for (int i=1; i<36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            assertFalse(menu.craftOnce(player));
            assertEquals(1, menu.getSlot(0).getItem().getCount(), "Full inventory does not consume input");
            for (int i=1; i<36; i++) assertEquals(64, player.getInventory().getItem(i).getCount());

            host = new ItemStack(Items.CRAFTER);
            menu = (PortableCrafterMenu)bind(player, host);
            Item[] ingredients = {Items.MILK_BUCKET,Items.MILK_BUCKET,Items.MILK_BUCKET,Items.SUGAR,Items.EGG,Items.SUGAR,Items.WHEAT,Items.WHEAT,Items.WHEAT};
            for(int i=0;i<9;i++) menu.getSlot(i).set(new ItemStack(ingredients[i]));
            for(int i=2;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
            assertFalse(menu.craftOnce(player), "One free slot fits cake, not cake plus buckets");
            for(int i=0;i<9;i++) assertEquals(1,menu.getSlot(i).getItem().getCount(), "Remainder reservation is atomic");
            player.getInventory().setItem(2,ItemStack.EMPTY);
            assertTrue(menu.craftOnce(player));
            assertEquals(1,count(player,Items.CAKE));
            assertEquals(3,count(player,Items.BUCKET));
            for(int i=0;i<9;i++) assertTrue(menu.getSlot(i).getItem().isEmpty());

            menu.getSlot(0).set(new ItemStack(Items.OAK_LOG, 2));
            var saved = host.get(DataComponents.CONTAINER);
            player.getInventory().setItem(0,host.copy());
            assertFalse(menu.craftOnce(player), "Replacing source invalidates craft request");
            assertFalse(menu.clickMenuButton(player, PortableCrafterMenu.DISABLE + 8));
            assertEquals(saved,host.get(DataComponents.CONTAINER));
            assertFalse(menu.clickMenuButton(player, -1));
            assertFalse(menu.clickMenuButton(player, 999));
        } finally {
            config.quickContainers = oldEnabled; config.quickContainersCreativeOnly = oldCreative;
            player.containerMenu = player.inventoryMenu; player.getInventory().clearContent();
        }
    }
    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
