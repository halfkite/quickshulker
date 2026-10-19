package net.kyrptonaught.quickshulker.mixin;

import net.kyrptonaught.quickshulker.api.ItemInventoryContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMixin implements ItemInventoryContainer {

    int playerInvSlot = -1;

    @Unique
    private Container quickshulker$sourceInventory;

    @Unique
    private int quickshulker$sourceInventorySlot = -1;

    @Unique
    private ItemStack quickshulker$sourceStack = ItemStack.EMPTY;

    @Unique
    private Player quickshulker$sourcePlayer;

    public int getUsedSlotInPlayerInv() {
        return playerInvSlot;
    }

    public void setUsedSlot(int playerInvSlotID) {
        this.playerInvSlot = playerInvSlotID;
    }

    @Override
    public void setSourceSlot(Player player, Container inventory, int inventorySlot, ItemStack stack) {
        this.quickshulker$sourcePlayer = player;
        this.quickshulker$sourceInventory = inventory;
        this.quickshulker$sourceInventorySlot = inventorySlot;
        this.quickshulker$sourceStack = stack;
    }

    @Override
    public Container getSourceInventory() {
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
    public NonNullList<Slot> slots;

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    public void QS$onClick(int slotId, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        if (getSourceInventory() != null && !isSourceValid(player)) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) serverPlayer.closeContainer();
            ci.cancel();
            return;
        }
        if (actionType == ContainerInput.SWAP && isSourceSlot(player.getInventory(), button)) {
            ci.cancel();
            return;
        }
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (isSourceSlot(slot.container, slot.getContainerSlot())) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void quickshulker$validateSourceSlot(CallbackInfo ci) {
        if (this.quickshulker$sourceInventory == null) {
            return;
        }

        this.quickshulker$sourceInventory.setChanged();
        boolean validSlot = this.quickshulker$sourceInventorySlot >= 0
                && this.quickshulker$sourceInventorySlot < this.quickshulker$sourceInventory.getContainerSize();
        boolean validStack = validSlot && isSourceValid(this.quickshulker$sourcePlayer);
        if (!validStack && this.quickshulker$sourcePlayer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            this.quickshulker$sourceInventory = null;
            serverPlayer.closeContainer();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void quickshulker$markSourceDirty(Player player, CallbackInfo ci) {
        if (this.quickshulker$sourceInventory != null) {
            this.quickshulker$sourceInventory.setChanged();
        }
    }
}
