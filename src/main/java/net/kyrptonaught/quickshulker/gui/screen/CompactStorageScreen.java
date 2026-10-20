package net.kyrptonaught.quickshulker.gui.screen;
import net.kyrptonaught.quickshulker.container.CompactStorageMenu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
public final class CompactStorageScreen extends HandledScreen<CompactStorageMenu> {
    public CompactStorageScreen(CompactStorageMenu menu,PlayerInventory inventory,Text title){
        super(menu,inventory,title);backgroundWidth=176;backgroundHeight=158;playerInventoryTitleY=64;
    }
    protected void drawBackground(DrawContext g,float delta,int mx,int my){
        g.fill(x,y,x+backgroundWidth,y+backgroundHeight,0xff373737);
        g.fill(x+1,y+1,x+backgroundWidth-1,y+backgroundHeight-1,0xffffffff);
        g.fill(x+3,y+3,x+backgroundWidth-3,y+backgroundHeight-3,0xffc6c6c6);
        for(var slot:handler.slots){int sx=x+slot.x-1,sy=y+slot.y-1;
            g.fill(sx,sy,sx+18,sy+18,0xff373737);g.fill(sx+1,sy+1,sx+18,sy+18,0xffffffff);
            g.fill(sx+1,sy+1,sx+17,sy+17,0xff8b8b8b);
        }
    }
    public void render(DrawContext g,int mx,int my,float d){super.render(g,mx,my,d);drawMouseoverTooltip(g,mx,my);}
}
