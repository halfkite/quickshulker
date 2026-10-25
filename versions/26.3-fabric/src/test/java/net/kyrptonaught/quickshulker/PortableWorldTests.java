package net.kyrptonaught.quickshulker;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.kyrptonaught.quickshulker.container.*;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Runs with real levels, recipes, players and applied production Mixins, without reflection. */
public final class PortableWorldTests {
    @GameTest(maxTicks = 100)
    public void childPermissionModes(GameTestHelper helper) {
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var c=QuickShulkerMod.getConfig();
        boolean enabled=c.quickContainers,creative=c.quickContainersCreativeOnly;
        int mask=ContainerKind.mask(c),creativeMask=c.containerCreativeMask;
        try {
            c.quickContainers=true;c.quickContainersCreativeOnly=false;
            for(var kind:ContainerKind.values()) {
                var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                    .filter(candidate->PortableContainers.kind(new ItemStack(candidate))==kind)
                    .findFirst().orElseThrow(()->new AssertionError("No item for container kind: "+kind));
                var stack=new ItemStack(item);
                for(int mode=0;mode<3;mode++) {
                    kind.setMode(c,mode);
                    player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
                    check(PortableContainers.allowed(player,stack)==(mode==1),"Survival child permission: "+kind);
                    player=helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
                    check(PortableContainers.allowed(player,stack)==(mode!=2),"Creative child permission: "+kind);
                    player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SPECTATOR);
                    check(!PortableContainers.allowed(player,stack),"Spectator never opens child: "+kind);
                }
                kind.setMode(c,1);
            }
            player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            c.quickContainersCreativeOnly=true;
            check(!PortableContainers.allowed(player,new ItemStack(Items.CHEST)),"Master creative restriction remains a ceiling");
            player=helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
            c.quickContainers=false;
            check(!PortableContainers.allowed(player,new ItemStack(Items.CHEST)),"Master false denies even creative");
        } finally {
            c.quickContainers=enabled;c.quickContainersCreativeOnly=creative;
            ContainerKind.applyMask(c,mask);c.containerCreativeMask=creativeMask;
        }
        helper.succeed();
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    private static AbstractContainerMenu bind(ServerPlayer player, ItemStack host) {
        player.containerMenu = player.inventoryMenu;
        player.getInventory().clearContent();
        player.getInventory().setItem(0, host);
        var menu = PortableContainers.create(1, player.getInventory(), host, PortableContainers.kind(host));
        var session = (ItemInventoryContainer) menu;
        session.setUsedSlot(0);
        session.setSourceSlot(player, player.getInventory(), 0, host);
        for (var slot : menu.slots) if (slot.container instanceof ItemStackInventory contents) {
            contents.bindSourceSession(session);
            contents.setWriteAllowed(() -> session.isSourceValid(player));
        }
        player.containerMenu = menu;
        return menu;
    }

    @GameTest(maxTicks = 100)
    public void sameTypeContainerNesting(GameTestHelper context){
        var player=context.makeMockServerPlayerInLevel();
        var config=QuickShulkerMod.getConfig();
        boolean enabled=config.quickContainers,creative=config.quickContainersCreativeOnly;
        try{
            config.quickContainers=true;config.quickContainersCreativeOnly=false;
            for(Item item:new Item[]{Items.CHEST,Items.TRAPPED_CHEST,Items.BARREL,Items.HOPPER,Items.DROPPER,Items.DISPENSER}){
                var host=new ItemStack(item);var child=new ItemStack(item);
                int capacity=PortableContainers.kind(host).slots;
                new ItemStackInventory(child,capacity).setItem(0,new ItemStack(Items.DIAMOND,7));
                var menu=bind(player,host);
                check(menu.getSlot(0).mayPlace(child),"Same-type container is accepted: "+item);
                check(!menu.getSlot(0).mayPlace(host),"Exact host cannot be inserted into itself");
                player.getInventory().setItem(1,child);
                int childSlot=menu.slots.stream().filter(s->s.container==player.getInventory()&&s.getContainerSlot()==1).findFirst().orElseThrow().index;
                check(!menu.quickMoveStack(player,childSlot).isEmpty(),"Shift-click inserts same-type container");
                check(player.getInventory().getItem(1).isEmpty(),"Insertion removes exactly the carried child");
                var stored=new ItemStackInventory(host,capacity).getItem(0);
                check(stored.is(item)&&new ItemStackInventory(stored,capacity).getItem(0).getCount()==7,
                    "Nested container and its contents persist");
                var reopened=PortableContainers.create(2,player.getInventory(),host,PortableContainers.kind(host));
                check(reopened.getSlot(0).getItem().is(item),"Reopening restores the nested container");
                check(!menu.quickMoveStack(player,0).isEmpty(),"Nested container can be extracted");
                check(new ItemStackInventory(host,capacity).isEmpty(),"Extraction clears outer storage");
                check(count(player,item)==2&&player.getInventory().getItem(0)==host,"Host and child are conserved");
            }
            var largeShulker=new PortableStorageHandler(3,player.getInventory(),new ItemStack(Items.SHULKER_BOX),
                new PortableContainers.Layout(54,54,MenuType.GENERIC_9x6));
            var whiteShulker=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft","white_shulker_box"));
            check(!largeShulker.getSlot(0).mayPlace(new ItemStack(whiteShulker)),"Large shulker still rejects other shulkers");
            check(largeShulker.getSlot(0).mayPlace(new ItemStack(Items.CHEST)),"Large shulker still accepts chests");
        }finally{
            config.quickContainers=enabled;config.quickContainersCreativeOnly=creative;
            player.containerMenu=player.inventoryMenu;
        }
        context.succeed();
    }

    @GameTest(maxTicks = 100)
    public void nativeMenusAndInitialSync(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var config = QuickShulkerMod.getConfig();
        boolean enabled = config.quickContainers, creative = config.quickContainersCreativeOnly;
        try {
            config.quickContainers = true; config.quickContainersCreativeOnly = false;
            for (var item : new Item[]{Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER}) {
                var host = new ItemStack(item);
                var menu = bind(player, host);
                check(menu.slots.size() == 39, "Furnace has three real slots");
                check(menu.getSlot(1).mayPlace(new ItemStack(Items.COAL)), "Fuel slot accepts coal");
                check(!menu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)), "Fuel slot rejects diamond");
                check(!menu.getSlot(2).mayPlace(new ItemStack(Items.DIAMOND)), "Output slot is read-only");
                menu.getSlot(0).set(new ItemStack(Items.COBBLESTONE, 3));
                check(new ItemStackInventory(host, 3).getItem(0).getCount() == 3, "Furnace input persists");
            }
            var brewing = bind(player, new ItemStack(Items.BREWING_STAND));
            check(brewing.slots.size() == 41, "Brewing stand has five real slots");
            check(brewing.getSlot(0).mayPlace(new ItemStack(Items.GLASS_BOTTLE)), "Potion slot accepts bottles");
            check(!brewing.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Potion slot rejects diamond");
            check(brewing.getSlot(3).mayPlace(new ItemStack(Items.NETHER_WART)), "Ingredient slot accepts wart");
            check(brewing.getSlot(4).mayPlace(new ItemStack(Items.BLAZE_POWDER)), "Brewing fuel accepts blaze powder");
            check(!brewing.getSlot(4).mayPlace(new ItemStack(Items.COAL)), "Brewing fuel rejects coal");
            // Deliver the full initial server property array into the actual vanilla client-sized menu.
            // A copied 26.3 data count must fail here on 26.1/26.2, not merely pass compilation.
            var clientMenu = new BrewingStandMenu(1, player.getInventory());
            boolean[] synced = {false};
            brewing.setSynchronizer(new ContainerSynchronizer() {
                public void sendInitialData(AbstractContainerMenu menu, java.util.List<ItemStack> items, ItemStack carried, int[] data) {
                    for (int i = 0; i < data.length; i++) clientMenu.setData(i, data[i]);
                    synced[0] = true;
                }
                public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {}
                public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {}
                public void sendDataChange(AbstractContainerMenu menu, int index, int value) { clientMenu.setData(index, value); }
                public RemoteSlot createSlot() { return RemoteSlot.PLACEHOLDER; }
            });
            check(synced[0], "Real initial property synchronization ran");
            var pot = bind(player, new ItemStack(Items.DECORATED_POT));
            check(pot.slots.size() == 37, "Pot has one slot, not a hopper");
            config.quickContainers = false;
            check(!PortableContainers.allowed(player, new ItemStack(Items.CHEST)), "Master disables storage");
            check(net.kyrptonaught.quickshulker.api.Util.isOpenableItem(new ItemStack(Items.LOOM, 8)), "Stacked loom is independent from storage policy");
        } finally {
            config.quickContainers = enabled; config.quickContainersCreativeOnly = creative;
            player.containerMenu = player.inventoryMenu; player.getInventory().clearContent();
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void crafterTransactions(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var config = QuickShulkerMod.getConfig();
        boolean enabled = config.quickContainers, creative = config.quickContainersCreativeOnly;
        try {
            config.quickContainers = true; config.quickContainersCreativeOnly = false;
            var host = new ItemStack(Items.CRAFTER);
            var menu = (PortableCrafterMenu) bind(player, host);
            check(menu.clickMenuButton(player, PortableCrafterMenu.DISABLE + 4), "Empty slot can be locked");
            check(menu.isSlotDisabled(4) && !menu.getSlot(4).mayPlace(new ItemStack(Items.STONE)), "Locked slot rejects input");
            check(menu.clickMenuButton(player, PortableCrafterMenu.ENABLE + 4), "Locked slot can be enabled");
            menu.getSlot(0).set(new ItemStack(Items.OAK_LOG, 2));
            check(menu.craftOnce(player), "Real log-to-planks recipe runs");
            check(menu.getSlot(0).getItem().getCount() == 1 && count(player, Items.OAK_PLANKS) == 4, "Recipe consumes one log and produces four planks");
            check(player.getInventory().getItem(0) == host && new ItemStackInventory(host, 9).getItem(0).getCount() == 1, "Host identity and consumed input persist");
            check(!menu.getSlot(45).mayPickup(player), "Preview cannot duplicate output");
            for (int i = 1; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            check(!menu.craftOnce(player) && menu.getSlot(0).getItem().getCount() == 1, "Full inventory refuses without consumption");
            host = new ItemStack(Items.CRAFTER);
            menu = (PortableCrafterMenu) bind(player, host);
            Item[] ingredients = {Items.MILK_BUCKET, Items.MILK_BUCKET, Items.MILK_BUCKET, Items.SUGAR, Items.EGG, Items.SUGAR, Items.WHEAT, Items.WHEAT, Items.WHEAT};
            for (int i = 0; i < 9; i++) menu.getSlot(i).set(new ItemStack(ingredients[i]));
            for (int i = 2; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            check(!menu.craftOnce(player), "One slot cannot reserve cake and buckets together");
            for (int i = 0; i < 9; i++) check(menu.getSlot(i).getItem().getCount() == 1, "Refused remainder transaction preserves every ingredient");
            player.getInventory().setItem(2, ItemStack.EMPTY);
            check(menu.craftOnce(player) && count(player, Items.CAKE) == 1 && count(player, Items.BUCKET) == 3, "Cake recipe returns all buckets atomically");
            menu.getSlot(0).set(new ItemStack(Items.OAK_LOG, 2));
            var saved = host.get(DataComponents.CONTAINER);
            player.getInventory().setItem(0, host.copy());
            check(!menu.craftOnce(player) && !menu.clickMenuButton(player, PortableCrafterMenu.DISABLE + 8), "Replaced host rejects crafting and lock writes");
            check(host.get(DataComponents.CONTAINER).equals(saved), "Invalid session cannot overwrite original host");
        } finally {
            config.quickContainers = enabled; config.quickContainersCreativeOnly = creative;
            player.containerMenu = player.inventoryMenu; player.getInventory().clearContent();
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void variantsPersistenceAndSourceProtection(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var config = QuickShulkerMod.getConfig();
        boolean enabled = config.quickContainers, creative = config.quickContainersCreativeOnly;
        try {
            config.quickContainers = true; config.quickContainersCreativeOnly = false;
            int copper = 0, shelves = 0;
            for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                var host = new ItemStack(block);
                var kind = PortableContainers.kind(host);
                if (kind != ContainerKind.COPPER_CHEST && kind != ContainerKind.SHELF) continue;
                if (kind == ContainerKind.COPPER_CHEST) copper++; else shelves++;
                var menu = bind(player, host);
                check(menu.slots.size() == kind.slots + 36, "Variant exposes exactly its native storage capacity");
                menu.getSlot(kind.slots - 1).set(new ItemStack(Items.DIAMOND, 17));
                var ops = player.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                var restored = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, host).getOrThrow()).getOrThrow();
                check(new ItemStackInventory(restored, kind.slots).getItem(kind.slots - 1).getCount() == 17, "Full variant item serialization preserves last slot");
                var hostSlot = menu.slots.stream().filter(s -> s.container == player.getInventory() && s.getContainerSlot() == 0).findFirst().orElseThrow();
                check(!hostSlot.mayPickup(player) && !hostSlot.mayPlace(host.copy()), "Source slot rejects movement and replacement");
                player.getInventory().setItem(0, host.copy());
                check(!menu.stillValid(player), "Identical source replacement invalidates session");
            }
            check(copper == 8 && shelves >= 12, "All copper and shelf variants were actually exercised");
        } finally {
            config.quickContainers = enabled; config.quickContainersCreativeOnly = creative;
            player.containerMenu = player.inventoryMenu; player.getInventory().clearContent();
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void policyPermissions(GameTestHelper helper) {
        var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
        net.kyrptonaught.quickshulker.command.QuickShulkerCommands.register(dispatcher);
        var root = dispatcher.getRoot().getChild("quickshulker");
        check(root != null && root.getChild("quickContainers") != null, "Master command is registered");
        check(root.getChild("quickCopperChest") != null && root.getChild("quickShelf") != null, "Variant child commands are registered");
        for (var level : net.minecraft.server.permissions.PermissionLevel.values()) {
            var permissions = net.minecraft.server.permissions.LevelBasedPermissionSet.forLevel(level);
            var source = helper.getLevel().getServer().createCommandSourceStack().withPermission(permissions);
            check(root.canUse(source) == permissions.hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER), "Command and policy-update packet retain the same OP gate");
        }
        helper.succeed();
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
