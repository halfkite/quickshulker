package net.kyrptonaught.quickshulker.container;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.*;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.block.CrafterBlock;

public final class PortableCrafterMenu extends CrafterScreenHandler {
    public static final int CRAFT=100, DISABLE=0, ENABLE=9, GRID=9;
    private final ItemStack host;
    private final PortableCraftingInventory contents;
    public PortableCrafterMenu(int id,PlayerInventory inv){super(id,inv);host=null;contents=null;}
    public PortableCrafterMenu(int id,PlayerInventory inv,ItemStack host){
        this(id,inv,host,new PortableCraftingInventory(host),data(host));
    }
    private PortableCrafterMenu(int id,PlayerInventory inv,ItemStack host,PortableCraftingInventory contents,PropertyDelegate data){
        super(id,inv,contents,data);this.host=host;this.contents=contents;
    }
    private static PropertyDelegate data(ItemStack host){
        var data=new ArrayPropertyDelegate(10);var stored=host.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        var inputs=new PortableCraftingInventory(host);
        if(stored!=null)for(int i:stored.copyNbt().getIntArray("disabled_slots"))
            if(i>=0 && i<GRID && inputs.getStack(i).isEmpty())data.set(i,1);
        return data;
    }
    @Override public ScreenHandlerType<?> getType(){return ScreenHandlerType.GENERIC_9X3;}
    @Override public boolean canUse(PlayerEntity player){
        return player.getWorld().isClient || ((ItemInventoryContainer)(Object)this).isSourceValid(player);
    }
    @Override public boolean onButtonClick(PlayerEntity player,int button){
        if(!(player instanceof ServerPlayerEntity serverPlayer)||!canUse(player))return false;
        if(button==CRAFT)return craftOnce(serverPlayer);
        if(button<DISABLE || button>=ENABLE+GRID)return false;
        int slot=button%GRID;
        if(!getSlot(slot).getStack().isEmpty())return false;
        setSlotEnabled(slot,button>=ENABLE);return true;
    }
    @Override public void setSlotEnabled(int slot,boolean enabled){
        if(slot<0||slot>=GRID||!getSlot(slot).getStack().isEmpty())return;
        super.setSlotEnabled(slot,enabled);
        if(host==null || contents==null)return;
        var session=(ItemInventoryContainer)(Object)this;
        // No component writes after the original source (including any ancestor) expires.
        if(session.getSourceInventory()!=null && !contents.canPlayerUse(null))return;
        var old=host.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        NbtCompound tag=old==null?new NbtCompound():old.copyNbt();
        tag.putString("id",net.minecraft.registry.Registries.BLOCK_ENTITY_TYPE.getId(net.minecraft.block.entity.BlockEntityType.CRAFTER).toString());
        tag.putIntArray("disabled_slots",IntStream.range(0,GRID).filter(this::isSlotDisabled).toArray());
        host.set(DataComponentTypes.BLOCK_ENTITY_DATA,NbtComponent.of(tag));
        contents.markDirty();
    }
    public boolean craftOnce(ServerPlayerEntity player){
        if(contents==null || !canUse(player))return false;
        var input=contents.createRecipeInput();
        var recipe=CrafterBlock.getCraftingRecipe(player.getServerWorld(),input);
        if(recipe.isEmpty())return false;
        ItemStack output=recipe.get().value().craft(input, player.getWorld().getRegistryManager());
        if(output.isEmpty())return false;
        var produced=new ArrayList<ItemStack>();produced.add(output);
        for(var remaining:recipe.get().value().getRecipeRemainders(input))if(!remaining.isEmpty())produced.add(remaining);
        var session=(ItemInventoryContainer)(Object)this;
        List<ItemStack> after=planInsert(player.getInventory().main,produced,
            i->session.isSourceSlot(player.getInventory(),i));
        if(after==null){player.sendMessage(net.minecraft.text.Text.translatable("quickshulker.crafter.full"),true);return false;}
        // Validation and reservation finished before either materials or player inventory are touched.
        for(int i=0;i<GRID;i++)if(!contents.getStack(i).isEmpty())contents.getStack(i).decrement(1);
        contents.markDirty();
        for(int i=0;i<after.size();i++){
            // Consuming ingredients changes the host component after the reservation snapshot.
            // Never restore a stale copy of this host or any ancestor container.
            if(session.isSourceSlot(player.getInventory(),i))continue;
            ItemStack before=player.getInventory().getStack(i), next=after.get(i);
            if(before.getCount()!=next.getCount()||!ItemStack.areItemsAndComponentsEqual(before,next))
                player.getInventory().setStack(i,next);
        }
        player.getInventory().markDirty();
        output.onCraftByCrafter(player.getWorld());
        sendContentUpdates();return true;
    }
    /** Pure reservation step: returning null leaves every caller-owned stack unchanged. */
    public static List<ItemStack> planInsert(List<ItemStack> inventory,List<ItemStack> outputs,java.util.function.IntPredicate locked){
        List<ItemStack> result=inventory.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for(ItemStack produced:outputs){
            ItemStack left=produced.copy();
            for(int pass=0;pass<2 && !left.isEmpty();pass++)for(int i=0;i<result.size() && !left.isEmpty();i++){
                if(locked.test(i))continue;
                ItemStack target=result.get(i);
                if(pass==0 && (target.isEmpty()||!ItemStack.areItemsAndComponentsEqual(target,left)))continue;
                if(pass==1 && !target.isEmpty())continue;
                int room=target.isEmpty()?Math.min(64,left.getMaxCount()):Math.min(64,target.getMaxCount())-target.getCount();
                int count=Math.min(Math.max(room,0),left.getCount());
                if(count==0)continue;
                if(target.isEmpty())result.set(i,left.copyWithCount(count));else target.increment(count);
                left.decrement(count);
            }
            if(!left.isEmpty())return null;
        }
        return result;
    }
}
