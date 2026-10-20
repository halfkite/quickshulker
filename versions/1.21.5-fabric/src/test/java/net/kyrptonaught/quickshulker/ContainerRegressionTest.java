package net.kyrptonaught.quickshulker;

import io.netty.buffer.Unpooled;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.kyrptonaught.quickshulker.network.OpenShulkerPacket;
import net.kyrptonaught.quickshulker.network.OpenContainerShulkerPacket;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

/** Real component and wire-format regression checks, independent of a running world. */
public final class ContainerRegressionTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void portableMenuPacketOrdering() {
        var executor = new net.minecraft.util.thread.ReentrantThreadExecutor<Runnable>("portable-menu-ordering") {
            private final Thread owner = Thread.currentThread();
            @Override public Runnable createTask(Runnable task) { return task; }
            @Override protected boolean canExecute(Runnable task) { return true; }
            @Override protected Thread getThread() { return owner; }
        };
        // Reproduce the old callback: Fabric queues the receiver, and our nested execute
        // runs after the already queued vanilla OpenScreen packet, despite the same thread.
        var seen = new java.util.ArrayList<Integer>();
        net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.clearPending();
        executor.send(() -> executor.execute(() ->
            net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.prepareClient(1)));
        executor.send(() -> seen.add(net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.takePending()));
        while (executor.runTask()) { }
        check(seen.equals(java.util.List.of(0)), "Old nested scheduling reproduces first-open fallback");
        check(net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.takePending() == 1,
            "Old callback leaves a stale pot envelope for the next menu");

        // Model Fabric's receiver/OpenScreen/content queue for repeated first opens and
        // transitions from another container, including pot -> crafter -> bundle -> chest.
        var inventory = new net.minecraft.entity.player.PlayerInventory(null,new net.minecraft.entity.EntityEquipment());
        var received = new net.minecraft.screen.ScreenHandler[1];
        for (int round = 0; round < 3; round++) {
            for (int kind : new int[] {1, 3, 4, 0}) {
                if (kind != 0) executor.send(() ->
                    net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.prepareClient(kind));
                executor.send(() -> {
                    int pending = net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.takePending();
                    check(pending == kind, "Envelope precedes its matching vanilla menu");
                    received[0] = switch (pending) {
                        case 1 -> new net.kyrptonaught.quickshulker.container.CompactStorageMenu(7, inventory, 1);
                        case 3 -> new net.kyrptonaught.quickshulker.container.PortableCrafterMenu(7, inventory);
                        case 4 -> new net.kyrptonaught.quickshulker.container.PortableBundleMenu(7, inventory);
                        default -> net.minecraft.screen.GenericContainerScreenHandler.createGeneric9x3(7, inventory);
                    };
                });
                executor.send(() -> {
                    check(received[0].slots.size() == switch (kind) { case 1 -> 37; case 3 -> 46; case 4 -> 100; default -> 63; },
                        "Content packet uses the intended storage/player slot layout");
                    if (kind == 3) {
                        received[0].setProperty(0, 1);
                        check(((net.kyrptonaught.quickshulker.container.PortableCrafterMenu) received[0]).isSlotDisabled(0),
                            "Crafter property packet applies to a crafter instead of an empty chest property list");
                    }
                });
            }
            while (executor.runTask()) { }
            check(net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.takePending() == 0,
                "No stale envelope survives container transitions");
        }
    }

    public static void main(String[] args) {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        // Fabric does not promise entrypoint invocation order between separate mods.
        if (QuickShulkerMod.getConfig() == null) new QuickShulkerMod().onInitialize();
        portableMenuPacketOrdering();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        OpenShulkerPacket.CODEC.encode(buf, new OpenShulkerPacket(45));
        check(buf.readableBytes() == Integer.BYTES, "Legacy protocol must contain exactly one int");
        check(OpenShulkerPacket.CODEC.decode(buf).invSlot() == 45 && !buf.isReadable(), "Original server consumes the complete packet");
        buf.clear();
        OpenContainerShulkerPacket.CODEC.encode(buf, new OpenContainerShulkerPacket(12, 53));
        check(OpenContainerShulkerPacket.CODEC.decode(buf).equals(new OpenContainerShulkerPacket(12, 53)), "Extended protocol round-trip");
        check(!OpenShulkerPacket.OPEN_SHULKER_PACKET.equals(OpenContainerShulkerPacket.OPEN_CONTAINER_SHULKER_PACKET), "Protocols must use separate channels");
        buf.release();

        ItemStack empty = new ItemStack(Items.CHEST);
        empty.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Keep my name"));
        ItemStackInventory chest = new ItemStackInventory(empty, 27);
        check(chest.isEmpty(), "A chest without a container component opens empty");
        chest.setStack(26, new ItemStack(Items.DIAMOND, 7));
        chest.markDirty();
        check(new ItemStackInventory(empty, 27).getStack(26).getCount() == 7, "Last chest slot persists");
        check(empty.getName().getString().equals("Keep my name"), "Unrelated components are preserved");

        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        DefaultedList<ItemStack> saved = DefaultedList.ofSize(54, ItemStack.EMPTY);
        saved.set(53, new ItemStack(Items.EMERALD, 13));
        shulker.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(saved));
        ItemStackInventory recovered = new ItemStackInventory(shulker, 27);
        check(recovered.size() == 54, "Existing high slots must not be truncated");
        recovered.setStack(0, new ItemStack(Items.STONE));
        recovered.markDirty();
        check(new ItemStackInventory(shulker, 54).getStack(53).getCount() == 13, "Editing low slots preserves slot 54");
        ContainerComponent before = shulker.get(DataComponentTypes.CONTAINER);
        recovered.setWriteAllowed(() -> false);
        recovered.setStack(53, ItemStack.EMPTY);
        recovered.markDirty();
        check(shulker.get(DataComponentTypes.CONTAINER).equals(before), "Invalid source session cannot overwrite host");
        check(PortableContainers.layout(new ItemStack(Items.DECORATED_POT)).storageSlots() == 1, "Pot has exactly one storage slot");
        check(PortableContainers.layout(new ItemStack(Items.HOPPER)).storageSlots() == 5, "Hopper has five slots");
        check(PortableContainers.layout(new ItemStack(Items.CRAFTER)).storageSlots() == 9, "Crafter has nine slots");
        check(!PortableContainers.supports(new ItemStack(Items.STONE)), "Noncontainers are rejected");
        var playerInventory = new net.minecraft.entity.player.PlayerInventory(null,new net.minecraft.entity.EntityEquipment());
        ItemStack pot = new ItemStack(Items.DECORATED_POT);
        var potMenu = new net.kyrptonaught.quickshulker.container.CompactStorageMenu(1,playerInventory,new ItemStackInventory(pot,1),1);
        check(potMenu.slots.size()==37 && potMenu.storageSlots()==1,"Pot has one real storage slot, no hopper padding");
        check(potMenu.getSlot(0).canInsert(new ItemStack(Items.DIAMOND)), "Pot accepts items");
        check(PortableContainers.layout(new ItemStack(Items.FURNACE)).type()==net.minecraft.screen.ScreenHandlerType.FURNACE,"Native furnace layout");
        check(PortableContainers.layout(new ItemStack(Items.SMOKER)).type()==net.minecraft.screen.ScreenHandlerType.SMOKER,"Native smoker layout");
        check(PortableContainers.layout(new ItemStack(Items.BLAST_FURNACE)).type()==net.minecraft.screen.ScreenHandlerType.BLAST_FURNACE,"Native blast furnace layout");
        check(PortableContainers.layout(new ItemStack(Items.BREWING_STAND)).type()==net.minecraft.screen.ScreenHandlerType.BREWING_STAND,"Native brewing slot roles");
        var config=new net.kyrptonaught.quickshulker.config.ConfigOptions();
        check(config.quickContainers && config.quickContainersCreativeOnly && config.quickLoom && config.quickBundle,"Default policy and workstations");
        for(var kind:net.kyrptonaught.quickshulker.config.ContainerKind.values()){
            check(kind.enabled(config),"Child defaults on");
            kind.set(config,false);check((net.kyrptonaught.quickshulker.config.ContainerKind.mask(config)&kind.bit())==0,"Child independently off");
            kind.set(config,true);
        }
        check(Items.BUNDLE.isEnabled(net.minecraft.resource.featuretoggle.FeatureSet.of(net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA,net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA)),"Experimental bundle item exists in 1.21.1");
        var reservation=net.kyrptonaught.quickshulker.container.PortableCrafterMenu.planInsert(java.util.List.of(new ItemStack(Items.OAK_PLANKS,63),ItemStack.EMPTY),java.util.List.of(new ItemStack(Items.OAK_PLANKS,4)),i->false);
        check(reservation.get(0).getCount()==64&&reservation.get(1).getCount()==3,"Atomic reservation merges before allocating");
        check(net.kyrptonaught.quickshulker.container.PortableCrafterMenu.planInsert(java.util.List.of(ItemStack.EMPTY),java.util.List.of(new ItemStack(Items.CAKE),new ItemStack(Items.BUCKET,3)),i->false)==null,"Reserve remainders as well as result");
        var wire=new PacketByteBuf(Unpooled.buffer());
        for(int index:new int[]{0,9,35,45,-1,Integer.MAX_VALUE}){
            net.kyrptonaught.quickshulker.upstreamwire.OpenShulkerPacket.CODEC.encode(wire,new net.kyrptonaught.quickshulker.upstreamwire.OpenShulkerPacket(index));
            check(wire.readableBytes()==4 && OpenShulkerPacket.CODEC.decode(wire).invSlot()==index && !wire.isReadable(),"Original-to-More decoding");
            wire.clear();OpenShulkerPacket.CODEC.encode(wire,new OpenShulkerPacket(index));
            check(wire.readableBytes()==4 && net.kyrptonaught.quickshulker.upstreamwire.OpenShulkerPacket.CODEC.decode(wire).invSlot()==index && !wire.isReadable(),"More-to-original decoding");wire.clear();
        }
        net.kyrptonaught.quickshulker.network.ContainerPolicyPacket.CODEC.encode(wire,new net.kyrptonaught.quickshulker.network.ContainerPolicyPacket(true,true));
        check(wire.readableBytes()==2,"Old policy still two bytes");wire.clear();
        var detailed=new net.kyrptonaught.quickshulker.network.DetailedContainerPolicyPacket(true,false,15);
        net.kyrptonaught.quickshulker.network.DetailedContainerPolicyPacket.CODEC.encode(wire,detailed);
        check(wire.readableBytes()==6 && net.kyrptonaught.quickshulker.network.DetailedContainerPolicyPacket.CODEC.decode(wire).equals(detailed),"Detailed policy independently versioned");wire.release();
        check(net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("quickshulker").orElseThrow().getMetadata().getName().equals("QuickShulkerMore"),"More brand preserved");
        check(net.minecraft.registry.Registries.SCREEN_HANDLER.getIds().stream().noneMatch(id->id.getNamespace().equals("quickshulker") && !id.getPath().equals("bundle_item")),"No added registry IDs required by original clients");
        check(!net.kyrptonaught.quickshulker.compat.ams.AMSCompat.isLargeShulkerBoxEnabled(),"Optional AMS absent path");
        var source = new net.minecraft.inventory.SimpleInventory(1);
        source.setStack(0, shulker);
        var largeMenu = new net.kyrptonaught.quickshulker.container.PortableStorageHandler(3, playerInventory, shulker,
            new PortableContainers.Layout(54, 54, net.minecraft.screen.ScreenHandlerType.GENERIC_9X6));
        var tracked = (net.kyrptonaught.quickshulker.api.ItemInventoryContainer) (Object) largeMenu;
        tracked.setSourceSlot(null, source, 0, shulker);
        check(largeMenu.canUse(null), "Mixin source tracking accepts the original shulker reference");
        source.setStack(0, shulker.copy());
        check(!largeMenu.canUse(null), "Even an identical replacement invalidates the original session");
        var outerSource=new net.minecraft.inventory.SimpleInventory(1);outerSource.setStack(0,empty);
        var outerMenu=new net.kyrptonaught.quickshulker.container.PortableStorageHandler(4,playerInventory,empty,PortableContainers.layout(empty));
        var outer=(net.kyrptonaught.quickshulker.api.ItemInventoryContainer)(Object)outerMenu;outer.setSourceSlot(null,outerSource,0,empty);
        var outerContents=(ItemStackInventory)outerMenu.getSlot(0).inventory;outerContents.bindSourceSession(outer);outerContents.setStack(0,shulker);
        tracked.setSourceSlot(null,outerContents,0,shulker);
        check(tracked.isSourceSlot(outerSource,0),"Nested source also protects its outer host");
        System.out.println("QuickShulkerMore " + net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString() + " regression checks passed: " + checks);
    }
}
