package net.kyrptonaught.quickshulker.container;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
public final class PortableCraftingInventory extends ItemStackInventory implements RecipeInputInventory {
    public PortableCraftingInventory(ItemStack host) { super(host,9); }
    public int getWidth(){return 3;}
    public int getHeight(){return 3;}
    public void provideRecipeInputs(RecipeFinder contents){
        for(var item:getHeldStacks())contents.addInput(item);
    }
}
