package net.kyrptonaught.quickshulker;

import net.kyrptonaught.kyrptconfig.config.ConfigManager;
import net.kyrptonaught.quickshulker.api.*;
import net.kyrptonaught.quickshulker.api.*;
import net.kyrptonaught.quickshulker.config.ConfigOptions;
import net.kyrptonaught.quickshulker.gui.MenuTypes;
import net.kyrptonaught.quickshulker.gui.screen.BundleContainer;
import net.kyrptonaught.quickshulker.gui.screen.BundleItemMenu;
import net.kyrptonaught.quickshulker.network.EnderChestS2CSyncPacket;
import net.kyrptonaught.quickshulker.network.OpenInventoryPacket;
import net.kyrptonaught.quickshulker.network.OpenShulkerPacket;
import net.kyrptonaught.quickshulker.network.QuickBundlePacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.apache.logging.log4j.LogManager;

import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.Logger;

import java.util.ServiceLoader;

@Mod(QuickShulker.MOD_ID)
public class QuickShulker {

    public static final String MOD_ID = "quickshulker";
    public static ConfigManager.SingleConfigManager config = new ConfigManager.SingleConfigManager(MOD_ID, new ConfigOptions());
    public static double lastMouseX, lastMouseY;
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public QuickShulker(IEventBus modEventBus, ModContainer modContainer) {
        config.load();
        modEventBus.register(OpenShulkerPacket.class);
        modEventBus.register(net.kyrptonaught.quickshulker.network.OpenContainerShulkerPacket.class);
        modEventBus.addListener(net.kyrptonaught.quickshulker.network.ContainerPolicyPacket::register);
        modEventBus.addListener(net.kyrptonaught.quickshulker.network.DetailedContainerPolicyPacket::register);
        modEventBus.addListener(net.kyrptonaught.quickshulker.network.ContainerPolicyUpdatePacket::register);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) -> net.kyrptonaught.quickshulker.command.QuickShulkerCommands.register(event));
        modEventBus.register(QuickBundlePacket.class);
        MenuTypes.MENU_TYPES.register(modEventBus);

        modEventBus.addListener(this::registerReceivePacket);
        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        ServiceLoader.load(RegisterQuickShulker.class).stream().map(ServiceLoader.Provider::get).forEach(RegisterQuickShulker::registerProviders);
    }

    @SubscribeEvent
    public void onPlayerUseItem(PlayerInteractEvent.RightClickItem event){
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (net.kyrptonaught.quickshulker.container.PortableContainers.supports(stack)) {
            if (!net.kyrptonaught.quickshulker.container.PortableContainers.allowed(player, stack)) return;
            if (event.getLevel().isClientSide() && !net.kyrptonaught.quickshulker.network.OpenContainerShulkerPacket.canSend()) return;
        }
        if(getConfig().rightClickToOpen){
            if(Util.isOpenableItem(stack) && Util.canOpenInHand(stack)){
                if(event.getLevel().isClientSide()){
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }else{
                    if(event.getHand() == InteractionHand.MAIN_HAND)
                        Util.openItem(player, 0, player.getInventory().getSelectedSlot());
                    else Util.openItem(player, 0, Inventory.SLOT_OFFHAND);
                    event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
                }
                event.setCanceled(true);
                return;
            }
        }
        event.setCancellationResult(InteractionResult.PASS);
    }

    public void registerReceivePacket(RegisterPayloadHandlersEvent event){
        event.registrar("1").playToClient(OpenInventoryPacket.OPEN_INV_ID, OpenInventoryPacket.CODEC);
        event.registrar("1").playToClient(EnderChestS2CSyncPacket.S2CEChestContentPacket.S2C_ECHEST_CONTENT_PACKET_ID, EnderChestS2CSyncPacket.S2CEChestContentPacket.CODEC);
        event.registrar("1").playToClient(EnderChestS2CSyncPacket.S2CEChestSlotPacket.S2C_ECHEST_SLOT_PACKET_ID, EnderChestS2CSyncPacket.S2CEChestSlotPacket.CODEC);
    }

    public static ConfigOptions getConfig() {
        return (ConfigOptions) config.getConfig();
    }

    public static class ProvidersRegister implements RegisterQuickShulker{
        @Override
        public void registerProviders() {
            if (getConfig().quickShulkerBox)
                new QuickOpenableRegistry.Builder()
                        .setItem(ShulkerBoxBlock.class)
                        .supportsBundleing(true)
                        .getSound((stack, isOpenSound) -> isOpenSound ? SoundEvents.SHULKER_BOX_OPEN : SoundEvents.SHULKER_BOX_CLOSE)
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

        if (getConfig().quickLoom)
            new QuickOpenableRegistry.Builder().setItem(LoomBlock.class).ignoreSingleStackCheck(true)
                .setOpenAction((player, stack) -> player.openMenu(new SimpleMenuProvider((id, inv, opener) ->
                    new LoomMenu(id, inv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
                    Component.translatable("container.loom")))).register();

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
                        .setOpenAction((playerEntity, stack) -> playerEntity.openMenu(new SimpleMenuProvider((i, playerInventory, player) ->
                                new BundleItemMenu(i, playerInventory, new BundleContainer(stack, 64)), stack.getComponents().has(DataComponents.CUSTOM_NAME) ? stack.getHoverName() : Component.translatable("item.minecraft.bundle"))))
                        .register();

            if(getConfig().quickAnvil)
                new QuickOpenableRegistry.Builder()
                        .setItem(AnvilBlock.class)
                        .setOpenAction((playerEntity, stack) -> playerEntity.openMenu(new SimpleMenuProvider((i, playerInventory, player) ->
                                new AnvilMenu(i, playerInventory, new ModContainerLevelAccess(playerEntity, stack)), Component.translatable("container.repair"))))
                        .register();
        }
    }
    private static AbstractContainerMenu createShulkerMenu(int id, Inventory inventory, ItemStack stack) {
        int size = Math.max(net.kyrptonaught.quickshulker.compat.ams.AMSCompat.getEffectiveShulkerInventorySize(stack, 27),
            stack.getOrDefault(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY).size());
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
