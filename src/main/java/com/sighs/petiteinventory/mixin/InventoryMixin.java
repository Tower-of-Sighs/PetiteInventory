package com.sighs.petiteinventory.mixin;

import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.utils.ItemUtils;
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
        /* 1. 只要快捷栏有空位，一律让原版处理（多尺寸物品先清 NBT 变 1×1） */
        if (SlotUtils.hasEmptyHotbarSlot(player)) {
            ItemUtils.ItemRotateHelper.setRotated(itemStack, false);
            return;                      // 走原版，大概率直接进快捷栏
        }

        /* 2. 快捷栏没空位，再看是不是多尺寸物品 */
        Area currentArea = Area.of(itemStack);
        if (currentArea.width() == 1 && currentArea.height() == 1) {
            return;                      // 1×1 物品让原版在主背包里自己找可堆叠槽
        }

        /* 3. 真正多尺寸，在主背包 27 槽里找能放下的区域 */
        Inventory inventory = player.getInventory();
        int slotIndex = SlotUtils.findSlotIndexForArea(player, currentArea);

        /* 4. 当前方向放不下，就转一次再试 */
        if (slotIndex == -1) {
            boolean wasRotated = ItemUtils.ItemRotateHelper.isRotated(itemStack);
            ItemUtils.ItemRotateHelper.setRotated(itemStack, !wasRotated);
            Area rotatedArea = Area.of(itemStack);
            slotIndex = SlotUtils.findSlotIndexForArea(player, rotatedArea);
            if (slotIndex == -1) {       // 旋转后也放不下，还原标记
                ItemUtils.ItemRotateHelper.setRotated(itemStack, wasRotated);
            }
        }

        /* 5. 最终拍板：找到就塞，找不到就拒绝 */
        if (slotIndex == -1) {
            cir.setReturnValue(false);
        } else {
            ItemStack existing = inventory.getItem(slotIndex);
            if (existing.isEmpty()) {                                         // 空槽
                inventory.setItem(slotIndex, itemStack.copy());
                itemStack.setCount(0);
                cir.setReturnValue(true);
            } else if (ItemStack.isSameItemSameTags(existing, itemStack)) {   // 可堆叠
                int max = Math.min(inventory.getMaxStackSize(), itemStack.getMaxStackSize());
                int total = existing.getCount() + itemStack.getCount();
                if (total <= max) {
                    existing.setCount(total);
                    itemStack.setCount(0);
                    cir.setReturnValue(true);
                } else {
                    existing.setCount(max);
                    itemStack.setCount(total - max);
                    cir.setReturnValue(false);
                }
            } else {                                                          // 被别的东西占了
                cir.setReturnValue(false);
            }
        }
    }
}