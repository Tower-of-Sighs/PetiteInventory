package com.sighs.petiteinventory.platform.inventory;

import com.sighs.petiteinventory.config.ModConfig;
import com.sighs.petiteinventory.compat.SophisticatedBackpacksCompat;
import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.platform.inventory.ContainerGrid;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InventorySlotService {
    public static ContainerGrid getContainerGrid(AbstractContainerMenu menu) {
        return getContainerGrid(menu, true);
    }

    /**
     * The per-screen switch applies to the foreign container only. Player
     * inventory participation remains exclusively controlled by the config.
     */
    public static ContainerGrid getContainerGrid(AbstractContainerMenu menu, boolean enableContainer) {
        boolean enableInventory = ModConfig.ENABLE_INVENTORY.get();
        ContainerGrid grid;

        if (menu instanceof InventoryMenu) {
            List<Slot> girdSlot = new ArrayList<>();
            for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.INV_SLOT_END; i++) {
                if (enableInventory) girdSlot.add(menu.getSlot(i));
            }
            grid = ContainerGrid.parse(girdSlot);
        }
        else if (SophisticatedBackpacksCompat.isBackpackMenu(menu)) {
            List<Slot> playerMainSlots = enableInventory
                    ? SophisticatedBackpacksCompat.getPlayerMainInventorySlots(menu)
                    : List.of();
            List<Slot> storageSlots = enableContainer
                    ? SophisticatedBackpacksCompat.getStorageSlots(menu)
                    : List.of();
            grid = ContainerGrid.parse(storageSlots, playerMainSlots);
        }
        else {
            List<Slot> containerSlot = new ArrayList<>();
            List<Slot> inventorySlot = new ArrayList<>();
            for (Slot slot : menu.slots) {
                if (enableContainer && !(slot.container instanceof Inventory)) containerSlot.add(slot);
                if (enableInventory && isPlayerMainInventorySlot(slot)) inventorySlot.add(slot);
            }
            grid = ContainerGrid.parse(containerSlot, inventorySlot);
        }

        return grid;
    }

    public static boolean isPlayerHotbarSlot(Slot slot) {
        return slot != null
                && slot.container instanceof Inventory
                && slot.getContainerSlot() >= 0
                && slot.getContainerSlot() < 9;
    }

    public static boolean isPlayerMainInventorySlot(Slot slot) {
        return slot != null
                && slot.container instanceof Inventory
                && slot.getContainerSlot() >= 9
                && slot.getContainerSlot() < 36;
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
        var inventory = player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack itemStack = inventory.getItem(slot);
            if (itemStack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 专门为快捷栏查找空位（0-8号槽位）
     * @return 空槽位索引，若无返回-1
     */
    public static int findEmptyHotbarSlot(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /** 仅用于主背包 27 槽（索引 9~35）的网格 */
    public static ContainerGrid createMainInventoryGrid(Inventory inv) {
        List<Slot> slots = new ArrayList<>(27);
        int baseY = 84, baseX = 8, spacing = 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = 9 + col + row * 9;
                int x = baseX + col * spacing;
                int y = baseY + row * spacing;
                slots.add(new Slot(inv, idx, x, y));
            }
        }
        return ContainerGrid.parse(slots);
    }
}
