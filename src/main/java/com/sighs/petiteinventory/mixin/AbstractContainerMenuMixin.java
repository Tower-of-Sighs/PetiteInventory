package com.sighs.petiteinventory.mixin;

import com.sighs.petiteinventory.api.IAbstractContainerMenu;
import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.init.ContainerGrid;
import com.sighs.petiteinventory.utils.SlotUtils;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
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

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void qq(int slot, int p_150401_, ClickType type, Player p_150403_, CallbackInfo ci) {
        if (slot < 0 || slot > slots.size()) return;
        if (type != ClickType.QUICK_MOVE) return;
        if ((Object) this instanceof CreativeModeInventoryScreen.ItemPickerMenu) return;
        Slot clickedSlot = slots.get(slot);

        List<Slot> targetContainerSlots = new ArrayList<>();
        ContainerGrid targetGrid;
        if (clickedSlot.container instanceof Inventory) {
            slots.forEach(s -> {
                if (!(s.container instanceof Inventory)) targetContainerSlots.add(s);
            });
            targetGrid = ContainerGrid.parse(targetContainerSlots);
        } else {
            if (SlotUtils.hasEmptyHotbarSlot(player)) return;
            slots.forEach(s -> {
                if (s.container instanceof Inventory) targetContainerSlots.add(s);
            });
            targetGrid = ContainerGrid.parse(targetContainerSlots);
            targetGrid.removeRow(targetGrid.getHeight() - 1);
        }
        ItemStack clickedItem = clickedSlot.getItem();
        int start = targetContainerSlots.get(0).index;
        int end = targetContainerSlots.get(targetContainerSlots.size() - 1).index;

        if (clickedItem.isStackable()) {
            tryMoveStackableItem(clickedItem, start, end, false);
        }
        if (!clickedItem.isEmpty()) {
            ContainerGrid.Cell targetCell = targetGrid.findArea(Area.of(clickedItem));
            if (targetCell != null) {
                int index = targetCell.slot().index;
                moveItemStackTo(clickedItem, index, index + 1, false);
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
}
