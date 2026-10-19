package net.kyrptonaught.quickshulker;

import io.netty.buffer.Unpooled;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.kyrptonaught.quickshulker.network.OpenShulkerPacket;
import net.kyrptonaught.quickshulker.network.OpenContainerShulkerPacket;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;

/** Real component and wire-format regression checks, independent of a running world. */
public final class ContainerRegressionTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Fabric does not promise entrypoint invocation order between separate mods.
        if (QuickShulker.getConfig() == null) QuickShulker.config.load();
        new QuickShulker.ProvidersRegister().registerProviders();
        if (!Items.CHEST.builtInRegistryHolder().areComponentsBound()) {
            var lookup = net.minecraft.data.registries.VanillaRegistries.createWorldLookup();
            net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup)
                .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        OpenShulkerPacket.CODEC.encode(buf, new OpenShulkerPacket(45));
        check(buf.readableBytes() == Integer.BYTES, "Legacy protocol must contain exactly one int");
        check(OpenShulkerPacket.CODEC.decode(buf).invSlot() == 45 && !buf.isReadable(), "Original server consumes the complete packet");
        buf.clear();
        OpenContainerShulkerPacket.CODEC.encode(buf, new OpenContainerShulkerPacket(12, 53));
        check(OpenContainerShulkerPacket.CODEC.decode(buf).equals(new OpenContainerShulkerPacket(12, 53)), "Extended protocol round-trip");
        check(!OpenShulkerPacket.OPEN_SHULKER_PACKET.equals(OpenContainerShulkerPacket.OPEN_CONTAINER_SHULKER_PACKET), "Protocols must use separate channels");
        buf.release();

        ItemStack empty = new ItemStack(Items.CHEST);
        empty.set(DataComponents.CUSTOM_NAME, Component.literal("Keep my name"));
        ItemStackInventory chest = new ItemStackInventory(empty, 27);
        check(chest.isEmpty(), "A chest without a container component opens empty");
        chest.setItem(26, new ItemStack(Items.DIAMOND, 7));
        chest.setChanged();
        check(new ItemStackInventory(empty, 27).getItem(26).getCount() == 7, "Last chest slot persists");
        check(empty.getHoverName().getString().equals("Keep my name"), "Unrelated components are preserved");

        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        NonNullList<ItemStack> saved = NonNullList.withSize(54, ItemStack.EMPTY);
        saved.set(53, new ItemStack(Items.EMERALD, 13));
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(saved));
        ItemStackInventory recovered = new ItemStackInventory(shulker, 27);
        check(recovered.getContainerSize() == 54, "Existing high slots must not be truncated");
        recovered.setItem(0, new ItemStack(Items.STONE));
        recovered.setChanged();
        check(new ItemStackInventory(shulker, 54).getItem(53).getCount() == 13, "Editing low slots preserves slot 54");
        ItemContainerContents before = shulker.get(DataComponents.CONTAINER);
        recovered.setWriteAllowed(() -> false);
        recovered.setItem(53, ItemStack.EMPTY);
        recovered.setChanged();
        check(shulker.get(DataComponents.CONTAINER).equals(before), "Invalid source session cannot overwrite host");
        check(PortableContainers.layout(new ItemStack(Items.DECORATED_POT)).storageSlots() == 1, "Pot has exactly one storage slot");
        check(PortableContainers.layout(new ItemStack(Items.HOPPER)).storageSlots() == 5, "Hopper has five slots");
        check(PortableContainers.layout(new ItemStack(Items.CRAFTER)).storageSlots() == 9, "Crafter has nine slots");
        check(!PortableContainers.supports(new ItemStack(Items.STONE)), "Noncontainers are rejected");
        var playerInventory = new net.minecraft.world.entity.player.Inventory(null, new net.minecraft.world.entity.EntityEquipment());
        ItemStack pot = new ItemStack(Items.DECORATED_POT);
        var potMenu = new net.kyrptonaught.quickshulker.container.PortableStorageHandler(1, playerInventory, pot, PortableContainers.layout(pot));
        check(potMenu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Pot storage accepts items");
        check(!potMenu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)), "Padding slots reject insertion");
        check(!potMenu.getSlot(1).mayPickup(null), "Padding slots reject extraction");
        ItemStack crafter = new ItemStack(Items.CRAFTER);
        var nbt = new net.minecraft.nbt.CompoundTag();
        nbt.putIntArray("disabled_slots", new int[]{4});
        crafter.set(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.TypedEntityData.of(net.minecraft.world.level.block.entity.BlockEntityTypes.CRAFTER, nbt));
        var crafterMenu = new net.kyrptonaught.quickshulker.container.PortableStorageHandler(2, playerInventory, crafter, PortableContainers.layout(crafter));
        check(!crafterMenu.getSlot(4).mayPlace(new ItemStack(Items.STONE)), "Crafter disabled slots remain disabled");
        check(crafterMenu.getSlot(3).mayPlace(new ItemStack(Items.STONE)), "Other crafter slots remain usable");
        check(crafter.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId().getIntArray("disabled_slots").orElse(new int[0])[0] == 4, "Crafter metadata preserved");
        var source = new net.minecraft.world.SimpleContainer(1);
        source.setItem(0, shulker);
        var largeMenu = new net.kyrptonaught.quickshulker.container.PortableStorageHandler(3, playerInventory, shulker,
            new PortableContainers.Layout(54, 54, net.minecraft.world.inventory.MenuType.GENERIC_9x6));
        var tracked = (net.kyrptonaught.quickshulker.api.ItemInventoryContainer) (Object) largeMenu;
        tracked.setSourceSlot(null, source, 0, shulker);
        check(largeMenu.stillValid(null), "Mixin source tracking accepts the original shulker reference");
        source.setItem(0, shulker.copy());
        check(!largeMenu.stillValid(null), "Even an identical replacement invalidates the original session");
        var outerSource = new net.minecraft.world.SimpleContainer(2);
        outerSource.setItem(0, empty);
        var outerMenu = new net.kyrptonaught.quickshulker.container.PortableStorageHandler(4, playerInventory, empty, PortableContainers.layout(empty));
        var outerTracked = (net.kyrptonaught.quickshulker.api.ItemInventoryContainer) (Object) outerMenu;
        outerTracked.setSourceSlot(null, outerSource, 0, empty);
        var outerContents = (ItemStackInventory) outerMenu.getSlot(0).container;
        outerContents.bindSourceSession(outerTracked);
        outerContents.setItem(0, shulker);
        tracked.setSourceSlot(null, outerContents, 0, shulker);
        check(tracked.isSourceSlot(outerSource, 0), "A nested menu also locks the outer host slot against shift moves");
        check(!tracked.isSourceSlot(outerSource, 1), "Ancestor protection does not lock unrelated slots");
        System.out.println("QuickShulker regression checks passed: " + checks);
    }
}
