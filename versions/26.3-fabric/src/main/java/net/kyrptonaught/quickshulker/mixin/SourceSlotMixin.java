package net.kyrptonaught.quickshulker.mixin;

import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SourceSlotMixin {
    @Shadow @Final public Container container;
    @Shadow public abstract int getContainerSlot();

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void lockInsertion(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (container instanceof Inventory playerInventory
                && ((ItemInventoryContainer) playerInventory.player.containerMenu).isSourceSlot(container, getContainerSlot()))
            cir.setReturnValue(false);
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void lockTaking(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (((ItemInventoryContainer) player.containerMenu).isSourceSlot(container, getContainerSlot()))
            cir.setReturnValue(false);
    }
}
