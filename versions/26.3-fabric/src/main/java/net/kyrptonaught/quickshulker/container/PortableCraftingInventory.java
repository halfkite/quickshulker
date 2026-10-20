package net.kyrptonaught.quickshulker.container;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
public final class PortableCraftingInventory extends ItemStackInventory implements CraftingContainer {
    public PortableCraftingInventory(ItemStack host) { super(host,9); }
    public int getWidth(){return 3;}
    public int getHeight(){return 3;}
    public void fillStackedContents(StackedItemContents contents){
        for(var item:getItems())contents.accountSimpleStack(item);
    }
}
