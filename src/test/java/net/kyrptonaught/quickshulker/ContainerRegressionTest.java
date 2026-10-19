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
    public static void main(String[] args) {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        // Fabric does not promise entrypoint invocation order between separate mods.
        if (QuickShulkerMod.getConfig() == null) new QuickShulkerMod().onInitialize();
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
        var playerInventory = new net.minecraft.entity.player.PlayerInventory(null);
        ItemStack pot = new ItemStack(Items.DECORATED_POT);
        var potMenu = new net.kyrptonaught.quickshulker.container.PortableStorageHandler(1, playerInventory, pot, PortableContainers.layout(pot));
        check(potMenu.getSlot(0).canInsert(new ItemStack(Items.DIAMOND)), "Pot storage accepts items");
        check(!potMenu.getSlot(1).canInsert(new ItemStack(Items.DIAMOND)), "Padding slots reject insertion");
        check(!potMenu.getSlot(1).canTakeItems(null), "Padding slots reject extraction");
        ItemStack crafter = new ItemStack(Items.CRAFTER);
        var nbt = new net.minecraft.nbt.NbtCompound();
        nbt.putIntArray("disabled_slots", new int[]{4});
        crafter.set(DataComponentTypes.BLOCK_ENTITY_DATA, net.minecraft.component.type.NbtComponent.of(nbt));
        var crafterMenu = new net.kyrptonaught.quickshulker.container.PortableStorageHandler(2, playerInventory, crafter, PortableContainers.layout(crafter));
        check(!crafterMenu.getSlot(4).canInsert(new ItemStack(Items.STONE)), "Crafter disabled slots remain disabled");
        check(crafterMenu.getSlot(3).canInsert(new ItemStack(Items.STONE)), "Other crafter slots remain usable");
        check(crafter.get(DataComponentTypes.BLOCK_ENTITY_DATA).copyNbt().getIntArray("disabled_slots")[0] == 4, "Crafter metadata preserved");
        var source = new net.minecraft.inventory.SimpleInventory(1);
        source.setStack(0, shulker);
        var largeMenu = new net.kyrptonaught.quickshulker.container.PortableStorageHandler(3, playerInventory, shulker,
            new PortableContainers.Layout(54, 54, net.minecraft.screen.ScreenHandlerType.GENERIC_9X6));
        var tracked = (net.kyrptonaught.quickshulker.api.ItemInventoryContainer) (Object) largeMenu;
        tracked.setSourceSlot(null, source, 0, shulker);
        check(largeMenu.canUse(null), "Mixin source tracking accepts the original shulker reference");
        source.setStack(0, shulker.copy());
        check(!largeMenu.canUse(null), "Even an identical replacement invalidates the original session");
        System.out.println("QuickShulker regression checks passed: " + checks);
    }
}
