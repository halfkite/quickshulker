package net.kyrptonaught.quickshulker.container;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.config.ContainerKind;

import net.kyrptonaught.quickshulker.network.ContainerPolicyPacket;
import net.kyrptonaught.quickshulker.network.DetailedContainerPolicyPacket;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.block.*;
public final class PortableContainers {
    public record Layout(int storageSlots,int menuSlots,ScreenHandlerType<?> type){}
    private static DetailedContainerPolicyPacket remote;
    private static boolean detailed;
    public static ContainerKind kind(ItemStack stack){
        if(!(stack.getItem() instanceof BlockItem item))return null;
        Block block=item.getBlock();
        if(block instanceof CopperChestBlock)return ContainerKind.COPPER_CHEST;
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
    private static ScreenHandlerType<?> type(ContainerKind kind){
        return switch(kind){
            case POT,SHELF,CRAFTER->ScreenHandlerType.GENERIC_9X3;
            case HOPPER->ScreenHandlerType.HOPPER;case DROPPER,DISPENSER->ScreenHandlerType.GENERIC_3X3;
            case FURNACE->ScreenHandlerType.FURNACE;case SMOKER->ScreenHandlerType.SMOKER;case BLAST_FURNACE->ScreenHandlerType.BLAST_FURNACE;
            case BREWING_STAND->ScreenHandlerType.BREWING_STAND;default->ScreenHandlerType.GENERIC_9X3;
        };
    }
    public static void setRemotePolicy(ContainerPolicyPacket policy){
        if(policy==null){remote=null;detailed=false;net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.clearPending();}
        else if(!detailed)remote=new DetailedContainerPolicyPacket(policy.enabled(),policy.creativeOnly(),ContainerKind.allMask());
    }
    public static void setDetailedPolicy(DetailedContainerPolicyPacket policy){
        remote=policy;detailed=true;
        var config=QuickShulkerMod.getConfig();
        config.quickContainers=policy.enabled();config.quickContainersCreativeOnly=policy.creativeOnly();
        ContainerKind.applyMask(config,policy.mask());
    }
    public static boolean hasDetailedPolicy(){return detailed;}
    public static boolean allowed(PlayerEntity player){
        var config=QuickShulkerMod.getConfig();
        boolean enabled=config.quickContainers,creative=config.quickContainersCreativeOnly;
        if(player.getEntityWorld().isClient() && remote!=null){enabled &= remote.enabled();creative |= remote.creativeOnly();}
        return !player.isSpectator()&&enabled&&(!creative||player.isCreative());
    }
    public static boolean allowed(PlayerEntity player,ItemStack stack){
        var kind=kind(stack);if(kind==null||!allowed(player)||!kind.enabled(QuickShulkerMod.getConfig()))return false;
        return !player.getEntityWorld().isClient()||remote==null||(remote.mask()&kind.bit())!=0;
    }
    public static boolean custom(ContainerKind kind){return kind==ContainerKind.POT||kind==ContainerKind.SHELF||kind==ContainerKind.CRAFTER;}
    public static void open(PlayerEntity player,ItemStack stack){
        var kind=kind(stack);if(kind==null||!allowed(player,stack)||stack.getCount()!=1)return;
        if(stack.getOrDefault(DataComponentTypes.CONTAINER,net.minecraft.component.type.ContainerComponent.DEFAULT).stream().count()>kind.slots){
            player.sendMessage(Text.translatable("quickshulker.container.oversized"), true);return;
        }
        var data=stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        if(data!=null && data.copyNbtWithoutId().contains("LootTable")){player.sendMessage(Text.translatable("quickshulker.container.loot_table"), true);return;}
        if(custom(kind) && player instanceof ServerPlayerEntity server && !net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.canSend(server)){
            player.sendMessage(Text.translatable("quickshulker.container.update_client"), true);return;
        }
        if(custom(kind) && player instanceof ServerPlayerEntity server)
            net.kyrptonaught.quickshulker.network.PortableMenuEnvelope.prepare(server,kind);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((id,inv,opener)->create(id,inv,stack,kind),Text.translatable("quickshulker.container.storage",stack.getName())));
    }
    public static ScreenHandler create(int id,PlayerInventory inv,ItemStack stack,ContainerKind kind){
        if(kind==ContainerKind.CRAFTER)return new PortableCrafterMenu(id,inv,stack);
        var contents=new ItemStackInventory(stack,kind.slots);
        return switch(kind){
            case POT,SHELF->new CompactStorageMenu(id,inv,contents,kind.slots);
            case FURNACE->new FurnaceScreenHandler(id,inv,contents,new ArrayPropertyDelegate(4));
            case BLAST_FURNACE->new BlastFurnaceScreenHandler(id,inv,contents,new ArrayPropertyDelegate(4));
            case SMOKER->new SmokerScreenHandler(id,inv,contents,new ArrayPropertyDelegate(4));
            case BREWING_STAND->new BrewingStandScreenHandler(id,inv,contents,new ArrayPropertyDelegate(2));
            default->new PortableStorageHandler(id,inv,stack,new Layout(kind.slots,kind.slots,type(kind)));
        };
    }
    public static void refresh(net.minecraft.server.MinecraftServer server){
        for(var player:server.getPlayerManager().getPlayerList()){
            var session=(net.kyrptonaught.quickshulker.api.ItemInventoryContainer)player.currentScreenHandler;
            if(supports(session.getSourceStack()) && !allowed(player,session.getSourceStack()))player.closeHandledScreen();
            ContainerPolicyPacket.send(player);
        }
    }
}
