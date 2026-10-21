package net.kyrptonaught.quickshulker.mixin;

import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.kyrptonaught.quickshulker.api.Util;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {CraftingScreenHandler.class, StonecutterScreenHandler.class, ForgingScreenHandler.class,
        GrindstoneScreenHandler.class, CartographyTableScreenHandler.class, LoomScreenHandler.class})
public abstract class CraftingScreenHandlerMixin extends ScreenHandler {
    protected CraftingScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    // Loom 1.15's string-selector remapping drops the descriptor. Preserve the
    // player argument as a typed selector rather than matching canUse(BlockState).
    @Inject(target = @Desc(value = "canUse", args = PlayerEntity.class, ret = boolean.class),
            at = @At("HEAD"), cancellable = true)
    public void overrideCanUse(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (((ItemInventoryContainer) this).hasItem()) {
            var stack = player.getInventory().getStack(((ItemInventoryContainer) this).getUsedSlotInPlayerInv());
            if (Util.isOpenableItem(stack)) cir.setReturnValue(true);
        }
    }
}
