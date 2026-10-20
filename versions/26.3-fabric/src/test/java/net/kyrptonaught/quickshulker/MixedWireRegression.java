package net.kyrptonaught.quickshulker;

import io.netty.buffer.Unpooled;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.quickshulker.network.OpenShulkerPacket;
import net.kyrptonaught.quickshulker.util.update.MoreReleaseTags;
import net.minecraft.network.FriendlyByteBuf;

/** Independent upstream and More codecs. This is wire testing, not a player-login simulation. */
public final class MixedWireRegression {
    private static int checks;
    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }
    public static int run() {
        checks = 0;
        portableMenuPacketOrdering();
        var metadata = FabricLoader.getInstance().getModContainer("quickshulker").orElseThrow().getMetadata();
        check(metadata.getName().equals("QuickShulkerMore"), "Public name is More");
        String gameVersion = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow()
            .getMetadata().getVersion().getFriendlyString();
        check(metadata.getVersion().getFriendlyString().equals("1.0.0-" + gameVersion), "More version matches the actual game");
        check(QuickShulkerMod.MOD_ID.equals("quickshulker"), "Legacy internal ID preserved");
        check(MoreReleaseTags.version("3.3.0-26.3", "fabric", "26.3").isEmpty(), "Historical 3.x tags cannot prompt an upgrade");
        check(MoreReleaseTags.version("quickshulkermore-fabric-1.0.1-26.3", "fabric", "26.3").orElseThrow().equals("1.0.1"), "More release tag accepted");
        check(MoreReleaseTags.version("quickshulkermore-neoforge-1.0.1-26.3", "fabric", "26.3").isEmpty(), "Other loader ignored");
        check(MoreReleaseTags.version("quickshulkermore-fabric-1.0.1-26.2", "fabric", "26.3").isEmpty(), "Other game version ignored");
        check(MoreReleaseTags.version("quickshulkermore-fabric-999999999999.0.0-26.3", "fabric", "26.3").isEmpty(), "Unbounded numeric tags rejected");
        check(MoreReleaseTags.version(null, "fabric", "26.3").isEmpty(), "Missing tag rejected");
        check(net.kyrptonaught.quickshulker.upstreamwire.OpenShulkerPacket.OPEN_SHULKER_PACKET.equals(OpenShulkerPacket.OPEN_SHULKER_PACKET), "Original channel unchanged");
        var originalCodec = net.kyrptonaught.quickshulker.upstreamwire.OpenShulkerPacket.CODEC;
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (int index : new int[]{0,9,35,36,45,-1,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
                originalCodec.encode(buffer, new net.kyrptonaught.quickshulker.upstreamwire.OpenShulkerPacket(index));
                check(buffer.readableBytes() == 4, "Original client writes exactly four bytes");
                check(OpenShulkerPacket.CODEC.decode(buffer).invSlot() == index && !buffer.isReadable(), "More server decodes original request completely");
                buffer.clear();
                OpenShulkerPacket.CODEC.encode(buffer, new OpenShulkerPacket(index));
                check(buffer.readableBytes() == 4, "More client preserves original packet length");
                check(originalCodec.decode(buffer).invSlot() == index && !buffer.isReadable(), "Original server decodes More request completely");
                buffer.clear();
            }
        } finally { buffer.release(); }
        var registered = net.minecraft.core.registries.BuiltInRegistries.MENU.keySet().stream()
            .filter(id -> id.getNamespace().equals("quickshulker")).map(Object::toString).collect(java.util.stream.Collectors.toSet());
        check(registered.equals(java.util.Set.of("quickshulker:bundle_item")), "Menu registry has exactly the upstream entry, no unknown More IDs");
        var inventory = new net.minecraft.world.entity.player.Inventory(null, new net.minecraft.world.entity.EntityEquipment());
        net.kyrptonaught.quickshulker.gui.MenuTypes.preparePortableMenu(1);
        var pot = net.kyrptonaught.quickshulker.gui.MenuTypes.createClientMenu(7, inventory);
        check(pot instanceof net.kyrptonaught.quickshulker.container.CompactStorageMenu && pot.slots.size() == 37, "Negotiated pot has one real slot");
        net.kyrptonaught.quickshulker.gui.MenuTypes.preparePortableMenu(2);
        var shelf = net.kyrptonaught.quickshulker.gui.MenuTypes.createClientMenu(8, inventory);
        check(shelf instanceof net.kyrptonaught.quickshulker.container.CompactStorageMenu && shelf.slots.size() == 39, "Negotiated shelf has three real slots");
        check(!net.kyrptonaught.quickshulker.compat.ams.AMSCompat.isLargeShulkerBoxEnabled(), "AMS-absent path does not resolve optional API classes");
        check(!net.kyrptonaught.quickshulker.compat.ams.AMSCompat.isLargeEnderChestEnabled(), "AMS-absent ender path is safe");
        return checks;
    }

    private static void portableMenuPacketOrdering() {
        var executor = new net.minecraft.util.thread.ReentrantBlockableEventLoop<Runnable>("portable-menu-ordering", true) {
            private final Thread owner = Thread.currentThread();
            @Override public Runnable wrapRunnable(Runnable task) { return task; }
            @Override protected boolean shouldRun(Runnable task) { return true; }
            @Override protected Thread getRunningThread() { return owner; }
            public void drain() { while (pollTask()) { } }
        };
        var inventory = new net.minecraft.world.entity.player.Inventory(null, new net.minecraft.world.entity.EntityEquipment());
        var observed = new java.util.ArrayList<net.minecraft.world.inventory.AbstractContainerMenu>();
        net.kyrptonaught.quickshulker.gui.MenuTypes.clearPendingMenu();
        executor.schedule(() -> executor.execute(() -> net.kyrptonaught.quickshulker.gui.MenuTypes.preparePortableMenu(1)));
        executor.schedule(() -> observed.add(net.kyrptonaught.quickshulker.gui.MenuTypes.createClientMenu(7, inventory)));
        executor.drain();
        check(!(observed.getFirst() instanceof net.kyrptonaught.quickshulker.container.CompactStorageMenu),
            "Old nested scheduling reproduces first-open fallback");
        check(net.kyrptonaught.quickshulker.gui.MenuTypes.createClientMenu(8, inventory) instanceof net.kyrptonaught.quickshulker.container.CompactStorageMenu,
            "Old callback leaves a stale pot envelope for the following menu");
        for (int round = 0; round < 3; round++) {
            for (int kind : new int[]{1, 2, 3}) {
                executor.schedule(() -> net.kyrptonaught.quickshulker.gui.MenuTypes.preparePortableMenu(kind));
                executor.schedule(() -> {
                    var menu = net.kyrptonaught.quickshulker.gui.MenuTypes.createClientMenu(7, inventory);
                    check(menu.slots.size() == switch (kind) { case 1 -> 37; case 2 -> 39; default -> 46; },
                        "Envelope precedes the correct pot/shelf/crafter slot layout");
                    if (kind == 3) {
                        menu.setData(0, 1);
                        check(((net.kyrptonaught.quickshulker.container.PortableCrafterMenu)menu).isSlotDisabled(0),
                            "Crafter properties apply to the intended menu");
                    }
                });
            }
            executor.drain();
            check(!(net.kyrptonaught.quickshulker.gui.MenuTypes.createClientMenu(8, inventory) instanceof net.kyrptonaught.quickshulker.container.CompactStorageMenu),
                "No stale envelope survives container transitions");
        }
    }
}
