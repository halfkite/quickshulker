package net.kyrptonaught.quickshulker;

import net.kyrptonaught.quickshulker.api.*;
import net.kyrptonaught.quickshulker.network.EnderChestS2CSyncPacket;
import net.kyrptonaught.quickshulker.network.OpenInventoryPacket;
import net.kyrptonaught.quickshulker.network.OpenShulkerPacket;
import net.kyrptonaught.quickshulker.network.QuickBundlePacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.kyrptconfig.config.ConfigManager;
import net.kyrptonaught.quickshulker.api.*;
import net.kyrptonaught.quickshulker.config.ConfigOptions;
import net.kyrptonaught.quickshulker.event.EventListeners;
import net.kyrptonaught.quickshulker.gui.MenuTypes;
import net.kyrptonaught.quickshulker.gui.screen.BundleContainer;
import net.kyrptonaught.quickshulker.gui.screen.BundleItemMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QuickShulkerMod implements ModInitializer, RegisterQuickShulker {

    public static final String MOD_ID = "quickshulker";
    public static ConfigManager.SingleConfigManager config = new ConfigManager.SingleConfigManager(MOD_ID, new ConfigOptions());
    public static double lastMouseX, lastMouseY;
    public static Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        config.load();
        net.kyrptonaught.quickshulker.command.QuickShulkerCommands.register();
        net.kyrptonaught.quickshulker.network.ContainerPolicyPacket.register();
        net.kyrptonaught.quickshulker.network.DetailedContainerPolicyPacket.register();
        net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.register();
        net.kyrptonaught.quickshulker.network.ContainerPolicyUpdatePacket.register();
        net.kyrptonaught.quickshulker.network.OpenContainerShulkerPacket.registerReceivePacket();
        OpenShulkerPacket.registerReceivePacket();
        QuickBundlePacket.registerReceivePacket();
        EventListeners.registerEventListeners();
        MenuTypes.registerMenuTypes();

        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (net.kyrptonaught.quickshulker.container.PortableContainers.supports(stack)) {
                if (!net.kyrptonaught.quickshulker.container.PortableContainers.allowed(player, stack)) return InteractionResult.PASS;
                if (level.isClientSide() && !net.kyrptonaught.quickshulker.network.OpenContainerShulkerPacket.canSend()) return InteractionResult.PASS;
            }
            if (QuickShulkerMod.getConfig().rightClickToOpen) {
                if (Util.isOpenableItem(stack) && Util.canOpenInHand(stack)) {
                    if(level.isClientSide()){
                        return InteractionResult.SUCCESS;
                    }else{
                        if (hand == InteractionHand.MAIN_HAND)
                            Util.openItem(player, 0, player.getInventory().getSelectedSlot());
                        else Util.openItem(player, 0, Inventory.SLOT_OFFHAND);
                        return InteractionResult.SUCCESS_SERVER;
                    }
                }
            }
            return InteractionResult.PASS;
        });

        PayloadTypeRegistry.clientboundPlay().register(OpenInventoryPacket.OPEN_INV_ID, OpenInventoryPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EnderChestS2CSyncPacket.S2CEChestContentPacket.S2C_ECHEST_CONTENT_PACKET_ID, EnderChestS2CSyncPacket.S2CEChestContentPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EnderChestS2CSyncPacket.S2CEChestSlotPacket.S2C_ECHEST_SLOT_PACKET_ID, EnderChestS2CSyncPacket.S2CEChestSlotPacket.CODEC);

        FabricLoader.getInstance().getEntrypoints(MOD_ID, RegisterQuickShulker.class).forEach(RegisterQuickShulker::registerProviders);
    }

    public static ConfigOptions getConfig() {
        return (ConfigOptions) config.getConfig();
    }

    @Override
    public void registerProviders() {
        if (getConfig().quickShulkerBox)
            new QuickOpenableRegistry.Builder()
                    .setItem(ShulkerBoxBlock.class)
                    .supportsBundleing(true)
                    .getSound(((stack, isOpenSound) -> isOpenSound ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE))
                    .setOpenAction(((player, stack) -> player.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) ->
                            createShulkerMenu(i, playerInventory, stack), stack.getComponents().has(DataComponents.CUSTOM_NAME) ? stack.getHoverName() : Component.translatable("container.shulkerBox")))))
                    .register();

        if (getConfig().quickEChest)
            new QuickOpenableRegistry.Builder(new QuickShulkerData.QuickEnderData())
                    .setItem(EnderChestBlock.class)
                    .supportsBundleing(true)
                    .ignoreSingleStackCheck(true)
                    .getSound((stack, isOpenSound) -> isOpenSound ? SoundEvents.ENDER_CHEST_OPEN : SoundEvents.ENDER_CHEST_CLOSE)
                    .setOpenAction(((player, stack) -> player.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) ->
                            createEnderMenu(i, playerInventory, player.getEnderChestInventory()), Component.translatable("container.enderchest")))))
                    .register();

        if (getConfig().quickCraftingTables)
            new QuickOpenableRegistry.Builder()
                    .setItem(CraftingTableBlock.class)
                    .ignoreSingleStackCheck(true)
                    .setOpenAction(((player, stack) -> player.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) ->
                            new CraftingMenu(i, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())), Component.translatable("container.crafting")))))
                    .register();

        if (getConfig().quickStonecutter)
            new QuickOpenableRegistry.Builder()
                    .setItem(StonecutterBlock.class)
                    .ignoreSingleStackCheck(true)
                    .setOpenAction(((player, stack) -> player.openMenu(new SimpleMenuProvider((i, playerInventory, playerEntity) ->
                            new StonecutterMenu(i, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())), Component.translatable("container.stonecutter")))))
                    .register();

        if (getConfig().quickGrindstone)
            new QuickOpenableRegistry.Builder().setItem(GrindstoneBlock.class)
                .ignoreSingleStackCheck(true)
                .setOpenAction((player, stack) -> player.openMenu(new SimpleMenuProvider((id, inv, opener) ->
                    new GrindstoneMenu(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
                    Component.translatable("container.grindstone_title")))).register();

        if (getConfig().quickSmithingTable)
            new QuickOpenableRegistry.Builder().setItem(SmithingTableBlock.class)
                .ignoreSingleStackCheck(true)
                .setOpenAction((player, stack) -> player.openMenu(new SimpleMenuProvider((id, inv, opener) ->
                    new SmithingMenu(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
                    Component.translatable("container.upgrade")))).register();

        if (getConfig().quickCartographyTable)
            new QuickOpenableRegistry.Builder().setItem(CartographyTableBlock.class)
                .ignoreSingleStackCheck(true)
                .setOpenAction((player, stack) -> player.openMenu(new SimpleMenuProvider((id, inv, opener) ->
                    new CartographyTableMenu(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
                    Component.translatable("container.cartography_table")))).register();

        if(getConfig().quickBundle)
            new QuickOpenableRegistry.Builder()
                    .setItem(BundleItem.class)
                    .setOpenAction((playerEntity, stack) -> {
                        if (!(playerEntity instanceof net.minecraft.server.level.ServerPlayer server)) return;
                        if (!net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.canSend(server)) {
                            playerEntity.sendOverlayMessage(Component.translatable("quickshulker.container.update_client"));
                            return;
                        }
                        net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.prepareBundle(server);
                        playerEntity.openMenu(new SimpleMenuProvider((i, playerInventory, player) ->
                            new BundleItemMenu(i, playerInventory, new BundleContainer(stack, 64)), stack.getComponents().has(DataComponents.CUSTOM_NAME) ? stack.getHoverName() : Component.translatable("item.minecraft.bundle")));
                    })
                    .register();

        if (getConfig().quickLoom)
            new QuickOpenableRegistry.Builder().setItem(LoomBlock.class).ignoreSingleStackCheck(true)
                .setOpenAction((player, stack) -> player.openMenu(new SimpleMenuProvider((id, inv, opener) ->
                    new LoomMenu(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
                    Component.translatable("container.loom")))).register();

        if(getConfig().quickAnvil)
            new QuickOpenableRegistry.Builder()
                    .setItem(AnvilBlock.class)
                    .setOpenAction((playerEntity, stack) -> playerEntity.openMenu(new SimpleMenuProvider((i, playerInventory, player) ->
                            new AnvilMenu(i, playerInventory, new ModContainerLevelAccess(playerEntity, stack)), Component.translatable("container.repair"))))
                    .register();
    }


    private static AbstractContainerMenu createShulkerMenu(int id, Inventory inventory, ItemStack stack) {
        int size = Math.max(net.kyrptonaught.quickshulker.compat.ams.AMSCompat.getEffectiveShulkerInventorySize(stack, 27),
            Math.toIntExact(stack.getOrDefault(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY).allItemsCopyStream().count()));
        if (size > 27) return new net.kyrptonaught.quickshulker.container.PortableStorageHandler(id, inventory, stack,
            new net.kyrptonaught.quickshulker.container.PortableContainers.Layout(54, 54, MenuType.GENERIC_9x6));
        return new ShulkerBoxMenu(id, inventory, new ItemStackInventory(stack, 27));
    }
    private static ChestMenu createEnderMenu(int id, Inventory inventory, net.minecraft.world.Container contents) {
        boolean enabled = net.kyrptonaught.quickshulker.compat.ams.AMSCompat.isLargeEnderChestEnabled();
        if (enabled != (contents.getContainerSize() == 54))
            LOGGER.debug("largeEnderChest={}, actual slots={}; using actual capacity", enabled, contents.getContainerSize());
        return contents.getContainerSize() == 54 ? ChestMenu.sixRows(id, inventory, contents) : ChestMenu.threeRows(id, inventory, contents);
    }
}
