package net.kyrptonaught.quickshulker.container;
import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
/** A real one- or three-slot menu. No inaccessible padding or borrowed hopper menu. */
public final class CompactStorageMenu extends ScreenHandler {
    private final int storageSlots;
    public CompactStorageMenu(int id, PlayerInventory inv, int count) { this(id,inv,new SimpleInventory(count),count); }
    public CompactStorageMenu(int id, PlayerInventory inv, Inventory contents, int count) {
        super(ScreenHandlerType.GENERIC_9X3,id);
        if(count!=1 && count!=3) throw new IllegalArgumentException("Unsupported compact layout");
        checkSize(contents,count); storageSlots=count;
        for(int i=0;i<count;i++) addSlot(new Slot(contents,i,80-(count-1)*9+i*18,27));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv, col+row*9+9,8+col*18,76+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,134));
    }
    public int storageSlots() { return storageSlots; }
    public boolean canUse(PlayerEntity player) {
        return player.getEntityWorld().isClient() || ((ItemInventoryContainer)(Object)this).isSourceValid(player);
    }
    public ItemStack quickMove(PlayerEntity player,int index) {
        if(index<0||index>=slots.size()||!canUse(player)) return ItemStack.EMPTY;
        Slot slot=getSlot(index);
        if(!slot.hasStack()||!slot.canTakeItems(player)) return ItemStack.EMPTY;
        ItemStack item=slot.getStack(), before=item.copy();
        if(index<storageSlots) {
            if(!insertItem(item,storageSlots,slots.size(),true))return ItemStack.EMPTY;
        } else if(!insertItem(item,0,storageSlots,false))return ItemStack.EMPTY;
        if(item.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();
        return before;
    }
}
