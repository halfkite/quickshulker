package net.kyrptonaught.quickshulker.gui.screen;
import net.kyrptonaught.quickshulker.container.PortableCrafterMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
public final class PortableCrafterScreen extends CrafterScreen{
    public PortableCrafterScreen(CrafterMenu menu,Inventory inv,Component title){super(menu,inv,title);}
    @Override protected void init(){
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("quickshulker.crafter.craft"),b->
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,PortableCrafterMenu.CRAFT))
            .bounds(leftPos+112,topPos+59,58,16).build());
    }
    private void setEnabled(int slot,boolean enabled){
        menu.setSlotState(slot,enabled);
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId,(enabled?PortableCrafterMenu.ENABLE:PortableCrafterMenu.DISABLE)+slot);
    }
    @Override protected void slotClicked(Slot slot,int slotId,int button,ContainerInput action){
        if(slot instanceof CrafterSlot && !slot.hasItem() && !minecraft.player.isSpectator()){
            if(action==ContainerInput.PICKUP && (menu.isSlotDisabled(slotId)||menu.getCarried().isEmpty())){
                boolean enabled=menu.isSlotDisabled(slotId);
                setEnabled(slotId,enabled);
                if(menu.getCarried().isEmpty())return;
            }else if(action==ContainerInput.SWAP && menu.isSlotDisabled(slotId)
                && !minecraft.player.getInventory().getItem(button).isEmpty())setEnabled(slotId,true);
        }
        super.slotClicked(slot,slotId,button,action);
    }
}
