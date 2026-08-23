package com.sighs.petiteinventory.platform.mixin;

import com.sighs.petiteinventory.inventory.ContainerAutomationService;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraftforge.items.VanillaInventoryCodeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bridges Petite's footprint rules into Forge's capability hopper path. */
@Mixin(value = VanillaInventoryCodeHooks.class, remap = false)
public abstract class VanillaInventoryCodeHooksMixin {
    @Inject(method = "insertHook", at = @At("HEAD"), cancellable = true, remap = false)
    private static void petiteinventory$insertHook(HopperBlockEntity hopper,
                                                   CallbackInfoReturnable<Boolean> callback) {
        Boolean handled = ContainerAutomationService.tryInsertFromForgeHook(hopper);
        if (handled != null) {
            callback.setReturnValue(handled);
        }
    }
}
