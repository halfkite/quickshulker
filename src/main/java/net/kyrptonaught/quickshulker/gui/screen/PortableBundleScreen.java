package net.kyrptonaught.quickshulker.gui.screen;
import net.kyrptonaught.quickshulker.container.PortableBundleMenu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public final class PortableBundleScreen extends HandledScreen<PortableBundleMenu> {
    public PortableBundleScreen(PortableBundleMenu menu,PlayerInventory inv,Text title){super(menu,inv,title);backgroundWidth=176;backgroundHeight=208;playerInventoryTitleY=114;}
    protected void drawBackground(DrawContext g,float d,int mx,int my){
        g.fill(x,y,x+176,y+208,0xff373737);g.fill(x+1,y+1,x+175,y+207,0xffffffff);g.fill(x+3,y+3,x+173,y+205,0xffc6c6c6);
        for(var slot:handler.slots)if(slot.x>=0&&slot.y>=0){int sx=x+slot.x-1,sy=y+slot.y-1;g.fill(sx,sy,sx+18,sy+18,0xff373737);g.fill(sx+1,sy+1,sx+18,sy+18,0xffffffff);g.fill(sx+1,sy+1,sx+17,sy+17,0xff8b8b8b);}
        g.fill(x+156,y+18,x+168,y+106,0xff555555);int top=y+18+handler.firstRow()*22;g.fill(x+157,top,x+167,top+22,0xffaaaaaa);
    }
    public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){handler.scroll(vertical<0?1:vertical>0?-1:0);return true;}
    public void render(DrawContext g,int mx,int my,float d){super.render(g,mx,my,d);drawMouseoverTooltip(g,mx,my);}
}
