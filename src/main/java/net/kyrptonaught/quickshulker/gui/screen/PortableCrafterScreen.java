package net.kyrptonaught.quickshulker.gui.screen;
import net.kyrptonaught.quickshulker.container.PortableCrafterMenu;
import net.minecraft.client.gui.screen.ingame.CrafterScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.slot.*;
import net.minecraft.text.Text;
public final class PortableCrafterScreen extends CrafterScreen {
    public PortableCrafterScreen(CrafterScreenHandler menu,PlayerInventory inv,Text title){super(menu,inv,title);}
    protected void init(){super.init();addDrawableChild(ButtonWidget.builder(Text.translatable("quickshulker.crafter.craft"),
        b->client.interactionManager.clickButton(handler.syncId,PortableCrafterMenu.CRAFT)).dimensions(x+112,y+59,58,16).build());}
    private void enable(int slot,boolean enabled){
        handler.setSlotEnabled(slot,enabled);
        client.interactionManager.clickButton(handler.syncId,(enabled?PortableCrafterMenu.ENABLE:PortableCrafterMenu.DISABLE)+slot);
    }
    protected void onMouseClick(Slot slot,int index,int button,SlotActionType action){
        if(slot instanceof CrafterInputSlot && !slot.hasStack() && !client.player.isSpectator()){
            if(action==SlotActionType.PICKUP && (handler.isSlotDisabled(index)||handler.getCursorStack().isEmpty())){
                enable(index,handler.isSlotDisabled(index));if(handler.getCursorStack().isEmpty())return;
            }else if(action==SlotActionType.SWAP && handler.isSlotDisabled(index) && !client.player.getInventory().getStack(button).isEmpty())enable(index,true);
        }
        super.onMouseClick(slot,index,button,action);
    }
}
