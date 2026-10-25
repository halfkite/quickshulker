package net.kyrptonaught.quickshulker.container;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.kyrptonaught.quickshulker.gui.MenuTypes;
import net.kyrptonaught.quickshulker.network.ContainerPolicyPacket;
import net.kyrptonaught.quickshulker.network.DetailedContainerPolicyPacket;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
public final class PortableContainers {
    public record Layout(int storageSlots,int menuSlots,MenuType<?> type){}
    private static DetailedContainerPolicyPacket remote;
    private static boolean detailed;
    public static ContainerKind kind(ItemStack stack){
        if(!(stack.getItem() instanceof BlockItem item))return null;
        Block block=item.getBlock();
        if(Blocks.COPPER_CHEST.asList().contains(block))return ContainerKind.COPPER_CHEST;
        if(block instanceof ShelfBlock)return ContainerKind.SHELF;
        if(block==Blocks.CHEST)return ContainerKind.CHEST;
        if(block==Blocks.TRAPPED_CHEST)return ContainerKind.TRAPPED_CHEST;
        if(block==Blocks.BARREL)return ContainerKind.BARREL;
        if(block==Blocks.HOPPER)return ContainerKind.HOPPER;
        if(block==Blocks.DROPPER)return ContainerKind.DROPPER;
        if(block==Blocks.DISPENSER)return ContainerKind.DISPENSER;
        if(block==Blocks.DECORATED_POT)return ContainerKind.POT;
        if(block==Blocks.CRAFTER)return ContainerKind.CRAFTER;
        if(block==Blocks.FURNACE)return ContainerKind.FURNACE;
        if(block==Blocks.SMOKER)return ContainerKind.SMOKER;
        if(block==Blocks.BLAST_FURNACE)return ContainerKind.BLAST_FURNACE;
        if(block==Blocks.BREWING_STAND)return ContainerKind.BREWING_STAND;
        return null;
    }
    public static boolean supports(ItemStack stack){return kind(stack)!=null;}
    public static Layout layout(ItemStack stack){
        var kind=kind(stack);return kind==null?null:new Layout(kind.slots,kind.slots,type(kind));
    }
    private static MenuType<?> type(ContainerKind kind){
        return switch(kind){
            case POT->MenuTypes.pot();case SHELF->MenuTypes.shelf();case CRAFTER->MenuTypes.crafter();
            case HOPPER->MenuType.HOPPER;case DROPPER,DISPENSER->MenuType.GENERIC_3x3;
            case FURNACE->MenuType.FURNACE;case SMOKER->MenuType.SMOKER;case BLAST_FURNACE->MenuType.BLAST_FURNACE;
            case BREWING_STAND->MenuType.BREWING_STAND;default->MenuType.GENERIC_9x3;
        };
    }
    public static void setRemotePolicy(ContainerPolicyPacket policy){
        if(policy==null){remote=null;detailed=false;MenuTypes.clearPendingMenu();}
        else if(!detailed)remote=new DetailedContainerPolicyPacket(policy.enabled(),policy.creativeOnly(),ContainerKind.allMask());
    }
    public static void setDetailedPolicy(DetailedContainerPolicyPacket policy){
        remote=policy;detailed=true;
        var config=QuickShulkerMod.getConfig();
        config.quickContainers=policy.enabled();config.quickContainersCreativeOnly=policy.creativeOnly();
        ContainerKind.applyMask(config,policy.mask());
        config.containerCreativeMask=policy.creativeMask();
    }
    public static boolean hasDetailedPolicy(){return detailed;}
    public static boolean allowed(Player player){
        var config=QuickShulkerMod.getConfig();
        boolean enabled=config.quickContainers,creative=config.quickContainersCreativeOnly;
        if(player.level().isClientSide() && remote!=null){enabled &= remote.enabled();creative |= remote.creativeOnly();}
        return !player.isSpectator()&&enabled&&(!creative||player.isCreative());
    }
    public static boolean allowed(Player player,ItemStack stack){
        var kind=kind(stack);if(kind==null||!allowed(player)||!kind.enabled(QuickShulkerMod.getConfig()))return false;
        if(kind.creativeOnly(QuickShulkerMod.getConfig())&&!player.isCreative())return false;
        return !player.level().isClientSide()||remote==null||((remote.mask()&kind.bit())!=0&&((remote.creativeMask()&kind.bit())==0||player.isCreative()));
    }
    public static boolean custom(ContainerKind kind){return kind==ContainerKind.POT||kind==ContainerKind.SHELF||kind==ContainerKind.CRAFTER;}
    public static void open(Player player,ItemStack stack){
        var kind=kind(stack);if(kind==null||!allowed(player,stack)||stack.getCount()!=1)return;
        if(stack.getOrDefault(DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.EMPTY).size()>kind.slots){
            player.sendOverlayMessage(Component.translatable("quickshulker.container.oversized"));return;
        }
        var data=stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if(data!=null && data.contains("LootTable")){player.sendOverlayMessage(Component.translatable("quickshulker.container.loot_table"));return;}
        if(custom(kind) && player instanceof ServerPlayer server && !net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.canSend(server)){
            player.sendOverlayMessage(Component.translatable("quickshulker.container.update_client"));return;
        }
        if(custom(kind) && player instanceof ServerPlayer server)
            net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.prepare(server,kind);
        player.openMenu(new SimpleMenuProvider((id,inv,opener)->create(id,inv,stack,kind),Component.translatable("quickshulker.container.storage",stack.getHoverName())));
    }
    public static AbstractContainerMenu create(int id,Inventory inv,ItemStack stack,ContainerKind kind){
        if(kind==ContainerKind.CRAFTER)return new PortableCrafterMenu(id,inv,stack);
        var contents=new ItemStackInventory(stack,kind.slots);
        return switch(kind){
            case POT,SHELF->new CompactStorageMenu(id,inv,contents,kind.slots);
            case FURNACE->new FurnaceMenu(id,inv,contents,new SimpleContainerData(AbstractFurnaceMenu.DATA_COUNT));
            case BLAST_FURNACE->new BlastFurnaceMenu(id,inv,contents,new SimpleContainerData(AbstractFurnaceMenu.DATA_COUNT));
            case SMOKER->new SmokerMenu(id,inv,contents,new SimpleContainerData(AbstractFurnaceMenu.DATA_COUNT));
            case BREWING_STAND->new BrewingStandMenu(id,inv,contents,new SimpleContainerData(net.minecraft.world.level.block.entity.BrewingStandBlockEntity.NUM_DATA_VALUES));
            default->new PortableStorageHandler(id,inv,stack,new Layout(kind.slots,kind.slots,type(kind)));
        };
    }
    public static void refresh(net.minecraft.server.MinecraftServer server){
        for(var player:server.getPlayerList().getPlayers()){
            var session=(net.kyrptonaught.quickshulker.api.ItemInventoryContainer)player.containerMenu;
            if(supports(session.getSourceStack()) && !allowed(player,session.getSourceStack()))player.closeContainer();
            ContainerPolicyPacket.send(player);
        }
    }
}
