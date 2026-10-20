package net.kyrptonaught.quickshulker;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.kyrptonaught.quickshulker.container.*;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.*;
import org.apache.commons.lang3.math.Fraction;

public final class PortableWorldTests {
    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,tickLimit=100)
    public void largeContainerCapacity(TestContext context){
        var server=context.getWorld().getServer();
        boolean ams=Boolean.getBoolean("quickshulker.test.ams");
        if(ams){
            check(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("carpet-ams-addition"),"AMS profile must load real AMS");
            var root=server.getCommandManager().getDispatcher().getRoot().getChild("carpet");
            check(root!=null,"Real Carpet rule command is registered");
            server.getCommandManager().executeWithPrefix(server.getCommandSource(),root.getName()+" largeShulkerBox true");
            server.getCommandManager().executeWithPrefix(server.getCommandSource(),root.getName()+" largeEnderChest true");
            check(net.kyrptonaught.quickshulker.compat.ams.AMSCompat.isLargeShulkerBoxEnabled(),"AMS enabled shulker rule must be detected");
            check(net.kyrptonaught.quickshulker.compat.ams.AMSCompat.isLargeEnderChestEnabled(),"AMS enabled ender rule must be detected");
        }
        var player=context.createMockCreativeServerPlayerInWorld();
        var host=new ItemStack(Items.SHULKER_BOX);player.getInventory().setStack(0,host);
        net.kyrptonaught.quickshulker.api.Util.openItem(player,0,0);
        var menu=player.currentScreenHandler;
        int size=ams?54:27;
        check(menu.slots.size()==size+36,"Quick shulker has expected actual capacity");
        menu.getSlot(size-1).setStack(new ItemStack(Items.DIAMOND,37));player.closeHandledScreen();
        net.kyrptonaught.quickshulker.api.Util.openItem(player,0,0);menu=player.currentScreenHandler;
        check(menu.getSlot(size-1).getStack().getCount()==37,"Last slot persists across close and reopen");
        int source=size+27;
        for(var action:net.minecraft.screen.slot.SlotActionType.values()){
            menu.onSlotClick(source,0,action,player);
            check(player.getInventory().getStack(0)==host,"Source survives every vanilla click action: "+action);
        }
        menu.onSlotClick(0,0,net.minecraft.screen.slot.SlotActionType.SWAP,player);
        check(player.getInventory().getStack(0)==host,"Number-key swap cannot replace source");
        player.getInventory().setStack(0,new ItemStack(Items.ENDER_CHEST));player.currentScreenHandler=player.playerScreenHandler;
        net.kyrptonaught.quickshulker.api.Util.openItem(player,0,0);
        check(player.currentScreenHandler.slots.size()==player.getEnderChestInventory().size()+36,"Ender menu matches real inventory");
        if(ams)check(player.getEnderChestInventory().size()==54,"Real AMS ender inventory expands to 54");
        player.currentScreenHandler=player.playerScreenHandler;
        if(ams){
            var root=server.getCommandManager().getDispatcher().getRoot().getChild("carpet");
            server.getCommandManager().executeWithPrefix(server.getCommandSource(),root.getName()+" largeShulkerBox false");
            server.getCommandManager().executeWithPrefix(server.getCommandSource(),root.getName()+" largeEnderChest false");
            check(!net.kyrptonaught.quickshulker.compat.ams.AMSCompat.isLargeShulkerBoxEnabled()&&!net.kyrptonaught.quickshulker.compat.ams.AMSCompat.isLargeEnderChestEnabled(),"Disabled rules detected immediately");
            player.getInventory().setStack(0,host);net.kyrptonaught.quickshulker.api.Util.openItem(player,0,0);
            check(player.currentScreenHandler.getSlot(53).getStack().getCount()==37,"Turning rule off never truncates high slots");
            player.currentScreenHandler=player.playerScreenHandler;
        }
        // Mouse drag uses repeated vanilla right-click PICKUP actions with the shulker on the cursor.
        // Exercise that real server path even when the 54-slot rule is off.
        player.getInventory().clear();
        var held=new ItemStack(Items.SHULKER_BOX);
        new ItemStackInventory(held,54).setStack(53,new ItemStack(Items.DIAMOND,37));
        var inventoryMenu=player.playerScreenHandler;inventoryMenu.setCursorStack(held);
        player.getInventory().setStack(9,new ItemStack(Items.STONE,5));
        player.getInventory().setStack(10,new ItemStack(Items.STONE,7));
        inventoryMenu.onSlotClick(9,1,net.minecraft.screen.slot.SlotActionType.PICKUP,player);
        inventoryMenu.onSlotClick(10,1,net.minecraft.screen.slot.SlotActionType.PICKUP,player);
        check(inventoryMenu.getCursorStack()==held,"Repeated held-shulker clicks preserve item identity");
        check(new ItemStackInventory(held,27).getStack(53).getCount()==37,"Held-shulker insertion never truncates slot 54");
        check(held.get(DataComponentTypes.CONTAINER).stream().filter(s->s.isOf(Items.STONE)).mapToInt(ItemStack::getCount).sum()==12,"Repeated pickup inserts exactly 12 stone");
        inventoryMenu.onSlotClick(11,1,net.minecraft.screen.slot.SlotActionType.PICKUP,player);
        int diamonds=count(player,Items.DIAMOND)+held.get(DataComponentTypes.CONTAINER).stream().filter(s->s.isOf(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
        check(diamonds==37,"Held-shulker extraction conserves all high-slot diamonds");
        inventoryMenu.setCursorStack(ItemStack.EMPTY);
        context.complete();
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,tickLimit=100)
    public void containerSourceValidation(TestContext context){
        var player=context.createMockCreativeServerPlayerInWorld();
        var chest=new net.minecraft.inventory.SimpleInventory(27);
        var host=new ItemStack(Items.SHULKER_BOX);chest.setStack(0,host);
        var original=GenericContainerScreenHandler.createGeneric9x3(23,player.getInventory(),chest);
        player.currentScreenHandler=original;
        var config=QuickShulkerMod.getConfig();boolean old=config.rightClickContainerShulker;
        try{
            config.rightClickContainerShulker=true;
            int[] bad={-1,original.slots.size(),Integer.MAX_VALUE};
            for(int slot:bad){net.kyrptonaught.quickshulker.api.Util.openItemFromScreenSlot(player,23,slot);check(player.currentScreenHandler==original,"Invalid screen slot rejected");}
            net.kyrptonaught.quickshulker.api.Util.openItemFromScreenSlot(player,22,0);check(player.currentScreenHandler==original,"Stale sync id rejected");
            original.setCursorStack(new ItemStack(Items.STONE));net.kyrptonaught.quickshulker.api.Util.openItemFromScreenSlot(player,23,0);
            check(player.currentScreenHandler==original,"Occupied cursor rejected");original.setCursorStack(ItemStack.EMPTY);
            host.setCount(2);net.kyrptonaught.quickshulker.api.Util.openItemFromScreenSlot(player,23,0);check(player.currentScreenHandler==original,"Multiple shulkers rejected");host.setCount(1);
            config.rightClickContainerShulker=false;net.kyrptonaught.quickshulker.api.Util.openItemFromScreenSlot(player,23,0);check(player.currentScreenHandler==original,"Disabled container option rejected");config.rightClickContainerShulker=true;
            net.kyrptonaught.quickshulker.api.Util.openItemFromScreenSlot(player,23,0);var opened=player.currentScreenHandler;
            check(opened!=original&&((ItemInventoryContainer)opened).getSourceInventory()==chest,"Real chest slot opens and retains source");
            opened.getSlot(0).setStack(new ItemStack(Items.EMERALD,11));
            check(new ItemStackInventory(host,27).getStack(0).getCount()==11,"Edits persist to exact chest item");
            var replacement=host.copy();chest.setStack(0,replacement);opened.sendContentUpdates();
            check(player.currentScreenHandler==player.playerScreenHandler,"External source replacement closes menu");
            opened.getSlot(0).setStack(new ItemStack(Items.EMERALD,20));
            check(new ItemStackInventory(replacement,27).getStack(0).getCount()==11,"Stale inventory cannot overwrite replacement");
        }finally{config.rightClickContainerShulker=old;player.currentScreenHandler=player.playerScreenHandler;}
        context.complete();
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static ScreenHandler bind(ServerPlayerEntity player,ItemStack host){
        player.currentScreenHandler=player.playerScreenHandler;player.getInventory().clear();player.getInventory().setStack(0,host);
        ScreenHandler menu=host.getItem() instanceof BundleItem?
            new PortableBundleMenu(1,player.getInventory(),new PortableBundleInventory(host)):
            PortableContainers.create(1,player.getInventory(),host,PortableContainers.kind(host));
        var session=(ItemInventoryContainer)menu;session.setUsedSlot(0);session.setSourceSlot(player,player.getInventory(),0,host);
        for(var slot:menu.slots)if(slot.inventory instanceof ItemStackInventory contents){
            contents.bindSourceSession(session);contents.setWriteAllowed(()->session.isSourceValid(player));
        }
        player.currentScreenHandler=menu;return menu;
    }
    private static int count(ServerPlayerEntity player,Item item){return player.getInventory().main.stream().filter(s->s.isOf(item)).mapToInt(ItemStack::getCount).sum();}

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,tickLimit=100)
    public void sameTypeContainerNesting(TestContext context){
        var player=context.createMockCreativeServerPlayerInWorld();
        var config=QuickShulkerMod.getConfig();
        boolean enabled=config.quickContainers,creative=config.quickContainersCreativeOnly;
        try{
            config.quickContainers=true;config.quickContainersCreativeOnly=false;
            for(Item item:new Item[]{Items.CHEST,Items.TRAPPED_CHEST,Items.BARREL,Items.HOPPER,Items.DROPPER,Items.DISPENSER}){
                var host=new ItemStack(item);var child=new ItemStack(item);
                int capacity=PortableContainers.kind(host).slots;
                new ItemStackInventory(child,capacity).setStack(0,new ItemStack(Items.DIAMOND,7));
                var menu=bind(player,host);
                check(menu.getSlot(0).canInsert(child),"Same-type container is accepted: "+item);
                check(!menu.getSlot(0).canInsert(host),"Exact host cannot be inserted into itself");
                player.getInventory().setStack(1,child);
                int childSlot=menu.slots.stream().filter(s->s.inventory==player.getInventory()&&s.getIndex()==1).findFirst().orElseThrow().id;
                check(!menu.quickMove(player,childSlot).isEmpty(),"Shift-click inserts same-type container");
                check(player.getInventory().getStack(1).isEmpty(),"Insertion removes exactly the carried child");
                var stored=new ItemStackInventory(host,capacity).getStack(0);
                check(stored.isOf(item)&&new ItemStackInventory(stored,capacity).getStack(0).getCount()==7,
                    "Nested container and its contents persist");
                var reopened=PortableContainers.create(2,player.getInventory(),host,PortableContainers.kind(host));
                check(reopened.getSlot(0).getStack().isOf(item),"Reopening restores the nested container");
                check(!menu.quickMove(player,0).isEmpty(),"Nested container can be extracted");
                check(new ItemStackInventory(host,capacity).isEmpty(),"Extraction clears outer storage");
                check(count(player,item)==2&&player.getInventory().getStack(0)==host,"Host and child are conserved");
            }
            var largeShulker=new PortableStorageHandler(3,player.getInventory(),new ItemStack(Items.SHULKER_BOX),
                new PortableContainers.Layout(54,54,ScreenHandlerType.GENERIC_9X6));
            check(!largeShulker.getSlot(0).canInsert(new ItemStack(Items.WHITE_SHULKER_BOX)),"Large shulker still rejects other shulkers");
            check(largeShulker.getSlot(0).canInsert(new ItemStack(Items.CHEST)),"Large shulker still accepts chests");
        }finally{
            config.quickContainers=enabled;config.quickContainersCreativeOnly=creative;
            player.currentScreenHandler=player.playerScreenHandler;
        }
        context.complete();
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,tickLimit=100)
    public void nativeContainerRoles(TestContext context){
        var player=context.createMockCreativeServerPlayerInWorld();var config=QuickShulkerMod.getConfig();
        boolean enabled=config.quickContainers,creative=config.quickContainersCreativeOnly,brewEnabled=config.quickBrewingStand;
        try{
            config.quickContainers=true;config.quickContainersCreativeOnly=false;config.quickBrewingStand=true;
            Item[] items={Items.FURNACE,Items.BLAST_FURNACE,Items.SMOKER};
            ScreenHandlerType<?>[] types={ScreenHandlerType.FURNACE,ScreenHandlerType.BLAST_FURNACE,ScreenHandlerType.SMOKER};
            for(int i=0;i<items.length;i++){
                var host=new ItemStack(items[i]);var menu=bind(player,host);
                check(menu.getType()==types[i]&&menu.slots.size()==39,"Native three-slot furnace menu");
                check(menu.getSlot(0).x==56&&menu.getSlot(0).y==17,"Input slot geometry");
                check(menu.getSlot(1).canInsert(new ItemStack(Items.COAL)),"Fuel accepts coal");
                check(!menu.getSlot(1).canInsert(new ItemStack(Items.DIAMOND)),"Fuel refuses diamond");
                check(!menu.getSlot(2).canInsert(new ItemStack(Items.STONE)),"Output rejects insertion");
                menu.getSlot(0).setStack(new ItemStack(Items.COBBLESTONE,3));
                check(new ItemStackInventory(host,3).getStack(0).getCount()==3,"Native input persists in host");
                var source=menu.slots.stream().filter(s->s.inventory==player.getInventory()&&s.getIndex()==0).findFirst().orElseThrow();
                check(!source.canTakeItems(player)&&!source.canInsert(new ItemStack(items[i])),"Source cannot move or merge");
            }
            var brewing=bind(player,new ItemStack(Items.BREWING_STAND));
            check(brewing.slots.size()==41&&brewing.getType()==ScreenHandlerType.BREWING_STAND,"Five native brewing roles");
            check(brewing.getSlot(0).canInsert(new ItemStack(Items.GLASS_BOTTLE)),"Bottle accepts bottle");
            check(!brewing.getSlot(0).canInsert(new ItemStack(Items.DIAMOND)),"Bottle rejects unrelated item");
            check(brewing.getSlot(3).canInsert(new ItemStack(Items.NETHER_WART)),"Ingredient slot");
            check(brewing.getSlot(4).canInsert(new ItemStack(Items.BLAZE_POWDER))&&!brewing.getSlot(4).canInsert(new ItemStack(Items.COAL)),"Brewing fuel role");
            config.quickBrewingStand=false;check(!brewing.canUse(player),"Child off invalidates live session");config.quickBrewingStand=true;
            var pot=bind(player,new ItemStack(Items.DECORATED_POT));check(pot.slots.size()==37,"Real one-slot pot");
            pot.getSlot(0).setStack(new ItemStack(Items.EMERALD,7));check(new ItemStackInventory(player.getInventory().getStack(0),1).getStack(0).getCount()==7,"Pot contents persist");
            config.quickContainers=false;check(!pot.canUse(player),"Master off invalidates portable menu");
            check(net.kyrptonaught.quickshulker.api.Util.isOpenableItem(new ItemStack(Items.LOOM,8)),"Loom independent and stackable");
        }finally{config.quickContainers=enabled;config.quickContainersCreativeOnly=creative;config.quickBrewingStand=brewEnabled;player.currentScreenHandler=player.playerScreenHandler;}
        context.complete();
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,tickLimit=100)
    public void crafterTransactions(TestContext context){
        var player=context.createMockCreativeServerPlayerInWorld();var config=QuickShulkerMod.getConfig();
        boolean enabled=config.quickContainers,creative=config.quickContainersCreativeOnly;
        try{
            config.quickContainers=true;config.quickContainersCreativeOnly=false;
            var host=new ItemStack(Items.CRAFTER);var menu=(PortableCrafterMenu)bind(player,host);
            check(menu.onButtonClick(player,4)&&menu.isSlotDisabled(4),"Empty slot can lock");
            check(!menu.getSlot(4).canInsert(new ItemStack(Items.STONE)),"Locked input rejects insertion");
            check(host.get(DataComponentTypes.BLOCK_ENTITY_DATA).copyNbt().getIntArray("disabled_slots")[0]==4,"Lock persists");
            var ops=context.getWorld().getRegistryManager().getOps(net.minecraft.nbt.NbtOps.INSTANCE);
            var encoded=ItemStack.CODEC.encodeStart(ops,host).getOrThrow();
            var decoded=ItemStack.CODEC.parse(ops,encoded).getOrThrow();
            check(decoded.get(DataComponentTypes.BLOCK_ENTITY_DATA).copyNbt().getIntArray("disabled_slots")[0]==4,"Crafter locks survive real item serialization");
            menu=(PortableCrafterMenu)bind(player,decoded);host=decoded;
            check(menu.isSlotDisabled(4),"Decoded crafter reopens with its saved lock");
            var extra=host.get(DataComponentTypes.BLOCK_ENTITY_DATA).copyNbt();extra.putString("more_test_marker","keep");
            host.set(DataComponentTypes.BLOCK_ENTITY_DATA,net.minecraft.component.type.NbtComponent.of(extra));
            check(menu.onButtonClick(player,PortableCrafterMenu.ENABLE+4)&&!menu.isSlotDisabled(4),"Slot unlocks");
            check(host.get(DataComponentTypes.BLOCK_ENTITY_DATA).copyNbt().getString("more_test_marker").equals("keep"),"Lock changes preserve unrelated block entity fields");
            menu.getSlot(0).setStack(new ItemStack(Items.OAK_LOG,2));check(!menu.onButtonClick(player,0),"Cannot lock occupied input");
            check(menu.onButtonClick(player,PortableCrafterMenu.CRAFT),"Real log recipe crafts");
            check(count(player,Items.OAK_PLANKS)==4&&menu.getSlot(0).getStack().getCount()==1,"Output and consumption match recipe");
            check(player.getInventory().getStack(0)==host&&new ItemStackInventory(host,9).getStack(0).getCount()==1,"Host identity preserved; no stale snapshot rollback");
            check(!menu.getSlot(45).canTakeItems(player),"Result preview cannot be extracted");
            for(int i=1;i<36;i++)player.getInventory().setStack(i,new ItemStack(Items.COBBLESTONE,64));
            check(!menu.craftOnce(player)&&menu.getSlot(0).getStack().getCount()==1,"Full inventory refuses without consuming");
            host=new ItemStack(Items.CRAFTER);menu=(PortableCrafterMenu)bind(player,host);
            Item[] inputs={Items.MILK_BUCKET,Items.MILK_BUCKET,Items.MILK_BUCKET,Items.SUGAR,Items.EGG,Items.SUGAR,Items.WHEAT,Items.WHEAT,Items.WHEAT};
            for(int i=0;i<9;i++)menu.getSlot(i).setStack(new ItemStack(inputs[i]));
            for(int i=2;i<36;i++)player.getInventory().setStack(i,new ItemStack(Items.COBBLESTONE,64));
            check(!menu.craftOnce(player),"Cake and empty buckets must both fit");
            for(int i=0;i<9;i++)check(menu.getSlot(i).getStack().getCount()==1,"No partial consumption on remainder refusal");
            player.getInventory().setStack(2,ItemStack.EMPTY);check(menu.craftOnce(player),"Cake recipe crafts once all outputs fit");
            check(count(player,Items.CAKE)==1&&count(player,Items.BUCKET)==3,"All bucket remainders returned");
            for(int i=0;i<9;i++)check(menu.getSlot(i).getStack().isEmpty(),"Cake consumes all nine inputs");
            menu.getSlot(0).setStack(new ItemStack(Items.OAK_LOG,2));var saved=host.get(DataComponentTypes.CONTAINER);
            player.getInventory().setStack(0,host.copy());check(!menu.craftOnce(player)&&!menu.onButtonClick(player,8),"Replaced source refuses all mutations");
            check(saved.equals(host.get(DataComponentTypes.CONTAINER)),"Expired source cannot overwrite host");
            check(!menu.onButtonClick(player,-1)&&!menu.onButtonClick(player,999),"Forged button bounds rejected");
        }finally{config.quickContainers=enabled;config.quickContainersCreativeOnly=creative;player.currentScreenHandler=player.playerScreenHandler;}
        context.complete();
    }

    @GameTest(templateName=FabricGameTest.EMPTY_STRUCTURE,tickLimit=100)
    public void experimentalBundleStorage(TestContext context){
        var player=context.createMockCreativeServerPlayerInWorld();
        check(Items.BUNDLE.isEnabled(context.getWorld().getEnabledFeatures()),"Test world actually enables experimental bundles");
        check(Items.BUNDLE.isEnabled(net.minecraft.resource.featuretoggle.FeatureSet.of(net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA,net.minecraft.resource.featuretoggle.FeatureFlags.BUNDLE)),"1.21.1 experimental bundle exists");
        var host=new ItemStack(Items.BUNDLE);var menu=(PortableBundleMenu)bind(player,host);
        player.getInventory().setStack(1,new ItemStack(Items.DIAMOND,64));
        check(!menu.quickMove(player,92).isEmpty(),"Shift inserts through real bundle weight limits");
        check(host.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS,BundleContentsComponent.DEFAULT).getOccupancy().equals(Fraction.ONE),"Bundle fills at vanilla capacity");
        check(count(player,Items.DIAMOND)==0,"Inserted items leave player inventory once");
        player.getInventory().setStack(2,new ItemStack(Items.STONE,10));
        check(menu.quickMove(player,93).isEmpty()&&player.getInventory().getStack(2).getCount()==10,"Full bundle does not consume more items");
        check(!menu.quickMove(player,0).isEmpty(),"Shift withdraw works");
        check(count(player,Items.DIAMOND)==64&&host.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS,BundleContentsComponent.DEFAULT).isEmpty(),"Withdrawal conserves all items");
        check(!menu.getSlot(1).canInsert(new ItemStack(Items.BUNDLE)),"Quick GUI refuses recursive bundle insertion");
        menu.scroll(99);check(menu.firstRow()==3,"Scroll lower bound");menu.scroll(-99);check(menu.firstRow()==0,"Scroll upper bound");
        var source=menu.slots.stream().filter(s->s.inventory==player.getInventory()&&s.getIndex()==0).findFirst().orElseThrow();
        check(!source.canTakeItems(player),"Opened experimental bundle source is protected");
        player.getInventory().setStack(0,host.copy());check(!menu.canUse(player),"Bundle source replacement invalidates menu");
        player.currentScreenHandler=player.playerScreenHandler;
        context.complete();
    }
}
