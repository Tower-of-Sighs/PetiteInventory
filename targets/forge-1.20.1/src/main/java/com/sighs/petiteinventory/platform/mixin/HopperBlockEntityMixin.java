package com.sighs.petiteinventory.platform.mixin;

import com.sighs.petiteinventory.inventory.ContainerAutomationService;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies footprint-aware insertion to vanilla hopper automation. */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(method = "tryMoveInItem", at = @At("HEAD"), cancellable = true)
    private static void petiteinventory$tryMoveInItem(@Nullable Container source,
                                                      Container target,
                                                      ItemStack incoming,
                                                      int slot,
                                                      @Nullable Direction direction,
                                                      CallbackInfoReturnable<ItemStack> callback) {
        ItemStack remainder = ContainerAutomationService.tryInsertFromHopper(target, incoming, direction);
        if (remainder != null) {
            callback.setReturnValue(remainder);
        }
    }
}
