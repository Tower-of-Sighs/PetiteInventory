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

    @Shadow
    public abstract int getFreeSlot();

    public abstract ItemStack addResource(ItemStack stack);

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void check(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        /* ===== 1. 优先尝试全局叠加（忽略旋转标记）===== */
        if (tryStackEverywhere(itemStack)) {
            if (itemStack.isEmpty()) {
                cir.setReturnValue(true);
                return;
            }
        }

        /* ===== 2. 处理1x1物品：使用原版逻辑但先清旋转标记 ===== */
        Area currentArea = Area.of(itemStack);
        boolean isOneByOne = currentArea.width() == 1 && currentArea.height() == 1;

        if (isOneByOne) {
            // 清除旋转标记，使其变为普通1×1物品
            ItemUtils.ItemRotateHelper.setRotated(itemStack, false);

            // 再次尝试叠加（原版逻辑）
            if (tryStackEverywhere(itemStack)) {
                if (itemStack.isEmpty()) {
                    cir.setReturnValue(true);
                    return;
                }
            }

            // 让原版处理剩下的
            return;
        }

        /* ===== 3. 多尺寸物品处理 ===== */
        Inventory inventory = player.getInventory();

        // 3.1 先尝试快捷栏叠加（多尺寸物品也可以叠加，但会变成1x1）
        for (int i = 0; i < 9; i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && ItemUtils.isSameItemIgnoreRotate(slot, itemStack)) {
                int max = Math.min(inventory.getMaxStackSize(), itemStack.getMaxStackSize());
                int add = Math.min(itemStack.getCount(), max - slot.getCount());
                if (add > 0) {
                    slot.grow(add);
                    itemStack.shrink(add);
                    if (itemStack.isEmpty()) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }

        // 3.2 再尝试主背包叠加（多尺寸物品只能叠加到相同尺寸的物品上）
        for (int i = 9; i < 36; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) continue;

            Area slotArea = Area.of(slot);
            if (slotArea.width() == currentArea.width() &&
                    slotArea.height() == currentArea.height() &&
                    ItemUtils.isSameItemIgnoreRotate(slot, itemStack)) {

                int max = Math.min(inventory.getMaxStackSize(), itemStack.getMaxStackSize());
                int add = Math.min(itemStack.getCount(), max - slot.getCount());
                if (add > 0) {
                    slot.grow(add);
                    itemStack.shrink(add);
                    if (itemStack.isEmpty()) {
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }

        /* ===== 4. 在主背包找空位放置 ===== */
        int slotIndex = SlotUtils.findSlotIndexForArea(player, currentArea);

        // 当前方向放不下，就转一次再试
        if (slotIndex == -1) {
            boolean wasRotated = ItemUtils.ItemRotateHelper.isRotated(itemStack);
            ItemUtils.ItemRotateHelper.setRotated(itemStack, !wasRotated);
            Area rotatedArea = Area.of(itemStack);
            slotIndex = SlotUtils.findSlotIndexForArea(player, rotatedArea);
            if (slotIndex == -1) {
                ItemUtils.ItemRotateHelper.setRotated(itemStack, wasRotated);
            }
        }

        /* ===== 5. 最终处理 ===== */
        if (slotIndex == -1) {
            // 5.1 如果主背包放不下，尝试转换为1x1放入快捷栏
            ItemUtils.ItemRotateHelper.setRotated(itemStack, false);
            int freeSlot = getFreeSlot();
            if (freeSlot != -1) {
                inventory.setItem(freeSlot, itemStack.copy());
                itemStack.setCount(0);
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(false); // 实在放不下了
        } else {
            ItemStack existing = inventory.getItem(slotIndex);
            if (existing.isEmpty()) {
                inventory.setItem(slotIndex, itemStack.copy());
                itemStack.setCount(0);
                cir.setReturnValue(true);
            } else if (ItemUtils.isSameItemIgnoreRotate(existing, itemStack)) {
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
            } else {
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * 尝试在所有槽位中叠加物品（忽略旋转标记）
     */
    private boolean tryStackEverywhere(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Inventory inventory = player.getInventory();
        boolean stacked = false;
        ItemStack workingStack = stack.copy();

        // 先尝试快捷栏 (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && ItemUtils.isSameItemIgnoreRotate(slot, workingStack)) {
                int max = Math.min(inventory.getMaxStackSize(), workingStack.getMaxStackSize());
                int add = Math.min(workingStack.getCount(), max - slot.getCount());
                if (add > 0) {
                    slot.grow(add);
                    workingStack.shrink(add);
                    stacked = true;
                    if (workingStack.isEmpty()) {
                        stack.setCount(0);
                        return true;
                    }
                }
            }
        }

        // 再尝试主背包 (9-35)
        for (int i = 9; i < 36; i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && ItemUtils.isSameItemIgnoreRotate(slot, workingStack)) {
                int max = Math.min(inventory.getMaxStackSize(), workingStack.getMaxStackSize());
                int add = Math.min(workingStack.getCount(), max - slot.getCount());
                if (add > 0) {
                    slot.grow(add);
                    workingStack.shrink(add);
                    stacked = true;
                    if (workingStack.isEmpty()) {
                        stack.setCount(0);
                        return true;
                    }
                }
            }
        }

        if (stacked) {
            stack.setCount(workingStack.getCount());
        }

        return stacked;
    }
}