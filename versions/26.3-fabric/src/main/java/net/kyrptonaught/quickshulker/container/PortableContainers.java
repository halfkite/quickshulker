package net.kyrptonaught.quickshulker.container;

import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import java.util.Map;

/** Explicit storage adapters. They never place a block or execute world ticks. */
public final class PortableContainers {
    public record Layout(int storageSlots, int menuSlots, MenuType<?> type) {}
    private static final Layout CHEST = new Layout(27, 27, MenuType.GENERIC_9x3);
    private static final Layout NINE = new Layout(9, 9, MenuType.GENERIC_3x3);
    private static final Layout FIVE = new Layout(5, 5, MenuType.HOPPER);
    private static final Layout THREE = new Layout(3, 5, MenuType.HOPPER);
    private static final Layout POT = new Layout(1, 5, MenuType.HOPPER);
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
    public static boolean allowed(Player player) {
        boolean enabled = player.level().isClientSide() && remotePolicy != null ? remotePolicy.enabled() : QuickShulkerMod.getConfig().quickContainers;
        boolean creativeOnly = player.level().isClientSide() && remotePolicy != null ? remotePolicy.creativeOnly() : QuickShulkerMod.getConfig().quickContainersCreativeOnly;
        return !player.isSpectator() && enabled && (!creativeOnly || player.isCreative());
    }

    public static void open(Player player, ItemStack stack) {
        Layout layout = layout(stack);
        if (layout == null || !allowed(player) || stack.getCount() != 1) return;
        var blockData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockData != null && blockData.copyTagWithoutId().contains("LootTable")) {
            player.sendOverlayMessage(Component.translatable("quickshulker.container.loot_table"));
            return;
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, opener) ->
            new PortableStorageHandler(id, inventory, stack, layout),
            Component.translatable("quickshulker.container.storage", stack.getHoverName())));
    }
}
