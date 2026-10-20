package net.kyrptonaught.quickshulker.gui.screen;
import net.kyrptonaught.quickshulker.container.CompactStorageMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
/** Draws only the actual one/three storage slots and the player's inventory. */
public final class CompactStorageScreen extends AbstractContainerScreen<CompactStorageMenu>{
    public CompactStorageScreen(CompactStorageMenu menu,Inventory inv,Component title){super(menu,inv,title,176,158);inventoryLabelY=64;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractBackground(g,mouseX,mouseY,delta);
        int x=leftPos,y=topPos;
        g.fill(x,y,x+imageWidth,y+imageHeight,0xff373737);
        g.fill(x+1,y+1,x+imageWidth-1,y+imageHeight-1,0xffffffff);
        g.fill(x+3,y+3,x+imageWidth-3,y+imageHeight-3,0xffc6c6c6);
        for(var slot:menu.slots){
            int sx=x+slot.x-1,sy=y+slot.y-1;
            g.fill(sx,sy,sx+18,sy+18,0xff373737);
            g.fill(sx+1,sy+1,sx+18,sy+18,0xffffffff);
            g.fill(sx+1,sy+1,sx+17,sy+17,0xff8b8b8b);
        }
    }
    public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractRenderState(g,mouseX,mouseY,delta);extractTooltip(g,mouseX,mouseY);
    }
}
