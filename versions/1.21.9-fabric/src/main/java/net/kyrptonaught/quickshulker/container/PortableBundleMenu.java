package net.kyrptonaught.quickshulker.container;
import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.kyrptonaught.quickshulker.mixin.SlotAccessor;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
public final class PortableBundleMenu extends ScreenHandler {
    private final PortableBundleInventory contents;
    private int firstRow;
    public PortableBundleMenu(int id,PlayerInventory inv){this(id,inv,new PortableBundleInventory(ItemStack.EMPTY));}
    public PortableBundleMenu(int id,PlayerInventory inv,PortableBundleInventory contents){
        super(net.kyrptonaught.quickshulker.gui.LegacyBundleMenus.BUNDLE_ITEM,id);this.contents=contents;
        for(int i=0;i<64;i++){final int index=i;
            addSlot(new Slot(contents,i,8+(i%8)*18,i/8<5?18+(i/8)*18:Integer.MIN_VALUE){
                public boolean canInsert(ItemStack stack){return contents.maxInSlot(index,stack)>0;}
                public int getMaxItemCount(ItemStack stack){return contents.maxInSlot(index,stack);}
            });
        }
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,c+r*9+9,8+c*18,126+r*18));
        for(int c=0;c<9;c++)addSlot(new Slot(inv,c,8+c*18,184));
    }
    public int firstRow(){return firstRow;}
    public void scroll(int rows){
        firstRow=Math.max(0,Math.min(3,firstRow+rows));
        for(int i=0;i<64;i++){
            var slot=(SlotAccessor)getSlot(i);int row=i/8-firstRow;
            slot.setX(row>=0&&row<5?8+(i%8)*18:Integer.MIN_VALUE);
            slot.setY(row>=0&&row<5?18+row*18:Integer.MIN_VALUE);
        }
    }
    public boolean canUse(PlayerEntity player){return player.getEntityWorld().isClient()||((ItemInventoryContainer)(Object)this).isSourceValid(player);}
    public ItemStack quickMove(PlayerEntity player,int index){
        if(index<0||index>=slots.size()||!canUse(player))return ItemStack.EMPTY;
        var slot=getSlot(index);if(!slot.hasStack()||!slot.canTakeItems(player))return ItemStack.EMPTY;
        var stack=slot.getStack();var copy=stack.copy();
        if(index<64){if(!insertItem(stack,64,100,true))return ItemStack.EMPTY;}
        else if(!insertItem(stack,0,64,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();
        return copy;
    }
    public void onClosed(PlayerEntity player){super.onClosed(player);contents.onClose(player);}
}
