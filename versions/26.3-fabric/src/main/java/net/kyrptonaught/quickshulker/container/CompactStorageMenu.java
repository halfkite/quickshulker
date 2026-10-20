package net.kyrptonaught.quickshulker.container;
import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.kyrptonaught.quickshulker.gui.MenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
/** A real one- or three-slot menu. No inaccessible padding or borrowed hopper menu. */
public final class CompactStorageMenu extends AbstractContainerMenu {
    private final int storageSlots;
    public CompactStorageMenu(int id, Inventory inv, int count) { this(id,inv,new SimpleContainer(count),count); }
    public CompactStorageMenu(int id, Inventory inv, Container contents, int count) {
        super(count==1 ? MenuTypes.pot() : MenuTypes.shelf(),id);
        if(count!=1 && count!=3) throw new IllegalArgumentException("Unsupported compact layout");
        checkContainerSize(contents,count); storageSlots=count;
        for(int i=0;i<count;i++) addSlot(new Slot(contents,i,80-(count-1)*9+i*18,27));
        addStandardInventorySlots(inv,8,76);
    }
    public int storageSlots() { return storageSlots; }
    public boolean stillValid(Player player) {
        return player.level().isClientSide() || ((ItemInventoryContainer)(Object)this).isSourceValid(player);
    }
    public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size()||!stillValid(player)) return ItemStack.EMPTY;
        Slot slot=getSlot(index);
        if(!slot.hasItem()||!slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack item=slot.getItem(), before=item.copy();
        if(index<storageSlots) {
            if(!moveItemStackTo(item,storageSlots,slots.size(),true))return ItemStack.EMPTY;
        } else if(!moveItemStackTo(item,0,storageSlots,false))return ItemStack.EMPTY;
        if(item.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        return before;
    }
}
