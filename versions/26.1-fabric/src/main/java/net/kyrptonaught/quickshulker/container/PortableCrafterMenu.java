package net.kyrptonaught.quickshulker.container;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.kyrptonaught.quickshulker.gui.MenuTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class PortableCrafterMenu extends CrafterMenu {
    public static final int CRAFT=100, DISABLE=0, ENABLE=9, GRID=9;
    private final ItemStack host;
    private final PortableCraftingInventory contents;
    public PortableCrafterMenu(int id,Inventory inv){super(id,inv);host=null;contents=null;}
    public PortableCrafterMenu(int id,Inventory inv,ItemStack host){
        this(id,inv,host,new PortableCraftingInventory(host),data(host));
    }
    private PortableCrafterMenu(int id,Inventory inv,ItemStack host,PortableCraftingInventory contents,ContainerData data){
        super(id,inv,contents,data);this.host=host;this.contents=contents;
    }
    private static ContainerData data(ItemStack host){
        var data=new SimpleContainerData(10);var stored=host.get(DataComponents.BLOCK_ENTITY_DATA);
        var inputs=new PortableCraftingInventory(host);
        if(stored!=null)for(int i:stored.copyTagWithoutId().getIntArray("disabled_slots").orElse(new int[0]))
            if(i>=0 && i<GRID && inputs.getItem(i).isEmpty())data.set(i,1);
        return data;
    }
    @Override public MenuType<?> getType(){return MenuTypes.crafter();}
    @Override public boolean stillValid(Player player){
        return player.level().isClientSide() || ((ItemInventoryContainer)(Object)this).isSourceValid(player);
    }
    @Override public boolean clickMenuButton(Player player,int button){
        if(!(player instanceof ServerPlayer serverPlayer)||!stillValid(player))return false;
        if(button==CRAFT)return craftOnce(serverPlayer);
        if(button<DISABLE || button>=ENABLE+GRID)return false;
        int slot=button%GRID;
        if(!getSlot(slot).getItem().isEmpty())return false;
        setSlotState(slot,button>=ENABLE);return true;
    }
    @Override public void setSlotState(int slot,boolean enabled){
        if(slot<0||slot>=GRID||!getSlot(slot).getItem().isEmpty())return;
        super.setSlotState(slot,enabled);
        if(host==null || contents==null)return;
        var session=(ItemInventoryContainer)(Object)this;
        // No component writes after the original source (including any ancestor) expires.
        if(session.getSourceInventory()!=null && !contents.stillValid(null))return;
        var old=host.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag=old==null?new CompoundTag():old.copyTagWithoutId();
        tag.putIntArray("disabled_slots",IntStream.range(0,GRID).filter(this::isSlotDisabled).toArray());
        host.set(DataComponents.BLOCK_ENTITY_DATA,TypedEntityData.of(old==null?BlockEntityType.CRAFTER:old.type(),tag));
        contents.setChanged();
    }
    public boolean craftOnce(ServerPlayer player){
        if(contents==null || !stillValid(player))return false;
        var input=contents.asCraftInput();
        var recipe=CrafterBlock.getPotentialResults(player.level(),input);
        if(recipe.isEmpty())return false;
        ItemStack output=recipe.get().value().assemble(input);
        if(output.isEmpty())return false;
        var produced=new ArrayList<ItemStack>();produced.add(output);
        for(var remaining:recipe.get().value().getRemainingItems(input))if(!remaining.isEmpty())produced.add(remaining);
        var session=(ItemInventoryContainer)(Object)this;
        List<ItemStack> after=planInsert(player.getInventory().getNonEquipmentItems(),produced,
            i->session.isSourceSlot(player.getInventory(),i));
        if(after==null){player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("quickshulker.crafter.full"));return false;}
        // Validation and reservation finished before either materials or player inventory are touched.
        for(int i=0;i<GRID;i++)if(!contents.getItem(i).isEmpty())contents.getItem(i).shrink(1);
        contents.setChanged();
        for(int i=0;i<after.size();i++){
            // Consuming ingredients changes the host component after the reservation snapshot.
            // Never restore a stale copy of this host or any ancestor container.
            if(session.isSourceSlot(player.getInventory(),i))continue;
            ItemStack before=player.getInventory().getItem(i), next=after.get(i);
            if(before.getCount()!=next.getCount()||!ItemStack.isSameItemSameComponents(before,next))
                player.getInventory().setItem(i,next);
        }
        player.getInventory().setChanged();
        output.onCraftedBySystem(player.level());
        broadcastChanges();return true;
    }
    /** Pure reservation step: returning null leaves every caller-owned stack unchanged. */
    public static List<ItemStack> planInsert(List<ItemStack> inventory,List<ItemStack> outputs,java.util.function.IntPredicate locked){
        List<ItemStack> result=inventory.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for(ItemStack produced:outputs){
            ItemStack left=produced.copy();
            for(int pass=0;pass<2 && !left.isEmpty();pass++)for(int i=0;i<result.size() && !left.isEmpty();i++){
                if(locked.test(i))continue;
                ItemStack target=result.get(i);
                if(pass==0 && (target.isEmpty()||!ItemStack.isSameItemSameComponents(target,left)))continue;
                if(pass==1 && !target.isEmpty())continue;
                int room=target.isEmpty()?Math.min(64,left.getMaxStackSize()):Math.min(64,target.getMaxStackSize())-target.getCount();
                int count=Math.min(Math.max(room,0),left.getCount());
                if(count==0)continue;
                if(target.isEmpty())result.set(i,left.copyWithCount(count));else target.grow(count);
                left.shrink(count);
            }
            if(!left.isEmpty())return null;
        }
        return result;
    }
}
