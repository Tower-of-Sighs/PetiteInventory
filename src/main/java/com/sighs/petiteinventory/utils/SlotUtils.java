package com.sighs.petiteinventory.utils;

import com.sighs.petiteinventory.Config;
import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.init.ContainerGrid;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SlotUtils {
    public static ContainerGrid getContainerGrid(AbstractContainerMenu menu) {
        String menuType = menu.getClass().toString();
        boolean matchedMenu = Config.WHITELIST.get().contains(menuType);
        boolean enableInventory = Config.ENABLE_INVENTORY.get();
        ContainerGrid grid;

        if (menu instanceof InventoryMenu) {
            List<Slot> girdSlot = new ArrayList<>();
            for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.INV_SLOT_END; i++) {
                if (enableInventory) girdSlot.add(menu.getSlot(i));
            }
            grid = ContainerGrid.parse(girdSlot);
        }
        else {
            List<Slot> containerSlot = new ArrayList<>();
            List<Slot> inventorySlot = new ArrayList<>();
            for (Slot slot : menu.slots) {
                if (matchedMenu && !(slot.container instanceof Inventory)) containerSlot.add(slot);
                if (enableInventory && slot.container instanceof Inventory) inventorySlot.add(slot);
            }
            grid = ContainerGrid.parse(containerSlot, inventorySlot);
        }

        if (enableInventory && !(menu instanceof InventoryMenu)) {
            grid.removeRow(grid.getHeight() - 1);
            System.out.print(grid.getCells().size() + ":\n" + grid.getCells()+"\n\n");
        }

        return grid;
    }

    /**
     * 从玩家背包中查找能容纳指定Area的槽位索引
     * @param player 玩家对象
     * @param area 要放置的区域大小
     * @return 符合条件的槽位索引，如果找不到返回-1
     */
    public static int findSlotIndexForArea(Player player, Area area) {
        // 获取玩家背包（27个通用槽位是索引9-35）
        Inventory playerInventory = player.getInventory();

        // 创建27个虚拟Slot来构建ContainerGrid
        List<Slot> slots = createInventorySlots(playerInventory);

        // 构建容器网格
        ContainerGrid grid = ContainerGrid.parse(slots);

        // 查找能容纳Area的空白区域
        ContainerGrid.Cell foundCell = grid.findArea(area);

        if (foundCell != null) {
            // 将Cell坐标转换回槽位索引
            return convertCellToSlotIndex(foundCell);
        }

        return -1; // 没有找到合适的槽位
    }

    /**
     * 创建代表27个通用容器槽位的Slot列表
     * @param inventory 玩家背包
     * @return Slot列表
     */
    private static List<Slot> createInventorySlots(Inventory inventory) {
        List<Slot> slots = new ArrayList<>();

        // 27个通用容器槽位的布局：3行9列
        // 在InventoryMenu中，这些槽位的索引是9-35
        // 坐标计算：第一行y=84，第二行y=102，第三行y=120；x从8开始，每18像素一个槽位

        int baseY = 84; // 第一行的y坐标
        int baseX = 8;  // 第一列的x坐标
        int slotSpacing = 18; // 槽位间距

        int slotIndex = 9; // 通用容器槽位起始索引

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = baseX + col * slotSpacing;
                int y = baseY + row * slotSpacing;

                // 创建虚拟Slot，使用真实的背包和索引
                Slot slot = new InventorySlot(inventory, slotIndex, x, y);
                slots.add(slot);
                slotIndex++;
            }
        }

        return slots;
    }

    /**
     * 将Cell坐标转换为槽位索引
     * @param cell 找到的Cell
     * @return 对应的槽位索引
     */
    private static int convertCellToSlotIndex(ContainerGrid.Cell cell) {
        // 在3x9的网格中，索引计算：9 + column + row * 9
        return 9 + cell.x() + cell.y() * 9;
    }

    /**
     * 自定义的Inventory Slot实现
     */
    private static class InventorySlot extends Slot {
        public InventorySlot(Container container, int slotIndex, int x, int y) {
            super(container, slotIndex, x, y);
        }

        // 使用父类的getItem()方法，它会从背包中获取对应槽位的物品
    }

    public static boolean hasEmptyHotbarSlot(Player player) {
        // 获取玩家的库存
        var inventory = player.getInventory();

        // 遍历快捷栏槽位（索引0到8）
        for (int slot = 0; slot < 9; slot++) {
            // 获取槽位中的ItemStack
            ItemStack itemStack = inventory.getItem(slot);

            // 检查ItemStack是否为空（包括空气物品）
            if (itemStack.isEmpty()) {
                return true; // 找到空槽，直接返回true
            }
        }

        // 所有槽位都有物品，返回false
        return false;
    }
}
