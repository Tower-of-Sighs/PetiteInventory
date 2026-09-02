package com.sighs.petiteinventory.platform.mixin;

import com.sighs.petiteinventory.event.InventoryEvents;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.items.VanillaInventoryCodeHooks;
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
        InventoryEvents.HopperInsert event = new InventoryEvents.HopperInsert(hopper, null, null);
        InventoryEvents.publish(event);
        if (event.isHandled()) {
            callback.setReturnValue((Boolean) event.remainder());
        }
    }
}
