package com.sighs.petiteinventory.platform.mixin;

import com.sighs.petiteinventory.event.InventoryEvents;
import com.sighs.petiteinventory.platform.IAbstractContainerMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mixin entry point only: menu behavior is handled by the internal event bus. */
@Mixin(value = AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin implements IAbstractContainerMenu {
    @Shadow @Final public NonNullList<Slot> slots;
    @Shadow protected abstract boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse);

    @Unique
    private Player petiteinventory$player;

    @Override
    public void setPlayer(Player player) {
        petiteinventory$player = player;
    }

    @Override
    public Player getPlayer() {
        return petiteinventory$player;
    }

    @Override
    public boolean petiteinventory$moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
        return moveItemStackTo(stack, start, end, reverse);
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void publishQuickMove(int slot, int button, ClickType type, Player player, CallbackInfo callback) {
        if (type != ClickType.QUICK_MOVE) {
            return;
        }
        InventoryEvents.QuickMove event = new InventoryEvents.QuickMove(this, slot, button, player);
        InventoryEvents.publish(event);
        if (event.isHandled()) {
            callback.cancel();
        }
    }
}
