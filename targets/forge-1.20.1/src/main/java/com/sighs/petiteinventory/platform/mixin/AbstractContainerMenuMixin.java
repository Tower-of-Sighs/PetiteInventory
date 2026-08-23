package com.sighs.petiteinventory.platform.mixin;

import com.sighs.petiteinventory.compat.SophisticatedBackpacksCompat;
import com.sighs.petiteinventory.platform.IAbstractContainerMenu;
import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.inventory.ContainerGrid;
import com.sighs.petiteinventory.inventory.ItemInventoryService;
import com.sighs.petiteinventory.inventory.InventorySlotService;
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
        if (p_150403_.isCreative()) return;
        if (slot < 0 || slot >= slots.size()) return;
        if (type != ClickType.QUICK_MOVE) return;
        if (SophisticatedBackpacksCompat.isBackpackMenu((AbstractContainerMenu) (Object) this)) return;

        Slot clickedSlot = slots.get(slot);
        ItemStack clickedItem = clickedSlot.getItem();
        if (clickedItem.isEmpty()) return;

        // Result slots (merchant, crafting, anvil, smithing, etc.) deliberately
        // reject placement. Let vanilla handle them so their onTake hooks consume
        // inputs and award the correct costs instead of duplicating the result.
        if (!clickedSlot.mayPlace(clickedItem) || isResultLikeSlot(clickedSlot)) return;

        ItemStack sourceBefore = clickedItem.copy();
        int sourceCountBefore = clickedItem.getCount();

        // ========== 1. 精确判断移动方向 ==========
        boolean toHotbar = false;           // 是否最终进入快捷栏
        boolean withinInventory = false;    // 是否在背包内部移动
        List<Slot> targetSlots = new ArrayList<>();
        List<List<Slot>> targetGroups = new ArrayList<>();

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
                targetGroups.add(targetSlots);
            } else {
                withinInventory = true; // 快捷栏 → 主背包
                for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.INV_SLOT_END; i++) {
                    targetSlots.add(this.slots.get(i));
                }
                targetGroups.add(targetSlots);
            }
        } else {
            // 容器 ↔ 背包
            if (clickedSlot.container instanceof Inventory) {
                // 背包 → 容器
                slots.forEach(s -> {
                    if (!(s.container instanceof Inventory) && s.mayPlace(clickedItem)) targetSlots.add(s);
                });
                targetGroups.add(targetSlots);
            } else {
                // 容器 -> 玩家物品栏：快捷栏优先，主物品栏作为后备。
                List<Slot> hotbarSlots = new ArrayList<>();
                List<Slot> mainInventorySlots = new ArrayList<>();
                slots.forEach(s -> {
                    if (!s.mayPlace(clickedItem)) return;
                    if (InventorySlotService.isPlayerHotbarSlot(s)) {
                        hotbarSlots.add(s);
                    } else if (InventorySlotService.isPlayerMainInventorySlot(s)) {
                        mainInventorySlots.add(s);
                    }
                });
                targetSlots.addAll(hotbarSlots);
                targetSlots.addAll(mainInventorySlots);
                targetGroups.add(hotbarSlots);
                targetGroups.add(mainInventorySlots);
            }
        }

        // Some mod menus expose a player inventory without any usable container
        // slots. There is no valid custom transfer target in that case.
        if (!toHotbar && targetGroups.stream().noneMatch(group -> !group.isEmpty())) return;

        // ========== 2. 优先尝试堆叠到现有物品（忽略旋转标记） ==========
        if (tryStackToExisting(clickedItem, targetSlots) && clickedItem.isEmpty()) {
            if (clickedItem.isEmpty()) {
                clickedSlot.set(ItemStack.EMPTY);
            }
            notifyQuickMoveTaken(clickedSlot, p_150403_, sourceBefore, sourceCountBefore, clickedItem);
            ci.cancel();
            return;
        }

        // ========== 3. 处理快捷栏移动：清除NBT，强制1×1 ==========
        if (toHotbar) {
            // 清除旋转状态，使其变为普通1×1物品
            ItemInventoryService.ItemRotateHelper.setRotated(clickedItem, false);
            // 使用原版逻辑直接移动到快捷栏（不经过网格计算）
            int start = targetSlots.get(0).index;
            int end = targetSlots.get(targetSlots.size() - 1).index;
            moveItemStackTo(clickedItem, start, end, false);
            notifyQuickMoveTaken(clickedSlot, p_150403_, sourceBefore, sourceCountBefore, clickedItem);
            ci.cancel();
            return;
        }

        // ========== 4. 非快捷栏移动：保持多尺寸逻辑 ==========
        Area area = ItemInventoryService.getArea(clickedItem);
        // 多尺寸物品按区域查找
        if (!clickedItem.isEmpty()) {
            Slot targetSlot = findAreaInPriorityGroups(targetGroups, area);
            if (targetSlot == null) {
                // 尝试旋转
                boolean wasRotated = ItemInventoryService.ItemRotateHelper.isRotated(clickedItem);
                ItemInventoryService.ItemRotateHelper.setRotated(clickedItem, !wasRotated);
                Area rotatedArea = ItemInventoryService.getArea(clickedItem);
                targetSlot = findAreaInPriorityGroups(targetGroups, rotatedArea);
                if (targetSlot == null) {
                    ItemInventoryService.ItemRotateHelper.setRotated(clickedItem, wasRotated);
                }
            }

            if (targetSlot != null) {
                int idx = targetSlot.index;
                moveItemStackTo(clickedItem, idx, idx + 1, false);
            }
        }

        notifyQuickMoveTaken(clickedSlot, p_150403_, sourceBefore, sourceCountBefore, clickedItem);
        ci.cancel();
    }

    @Unique
    private boolean isResultLikeSlot(Slot slot) {
        String name = slot.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith("resultslot")
                || name.endsWith("outputslot")
                || name.contains("merchantresult")
                || name.contains("traderesult");
    }

    @Unique
    private void notifyQuickMoveTaken(Slot source, Player player, ItemStack original,
                                      int originalCount, ItemStack remaining) {
        int movedCount = originalCount - remaining.getCount();
        if (movedCount <= 0) return;

        ItemStack moved = original.copy();
        moved.setCount(Math.min(movedCount, original.getMaxStackSize()));
        source.onTake(player, moved);
    }

    @Unique
    private Slot findAreaInPriorityGroups(List<List<Slot>> groups, Area area) {
        for (List<Slot> group : groups) {
            if (group.isEmpty()) continue;
            ContainerGrid grid = ContainerGrid.parse(group);
            ContainerGrid.Cell cell = grid.findArea(area);
            if (cell != null) return cell.slot();
        }
        return null;
    }

    /**
     * 尝试将物品堆叠到目标区域中的现有物品上（忽略旋转标记）
     */
    @Unique
    private boolean tryStackToExisting(ItemStack stack, List<Slot> targetSlots) {
        if (stack.isEmpty() || !stack.isStackable()) return false;

        boolean stacked = false;
        ItemStack workingStack = stack.copy();

        // 遍历所有目标槽位，尝试堆叠
        for (Slot slot : targetSlots) {
            ItemStack slotItem = slot.getItem();
            if (slotItem.isEmpty()) continue;

            if (ItemInventoryService.isSameItemIgnoreRotate(slotItem, workingStack)) {
                int max = Math.min(slot.getMaxStackSize(), workingStack.getMaxStackSize());
                int add = Math.min(workingStack.getCount(), max - slotItem.getCount());

                if (add > 0) {
                    slotItem.grow(add);
                    workingStack.shrink(add);
                    slot.setChanged();
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

            // 检查槽位中是否有相同物品可以堆叠（忽略旋转标记）
            if (!slotItem.isEmpty() && ItemInventoryService.isSameItemIgnoreRotate(stackToMove, slotItem)) {
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

    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    private void handleMultiSizeItemOnClose(Player player, CallbackInfo ci) {
        if (player.level().isClientSide) return;
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;

        ci.cancel();
        setCarried(ItemStack.EMPTY);

        Inventory inv = player.getInventory();

        Area originalArea = ItemInventoryService.getArea(carried);
        boolean isOneByOne = originalArea.width() == 1 && originalArea.height() == 1;

        ItemStack workingStack = carried.copy();

        /* ===== 1. 全局叠加（快捷栏优先，然后主背包，忽略旋转标记）===== */
        if (isOneByOne) {
            // 尝试快捷栏堆叠
            for (int hotbar = 0; hotbar < 9; hotbar++) {
                ItemStack slot = inv.getItem(hotbar);
                if (!slot.isEmpty() && ItemInventoryService.isSameItemIgnoreRotate(slot, workingStack)) {
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
                if (!slot.isEmpty() && ItemInventoryService.isSameItemIgnoreRotate(slot, workingStack)) {
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

                ContainerGrid.Cell cell = grid.findArea(originalArea);
                if (cell != null) {
                    int idx = 9 + cell.x() + cell.y() * 9;
                    inv.setItem(idx, workingStack.copy());
                    return;
                }

                boolean wasRot = ItemInventoryService.ItemRotateHelper.isRotated(workingStack);
                ItemInventoryService.ItemRotateHelper.setRotated(workingStack, !wasRot);
                Area rotatedArea = ItemInventoryService.getArea(workingStack);
                cell = grid.findArea(rotatedArea);
                if (cell != null) {
                    int idx = 9 + cell.x() + cell.y() * 9;
                    inv.setItem(idx, workingStack.copy());
                    return;
                }

                ItemStack copyForHotbar = workingStack.copy();
                ItemInventoryService.ItemRotateHelper.setRotated(copyForHotbar, false);
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
        return ItemInventoryService.getArea(stack);
    }
}
