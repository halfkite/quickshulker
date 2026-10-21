package net.kyrptonaught.quickshulker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.kyrptconfig.config.ConfigManager;
import net.kyrptonaught.quickshulker.api.*;
import net.kyrptonaught.quickshulker.compat.ModIds;
import net.kyrptonaught.quickshulker.compat.ModUtils;
import net.kyrptonaught.quickshulker.compat.reinfshulker.ReinfshulkerOpenableRegistry;
import net.kyrptonaught.quickshulker.compat.ams.AMSCompat;
import net.kyrptonaught.quickshulker.config.ConfigOptions;
import net.kyrptonaught.quickshulker.event.EventListeners;
import net.kyrptonaught.quickshulker.network.EnderChestS2CSyncPacket;
import net.kyrptonaught.quickshulker.network.OpenInventoryPacket;
import net.kyrptonaught.quickshulker.network.OpenShulkerPacket;
import net.kyrptonaught.quickshulker.network.OpenContainerShulkerPacket;
import net.kyrptonaught.quickshulker.network.QuickBundlePacket;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.kyrptonaught.shulkerutils.ShulkerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickShulkerMod implements ModInitializer, RegisterQuickShulker {

    public static final String MOD_ID = "quickshulker";
    public static ConfigManager.SingleConfigManager config = new ConfigManager.SingleConfigManager(MOD_ID, new ConfigOptions());
    public static double lastMouseX, lastMouseY;
    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        config.load();
        net.kyrptonaught.quickshulker.gui.LegacyBundleMenus.register();
        net.kyrptonaught.quickshulker.command.QuickShulkerCommands.register();
        net.kyrptonaught.quickshulker.network.ContainerPolicyPacket.register();
        net.kyrptonaught.quickshulker.network.DetailedContainerPolicyPacket.register();
        net.kyrptonaught.quickshulker.network.ContainerPolicyUpdatePacket.register();
        net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.register();
        OpenShulkerPacket.registerReceivePacket();
        OpenContainerShulkerPacket.registerReceivePacket();
        QuickBundlePacket.registerReceivePacket();
        EventListeners.registerEventListeners();

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (net.kyrptonaught.quickshulker.container.PortableContainers.supports(stack)) {
                if (!net.kyrptonaught.quickshulker.container.PortableContainers.allowed(player, stack)) return ActionResult.PASS;
                if (world.isClient() && !OpenContainerShulkerPacket.canSend()) return ActionResult.PASS;
            }
            if (QuickShulkerMod.getConfig().rightClickToOpen) {
                if (Util.isOpenableItem(stack) && Util.canOpenInHand(stack)) {
                    if(world.isClient()){
                        return ActionResult.SUCCESS;
                    }else{
                        if (hand == Hand.MAIN_HAND)
                            Util.openItem(player, 0, player.getInventory().getSelectedSlot());
                        else Util.openItem(player, 0, PlayerInventory.OFF_HAND_SLOT);
                        return ActionResult.SUCCESS_SERVER;
                    }
                }
            }
            return ActionResult.PASS;
        });

        PayloadTypeRegistry.playS2C().register(OpenInventoryPacket.OPEN_INV_ID, OpenInventoryPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(EnderChestS2CSyncPacket.S2CEChestContentPacket.S2C_ECHEST_CONTENT_PACKET_ID, EnderChestS2CSyncPacket.S2CEChestContentPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(EnderChestS2CSyncPacket.S2CEChestSlotPacket.S2C_ECHEST_SLOT_PACKET_ID, EnderChestS2CSyncPacket.S2CEChestSlotPacket.CODEC);

        FabricLoader.getInstance().getEntrypoints(MOD_ID, RegisterQuickShulker.class).forEach(RegisterQuickShulker::registerProviders);
    }

    public static ConfigOptions getConfig() {
        return (ConfigOptions) config.getConfig();
    }

    @Override
    public void registerProviders() {
        if(getConfig().quickBundle)
            new QuickOpenableRegistry.Builder().setItem(net.minecraft.item.BundleItem.class)
                .setOpenAction((player,stack)->{
                    if(stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS,net.minecraft.component.type.BundleContentsComponent.DEFAULT).size()>64)return;
                    if(stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS,net.minecraft.component.type.BundleContentsComponent.DEFAULT).stream().anyMatch(s->s.getItem() instanceof net.minecraft.item.BundleItem)){
                        player.sendMessage(Text.translatable("quickshulker.bundle.nested"),true);return;
                    }
                    if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity server)
                            || !net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.canSend(server)) {
                        player.sendMessage(Text.translatable("quickshulker.container.update_client"),true);return;
                    }
                    net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.prepareBundle(server);
                    player.openHandledScreen(new SimpleNamedScreenHandlerFactory((id,inv,opener)->
                        new net.kyrptonaught.quickshulker.container.PortableBundleMenu(id,inv,new net.kyrptonaught.quickshulker.container.PortableBundleInventory(stack)),stack.getName()));
                }).register();
        if (getConfig().quickShulkerBox)
            new QuickOpenableRegistry.Builder()
                    .setItem(ShulkerBoxBlock.class)
                    .supportsBundleing(true)
                    .getSound(((stack, isOpenSound) -> isOpenSound ? SoundEvents.BLOCK_SHULKER_BOX_OPEN : SoundEvents.BLOCK_SHULKER_BOX_CLOSE))
                    .setOpenAction(((player, stack) -> player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) ->
                            createShulkerHandler(i, playerInventory, stack), stack.getComponents().contains(DataComponentTypes.CUSTOM_NAME) ? stack.getName() : Text.translatable("container.shulkerBox")))))
                    .register();

        if (getConfig().quickEChest)
            new QuickOpenableRegistry.Builder(new QuickShulkerData.QuickEnderData())
                    .setItem(EnderChestBlock.class)
                    .supportsBundleing(true)
                    .ignoreSingleStackCheck(true)
                    .getSound((stack, isOpenSound) -> isOpenSound ? SoundEvents.BLOCK_ENDER_CHEST_OPEN : SoundEvents.BLOCK_ENDER_CHEST_CLOSE)
                    .setOpenAction(((player, stack) -> player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) ->
                            createEnderChestHandler(i, playerInventory, player.getEnderChestInventory()), Text.translatable("container.enderchest")))))
                    .register();

        if (getConfig().quickCraftingTables)
            new QuickOpenableRegistry.Builder()
                    .setItem(CraftingTableBlock.class)
                    .ignoreSingleStackCheck(true)
                    .setOpenAction(((player, stack) -> player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) ->
                            new CraftingScreenHandler(i, playerInventory, ScreenHandlerContext.create(player.getWorld(), player.getBlockPos())), Text.translatable("container.crafting")))))
                    .register();

        if (getConfig().quickStonecutter)
            new QuickOpenableRegistry.Builder()
                    .setItem(StonecutterBlock.class)
                    .ignoreSingleStackCheck(true)
                    .setOpenAction(((player, stack) -> player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) ->
                            new StonecutterScreenHandler(i, playerInventory, ScreenHandlerContext.create(player.getWorld(), player.getBlockPos())), Text.translatable("container.stonecutter")))))
                    .register();

        if (getConfig().quickLoom)
            new QuickOpenableRegistry.Builder().setItem(LoomBlock.class).ignoreSingleStackCheck(true)
                .setOpenAction((player, stack) -> player.openHandledScreen(new SimpleNamedScreenHandlerFactory((id, inv, opener) ->
                    new LoomScreenHandler(id, inv, ScreenHandlerContext.create(player.getWorld(), player.getBlockPos())), Text.translatable("container.loom")))).register();

        if(getConfig().quickAnvil)
            new QuickOpenableRegistry.Builder()
                    .setItem(AnvilBlock.class)
                    .setOpenAction((playerEntity, stack) -> playerEntity.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, player) ->
                            new AnvilScreenHandler(i, playerInventory, new ModScreenHandlerContext(playerEntity, stack)), Text.translatable("container.repair"))))
                    .register();

        if (getConfig().quickGrindstone)
            new QuickOpenableRegistry.Builder()
                    .setItem(GrindstoneBlock.class)
                    .ignoreSingleStackCheck(true)
                    .setOpenAction((player, stack) -> player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) ->
                            new GrindstoneScreenHandler(i, playerInventory, ScreenHandlerContext.create(player.getWorld(), player.getBlockPos())), Text.translatable("container.grindstone_title"))))
                    .register();

        if (getConfig().quickSmithingTable)
            new QuickOpenableRegistry.Builder()
                    .setItem(SmithingTableBlock.class)
                    .ignoreSingleStackCheck(true)
                    .setOpenAction((player, stack) -> player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) ->
                            new SmithingScreenHandler(i, playerInventory, ScreenHandlerContext.create(player.getWorld(), player.getBlockPos())), Text.translatable("container.upgrade"))))
                    .register();

        if (getConfig().quickCartographyTable)
            new QuickOpenableRegistry.Builder()
                    .setItem(CartographyTableBlock.class)
                    .ignoreSingleStackCheck(true)
                    .setOpenAction((player, stack) -> player.openHandledScreen(new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) ->
                            new CartographyTableScreenHandler(i, playerInventory, ScreenHandlerContext.create(player.getWorld(), player.getBlockPos())), Text.translatable("container.cartography_table"))))
                    .register();

        if(ModUtils.isModLoad(ModIds.reinfshulker) && QuickShulkerMod.getConfig().quickShulkerBox) {
            ReinfshulkerOpenableRegistry.registerProviders();
        }
    }

    private static int getShulkerInventorySize(ItemStack stack) {
        int desired = AMSCompat.getEffectiveShulkerInventorySize(stack, ShulkerUtils.getInventorySize(stack));
        return Math.max(desired, (int) stack.getOrDefault(DataComponentTypes.CONTAINER, net.minecraft.component.type.ContainerComponent.DEFAULT).stream().count());
    }

    private static ScreenHandler createShulkerHandler(int id, PlayerInventory playerInventory, ItemStack stack) {
        int size = getShulkerInventorySize(stack);
        if (size > 27) {
            return new net.kyrptonaught.quickshulker.container.PortableStorageHandler(id, playerInventory, stack,
                new net.kyrptonaught.quickshulker.container.PortableContainers.Layout(54, 54, ScreenHandlerType.GENERIC_9X6));
        }
        return new ShulkerBoxScreenHandler(id, playerInventory, new ItemStackInventory(stack, size));
    }

    private static GenericContainerScreenHandler createEnderChestHandler(int syncId, PlayerInventory playerInventory, net.minecraft.inventory.Inventory inventory) {
        boolean ruleEnabled = AMSCompat.isLargeEnderChestEnabled();
        if (ruleEnabled != (inventory.size() == 54) && FabricLoader.getInstance().isModLoaded("carpet-ams-addition"))
            LOGGER.debug("AMS largeEnderChest={}, actual slots={}; using actual slots to preserve contents", ruleEnabled, inventory.size());
        if (inventory.size() == 54) {
            return GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, inventory);
        }
        return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, inventory);
    }

}
