package com.sighs.petiteinventory.platform.mixin;

import com.sighs.petiteinventory.event.InventoryEvents;
import com.sighs.petiteinventory.platform.ISophisticatedStorageMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sophisticated Core adapter; policy lives in the internal event subscriber. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class StorageContainerMenuBaseMixin implements ISophisticatedStorageMenu {
    @Shadow
    public abstract boolean isStorageInventorySlot(int slot);

    @Shadow
    public abstract int getNumberOfRows();

    @Override
    public boolean petiteinventory$isStorageInventorySlot(int slot) {
        return isStorageInventorySlot(slot);
    }

    @Override
    public int petiteinventory$getNumberOfRows() {
        return getNumberOfRows();
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void publishStorageQuickMove(Player player, int slotIndex,
                                         CallbackInfoReturnable<ItemStack> callback) {
        InventoryEvents.StorageQuickMove event = new InventoryEvents.StorageQuickMove(this, slotIndex, player);
        InventoryEvents.publish(event);
        if (event.isHandled()) {
            callback.setReturnValue((ItemStack) event.result());
        }
    }
}
