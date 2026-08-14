package net.kyrptonaught.quickshulker.mixin;

import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ScreenHandler.class)
public abstract class ContainerMixin implements ItemInventoryContainer {

    int playerInvSlot = -1;

    @Unique
    private Inventory quickshulker$sourceInventory;

    @Unique
    private int quickshulker$sourceInventorySlot = -1;

    @Unique
    private ItemStack quickshulker$sourceStack = ItemStack.EMPTY;

    @Unique
    private PlayerEntity quickshulker$sourcePlayer;

    public int getUsedSlotInPlayerInv() {
        return playerInvSlot;
    }

    public void setUsedSlot(int playerInvSlotID) {
        this.playerInvSlot = playerInvSlotID;
    }

    @Override
    public void setSourceSlot(PlayerEntity player, Inventory inventory, int inventorySlot, ItemStack stack) {
        this.quickshulker$sourcePlayer = player;
        this.quickshulker$sourceInventory = inventory;
        this.quickshulker$sourceInventorySlot = inventorySlot;
        this.quickshulker$sourceStack = stack;
    }

    @Override
    public Inventory getSourceInventory() {
        return this.quickshulker$sourceInventory;
    }

    @Override
    public int getSourceInventorySlot() {
        return this.quickshulker$sourceInventorySlot;
    }

    @Override
    public ItemStack getSourceStack() {
        return this.quickshulker$sourceStack;
    }

    @Shadow
    @Final
    public DefaultedList<Slot> slots;

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    public void QS$onClick(int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (isSourceSlot(slot.inventory, slot.getIndex())) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "sendContentUpdates", at = @At("HEAD"))
    private void quickshulker$validateSourceSlot(CallbackInfo ci) {
        if (this.quickshulker$sourceInventory == null) {
            return;
        }

        this.quickshulker$sourceInventory.markDirty();
        boolean validSlot = this.quickshulker$sourceInventorySlot >= 0
                && this.quickshulker$sourceInventorySlot < this.quickshulker$sourceInventory.size();
        boolean validStack = validSlot
                && this.quickshulker$sourceInventory.getStack(this.quickshulker$sourceInventorySlot) == this.quickshulker$sourceStack;
        if (!validStack && this.quickshulker$sourcePlayer instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            this.quickshulker$sourceInventory = null;
            serverPlayer.closeHandledScreen();
        }
    }

    @Inject(method = "onClosed", at = @At("HEAD"))
    private void quickshulker$markSourceDirty(PlayerEntity player, CallbackInfo ci) {
        if (this.quickshulker$sourceInventory != null) {
            this.quickshulker$sourceInventory.markDirty();
        }
    }
}
