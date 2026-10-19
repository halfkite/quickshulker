package net.kyrptonaught.quickshulker.container;

import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import java.util.Map;

/** Explicit storage adapters. They never place a block or execute world ticks. */
public final class PortableContainers {
    public record Layout(int storageSlots, int menuSlots, ScreenHandlerType<?> type) {}
    private static final Layout CHEST = new Layout(27, 27, ScreenHandlerType.GENERIC_9X3);
    private static final Layout NINE = new Layout(9, 9, ScreenHandlerType.GENERIC_3X3);
    private static final Layout FIVE = new Layout(5, 5, ScreenHandlerType.HOPPER);
    private static final Layout THREE = new Layout(3, 5, ScreenHandlerType.HOPPER);
    private static final Layout POT = new Layout(1, 5, ScreenHandlerType.HOPPER);
    private static final Map<Item, Layout> LAYOUTS = Map.ofEntries(
        Map.entry(Blocks.CHEST.asItem(), CHEST), Map.entry(Blocks.TRAPPED_CHEST.asItem(), CHEST),
        Map.entry(Blocks.BARREL.asItem(), CHEST), Map.entry(Blocks.HOPPER.asItem(), FIVE),
        Map.entry(Blocks.DROPPER.asItem(), NINE), Map.entry(Blocks.DISPENSER.asItem(), NINE),
        Map.entry(Blocks.CRAFTER.asItem(), NINE), Map.entry(Blocks.DECORATED_POT.asItem(), POT),
        Map.entry(Blocks.FURNACE.asItem(), THREE), Map.entry(Blocks.SMOKER.asItem(), THREE),
        Map.entry(Blocks.BLAST_FURNACE.asItem(), THREE), Map.entry(Blocks.BREWING_STAND.asItem(), FIVE));

    private PortableContainers() {}
    private static net.kyrptonaught.quickshulker.network.ContainerPolicyPacket remotePolicy;
    public static void setRemotePolicy(net.kyrptonaught.quickshulker.network.ContainerPolicyPacket policy) {
        remotePolicy = policy;
    }

    public static boolean supports(ItemStack stack) { return LAYOUTS.containsKey(stack.getItem()); }
    public static Layout layout(ItemStack stack) { return LAYOUTS.get(stack.getItem()); }
    public static boolean allowed(PlayerEntity player) {
        boolean enabled = player.getWorld().isClient && remotePolicy != null ? remotePolicy.enabled() : QuickShulkerMod.getConfig().quickContainers;
        boolean creativeOnly = player.getWorld().isClient && remotePolicy != null ? remotePolicy.creativeOnly() : QuickShulkerMod.getConfig().quickContainersCreativeOnly;
        return !player.isSpectator() && enabled && (!creativeOnly || player.isCreative());
    }

    public static void open(PlayerEntity player, ItemStack stack) {
        Layout layout = layout(stack);
        if (layout == null || !allowed(player) || stack.getCount() != 1) return;
        NbtComponent blockData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        if (blockData != null && blockData.copyNbt().contains("LootTable")) {
            player.sendMessage(Text.translatable("quickshulker.container.loot_table"), true);
            return;
        }
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((id, inventory, opener) ->
            new PortableStorageHandler(id, inventory, stack, layout),
            Text.translatable("quickshulker.container.storage", stack.getName())));
    }
}
