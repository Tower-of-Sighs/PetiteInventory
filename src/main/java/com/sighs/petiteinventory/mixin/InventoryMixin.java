package com.sighs.petiteinventory.mixin;

import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.utils.SlotUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Inventory.class)
public abstract class InventoryMixin {
    @Shadow @Final public Player player;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void check(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
//        SlotUtils.addInventoryItem(player, itemStack);
        Inventory inventory = player.getInventory();
        if (SlotUtils.hasEmptyHotbarSlot(player)) return;
        int slotIndex = SlotUtils.findSlotIndexForArea(player, Area.of(itemStack));
        if (slotIndex == -1) cir.setReturnValue(false);
        else cir.setReturnValue(inventory.add(slotIndex, itemStack));
    }
}
