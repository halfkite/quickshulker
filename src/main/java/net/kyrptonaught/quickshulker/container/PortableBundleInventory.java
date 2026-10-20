package net.kyrptonaught.quickshulker.container;
import net.kyrptonaught.shulkerutils.ItemStackInventory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
/** Uses vanilla's bundle builder for all weight and eligibility calculations. */
public final class PortableBundleInventory extends ItemStackInventory {
    private boolean ready;
    public PortableBundleInventory(ItemStack host) {
        super(host,64);
        var saved=host.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS,BundleContentsComponent.DEFAULT);
        if(saved.size()>64)throw new IllegalArgumentException("Bundle has too many distinct stacks");
        for(int i=0;i<64;i++)setStack(i,i<saved.size()?saved.get(i).copy():ItemStack.EMPTY);
        ready=true;
    }
    public BundleContentsComponent component(){
        var items=new ArrayList<ItemStack>();for(int i=0;i<64;i++)if(!getStack(i).isEmpty())items.add(getStack(i).copy());
        return new BundleContentsComponent(items);
    }
    public int maxInSlot(int slot,ItemStack candidate){
        if(candidate.isEmpty()||candidate.getItem() instanceof net.minecraft.item.BundleItem)return 0;
        var other=new ArrayList<ItemStack>();for(int i=0;i<64;i++)if(i!=slot&&!getStack(i).isEmpty())other.add(getStack(i).copy());
        var builder=new BundleContentsComponent.Builder(new BundleContentsComponent(other));
        return builder.add(candidate.copyWithCount(candidate.getMaxCount()));
    }
    @Override public void markDirty(){
        if(!ready||itemStack.isEmpty()||!canPlayerUse(null))return;
        var value=component();
        if(value.getOccupancy().compareTo(org.apache.commons.lang3.math.Fraction.ONE)>0)
            throw new IllegalStateException("Refusing overweight bundle component");
        itemStack.set(DataComponentTypes.BUNDLE_CONTENTS,value);
    }
}
