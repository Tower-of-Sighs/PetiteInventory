package com.sighs.petiteinventory.mixin;

import com.sighs.petiteinventory.api.IAbstractContainerMenu;
import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.init.ContainerGrid;
import com.sighs.petiteinventory.utils.ItemUtils;
import com.sighs.petiteinventory.utils.OperateUtils;
import com.sighs.petiteinventory.utils.SlotUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin implements IAbstractContainerMenu {
    @Shadow @Final public NonNullList<Slot> slots;

    @Shadow protected abstract boolean moveItemStackTo(ItemStack p_38904_, int p_38905_, int p_38906_, boolean p_38907_);

    @Unique
    private Player player;

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Shadow
    public abstract ItemStack getCarried();

    @Shadow
    public abstract void setCarried(ItemStack itemStack);

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void qq(int slot, int p_150401_, ClickType type, Player p_150403_, CallbackInfo ci) {
        if (player.isCreative()) return;
        if (slot < 0 || slot >= slots.size()) return;
        if (type != ClickType.QUICK_MOVE) return;

        Slot clickedSlot = slots.get(slot);
        ItemStack clickedItem = clickedSlot.getItem();
        if (clickedItem.isEmpty()) return;

        // ========== 1. 精确判断移动方向 ==========
        boolean toHotbar = false;           // 是否最终进入快捷栏
        boolean withinInventory = false;    // 是否在背包内部移动
        List<Slot> targetSlots = new ArrayList<>();
        ContainerGrid targetGrid = null;

        if ((Object)this instanceof InventoryMenu) {
            // 背包内部：区分主背包(9-35) ↔ 快捷栏(0-8)
            boolean isMainSlot = slot >= InventoryMenu.INV_SLOT_START && slot < InventoryMenu.INV_SLOT_END;
            boolean isHotbarSlot = slot >= InventoryMenu.USE_ROW_SLOT_START && slot < InventoryMenu.USE_ROW_SLOT_END;
            if (!isMainSlot && !isHotbarSlot) return; // 装备栏走原版

            if (isMainSlot) {
                toHotbar = true; // 主背包 → 快捷栏
                for (int i = InventoryMenu.USE_ROW_SLOT_START; i < InventoryMenu.USE_ROW_SLOT_END; i++) {
                    targetSlots.add(this.slots.get(i));
                }
            } else {
                withinInventory = true; // 快捷栏 → 主背包
                for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.INV_SLOT_END; i++) {
                    targetSlots.add(this.slots.get(i));
                }
                targetGrid = ContainerGrid.parse(targetSlots);
            }
        } else {
            // 容器 ↔ 背包
            if (clickedSlot.container instanceof Inventory) {
                // 背包 → 容器
                slots.forEach(s -> { if (!(s.container instanceof Inventory)) targetSlots.add(s); });
                targetGrid = ContainerGrid.parse(targetSlots);
            } else {
                // 容器 → 背包（可能包含快捷栏）
                slots.forEach(s -> { if (s.container instanceof Inventory) targetSlots.add(s); });
                // 检测目标槽位是否包含快捷栏
                toHotbar = targetSlots.stream().anyMatch(s -> s.index >= 0 && s.index <= 8);
                targetGrid = ContainerGrid.parse(targetSlots);
            }
        }

        // ========== 2. 处理快捷栏移动：清除NBT，强制1×1 ==========
        if (toHotbar) {
            // 清除旋转状态，使其变为普通1×1物品
            ItemUtils.ItemRotateHelper.setRotated(clickedItem, false);
            // 使用原版逻辑直接移动到快捷栏（不经过网格计算）
            int start = targetSlots.get(0).index;
            int end = targetSlots.get(targetSlots.size() - 1).index;
            moveItemStackTo(clickedItem, start, end, false);
            ci.cancel();
            return;
        }

        // ========== 3. 非快捷栏移动：保持多尺寸逻辑 ==========
        Area area = Area.of(clickedItem);
        int start = targetSlots.get(0).index;
        int end = targetSlots.get(targetSlots.size() - 1).index;

        // 1×1可堆叠物品优先合并
        if (clickedItem.isStackable() && area.width() == 1 && area.height() == 1) {
            if (tryMoveStackableItem(clickedItem, start, end, false)) {
                ci.cancel();
                return;
            }
        }

        // 多尺寸物品按区域查找
        if (!clickedItem.isEmpty()) {
            ContainerGrid.Cell targetCell = targetGrid.findArea(area);
            if (targetCell == null) {
                // 尝试旋转
                boolean wasRotated = ItemUtils.ItemRotateHelper.isRotated(clickedItem);
                ItemUtils.ItemRotateHelper.setRotated(clickedItem, !wasRotated);
                Area rotatedArea = Area.of(clickedItem);
                targetCell = targetGrid.findArea(rotatedArea);
                if (targetCell == null) {
                    ItemUtils.ItemRotateHelper.setRotated(clickedItem, wasRotated);
                }
            }

            if (targetCell != null) {
                int idx = targetCell.slot().index;
                moveItemStackTo(clickedItem, idx, idx + 1, false);
            }
        }

        ci.cancel();
    }

    /**
     * 尝试将可堆叠物品移动到指定范围内的槽位中
     * 只处理堆叠逻辑，不处理空槽位
     *
     * @param stackToMove 要移动的物品堆（必须是可堆叠的）
     * @param startIndex 起始槽位索引
     * @param endIndex 结束槽位索引（不包含）
     * @param reverse 是否反向遍历
     * @return 是否成功移动了物品
     */
    private boolean tryMoveStackableItem(ItemStack stackToMove, int startIndex, int endIndex, boolean reverse) {
        boolean moved = false;
        int currentIndex = reverse ? endIndex - 1 : startIndex;

        // 遍历指定范围内的槽位，尝试堆叠物品
        while (!stackToMove.isEmpty()) {
            // 检查是否超出遍历范围
            if (reverse) {
                if (currentIndex < startIndex) break;
            } else {
                if (currentIndex >= endIndex) break;
            }

            Slot slot = this.slots.get(currentIndex);
            ItemStack slotItem = slot.getItem();

            // 检查槽位中是否有相同物品可以堆叠
            if (!slotItem.isEmpty() && ItemStack.isSameItemSameTags(stackToMove, slotItem)) {
                moved = tryStackItems(stackToMove, slotItem, slot) || moved;
            }

            // 移动到下一个槽位
            currentIndex = reverse ? currentIndex - 1 : currentIndex + 1;
        }

        return moved;
    }

    /**
     * 尝试将物品堆叠到目标槽位中
     *
     * @param sourceStack 源物品堆（要移动的物品）
     * @param targetStack 目标槽位中的物品堆
     * @param slot 目标槽位
     * @return 是否成功堆叠了物品
     */
    private boolean tryStackItems(ItemStack sourceStack, ItemStack targetStack, Slot slot) {
        int totalCount = targetStack.getCount() + sourceStack.getCount();
        int maxStackSize = Math.min(slot.getMaxStackSize(), sourceStack.getMaxStackSize());

        if (totalCount <= maxStackSize) {
            // 情况1：可以完全合并
            sourceStack.setCount(0);
            targetStack.setCount(totalCount);
            slot.setChanged();
            return true;
        } else if (targetStack.getCount() < maxStackSize) {
            // 情况2：部分合并（填满目标槽位）
            int amountToTransfer = maxStackSize - targetStack.getCount();
            sourceStack.shrink(amountToTransfer);
            targetStack.setCount(maxStackSize);
            slot.setChanged();
            return true;
        }

        return false; // 无法堆叠
    }

    // 添加到 AbstractContainerMenuMixin.java 中

    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    private void handleMultiSizeItemOnClose(Player player, CallbackInfo ci) {
        if (player.level().isClientSide) return;
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;

        ci.cancel();
        setCarried(ItemStack.EMPTY);

        Inventory inv = player.getInventory();

        // ✅ 关键修复1：只计算一次面积，后续都用这个，避免动态变化
        Area originalArea = Area.of(carried);
        boolean isOneByOne = originalArea.width() == 1 && originalArea.height() == 1;

        // ✅ 关键修复2：创建一个干净的工作副本，不影响原始物品
        ItemStack workingStack = carried.copy();

        /* ===== 1. 全局叠加（快捷栏优先，然后主背包）===== */
        // ✅ 修复3：只有1x1物品才尝试堆叠，多尺寸物品跳过此步
        if (isOneByOne) {
            // 尝试快捷栏堆叠
            for (int hotbar = 0; hotbar < 9; hotbar++) {
                ItemStack slot = inv.getItem(hotbar);
                if (ItemStack.isSameItemSameTags(slot, workingStack)) {
                    int max = Math.min(slot.getMaxStackSize(), inv.getMaxStackSize());
                    int add = Math.min(workingStack.getCount(), max - slot.getCount());
                    if (add > 0) {
                        slot.grow(add);
                        workingStack.shrink(add);
                        if (workingStack.isEmpty()) return;   // 全部叠完
                    }
                }
            }

            // 尝试主背包堆叠
            for (int i = 9; i < 36; i++) {
                ItemStack slot = inv.getItem(i);
                if (ItemStack.isSameItemSameTags(slot, workingStack)) {
                    int max = Math.min(slot.getMaxStackSize(), inv.getMaxStackSize());
                    int add = Math.min(workingStack.getCount(), max - slot.getCount());
                    if (add > 0) {
                        slot.grow(add);
                        workingStack.shrink(add);
                        if (workingStack.isEmpty()) return;   // 全部叠完
                    }
                }
            }
        }

        /* ===== 2. 仍有剩余，找空位 ===== */
        if (!workingStack.isEmpty()) {
            // ✅ 修复4：只有1x1物品才使用简单空位查找
            if (isOneByOne) {
                // 1x1物品：主背包空位 → 快捷栏空位
                for (int i = 9; i < 36; i++) {
                    if (inv.getItem(i).isEmpty()) {
                        inv.setItem(i, workingStack.copy());
                        return;
                    }
                }
                for (int hotbar = 0; hotbar < 9; hotbar++) {
                    if (inv.getItem(hotbar).isEmpty()) {
                        inv.setItem(hotbar, workingStack.copy());
                        return;
                    }
                }
            } else {
                // ✅ 修复5：多尺寸物品使用保持原始旋转状态的workingStack
                List<Slot> mainSlots = new ArrayList<>();
                int baseY = 84, baseX = 8, spacing = 18;
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 9; col++) {
                        int idx = 9 + col + row * 9;
                        int x = baseX + col * spacing;
                        int y = baseY + row * spacing;
                        mainSlots.add(new Slot(inv, idx, x, y));
                    }
                }
                ContainerGrid grid = ContainerGrid.parse(mainSlots);

                // 原始方向找空位
                ContainerGrid.Cell cell = grid.findArea(originalArea);
                if (cell != null) {
                    int idx = 9 + cell.x() + cell.y() * 9;
                    inv.setItem(idx, workingStack.copy());
                    return;
                }

                // ✅ 修复6：旋转一次再试（旋转workingStack，不影响原始物品）
                boolean wasRot = ItemUtils.ItemRotateHelper.isRotated(workingStack);
                ItemUtils.ItemRotateHelper.setRotated(workingStack, !wasRot);
                Area rotatedArea = Area.of(workingStack);
                cell = grid.findArea(rotatedArea);
                if (cell != null) {
                    int idx = 9 + cell.x() + cell.y() * 9;
                    inv.setItem(idx, workingStack.copy());
                    return;
                }

                // ✅ 修复7：旋转后也放不下，尝试强制1x1放入快捷栏
                ItemStack copyForHotbar = workingStack.copy();
                ItemUtils.ItemRotateHelper.setRotated(copyForHotbar, false);
                for (int hotbar = 0; hotbar < 9; hotbar++) {
                    if (inv.getItem(hotbar).isEmpty()) {
                        inv.setItem(hotbar, copyForHotbar);
                        return;
                    }
                }
            }
        }

        /* ===== 3. 真 · 掉落 ===== */
        player.drop(workingStack, false);
    }

    @Unique
    private Area getRotatedAreaServer(ItemStack stack) {
        return Area.of(stack); // ✅ 同样读取 NBT 旋转状态
    }
}
