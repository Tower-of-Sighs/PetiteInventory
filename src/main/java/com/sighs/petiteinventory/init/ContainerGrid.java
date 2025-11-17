package com.sighs.petiteinventory.init;

import net.minecraft.world.inventory.Slot;

import java.util.*;
import java.util.function.Predicate;

public class ContainerGrid {
    private Set<Cell> cells = new HashSet<>();

    public static ContainerGrid parse(Collection<Slot> slots) {
        ContainerGrid grid = new ContainerGrid();
        if (slots == null || slots.isEmpty()) {
            return grid;
        }

        // 找出所有可能的x坐标和y坐标，用于确定网格结构
        Set<Integer> xCoords = new TreeSet<>();
        Set<Integer> yCoords = new TreeSet<>();

        for (Slot slot : slots) {
            xCoords.add(slot.x);
            yCoords.add(slot.y);
        }

        // 将坐标排序并映射到行列索引
        List<Integer> sortedX = new ArrayList<>(xCoords);
        List<Integer> sortedY = new ArrayList<>(yCoords);

        // 创建坐标到行列索引的映射
        Map<Integer, Integer> xToColumn = new HashMap<>();
        Map<Integer, Integer> yToRow = new HashMap<>();

        for (int i = 0; i < sortedX.size(); i++) {
            xToColumn.put(sortedX.get(i), i);
        }
        for (int i = 0; i < sortedY.size(); i++) {
            yToRow.put(sortedY.get(i), i);
        }

        // 将每个槽位转换为Cell并添加到网格中
        for (Slot slot : slots) {
            Integer column = xToColumn.get(slot.x);
            Integer row = yToRow.get(slot.y);

            if (column != null && row != null) {
                grid.cells.add(new Cell(row, column, slot));
            }
        }
        return grid;
    }

    public int getWidth() {
        Set<Integer> set = new HashSet<>();
        for (Cell cell : cells) {
            set.add(cell.x);
        }
        return set.size();
    }
    public int getHeight() {
        Set<Integer> set = new HashSet<>();
        for (Cell cell : cells) {
            set.add(cell.y);
        }
        return set.size();
    }

    public Set<Cell> getCells() {
        return cells;
    }
    public Set<Cell> getCells(Predicate<Cell> predicate) {
        Set<Cell> result = new HashSet<>();
        for (Cell cell : cells) {
            if (predicate.test(cell)) result.add(cell);
        }
        return result;
    }
    public Set<Cell> getCells(Cell cell, Area area) {
        int[] xRange = new int[] {cell.x, cell.x + area.width() - 1};
        int[] yRange = new int[] {cell.y, cell.y + area.height() - 1};
        return getCells(c -> c.inRangeX(xRange) && c.inRangeY(yRange));
    }

    public Cell getCell(int x, int y) {
        for (Cell cell : cells) {
            if (cell.x == x && cell.y == y) return cell;
        }
        return null;
    }
    public Cell getCell(Slot slot) {
        for (Cell cell : cells) {
            if (cell.slot.equals(slot)) return cell;
        }
        return null;
    }

    public boolean isEmpty(Cell cell, Area area) {
        boolean result = true;
        for (Cell c : getCells(cell, area)) {
            if (!c.isEmpty()) result = false;
        }
        return result;
    }
    public boolean isEmpty(int x, int y) {
        Cell cell = getCell(x, y);
        return cell != null && cell.isEmpty();
    }
    public boolean isEmpty(int[] p1, int[] p2) {
        int[] rangeX = {Math.min(p1[0], p2[0]), Math.max(p1[0], p2[0])};
        int[] rangeY = {Math.min(p1[1], p2[1]), Math.max(p1[1], p2[1])};
        boolean result = true;
        for (int x = rangeX[0]; x <= rangeX[1]; x++) {
            for (int y = rangeX[0]; y <= rangeY[1]; y++) {
                if (!isEmpty(x, y)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    public Map<Cell, Cell> getCellMap() {
        Map<Cell, Cell> result = new HashMap<>();
        for (Cell cell : cells) {
            if (!cell.isEmpty()) {
                Area area = Area.of(cell.slot.getItem());
                for (int x = 0; x < area.width(); x++) for (int y = 0; y < area.height(); y++) {
                    result.put(getCell(cell.x + x, cell.y + y), cell);
                }
            }
        }
        return result;
    }

    public Cell findArea(Area area) {
        int size = area.width() * area.height();
        var cellMap = getCellMap();
        for (Cell cell : cells) {
            var cells = getCells(cell, area);
            if (size > cells.size()) continue;
            boolean valid = true;
            for (Cell c : cells) {
                if (!c.isEmpty() || cellMap.containsKey(c)) valid = false;
            }
            if (valid) return cell;
        }
        return null;
    }

    public void removeRow(int row) {
        List<Slot> slots = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.y != row) slots.add(cell.slot);
        }
        cells = parse(slots).cells;
    }
    public void removeColumn(int column) {
        List<Slot> slots = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.x != column) slots.add(cell.slot);
        }
        cells = parse(slots).cells;
    }

    /**
     * 查找能容纳指定Area的完全空白区域，返回最左上角的Cell
     * @param area 要放置的区域大小
     * @return 符合条件的区域左上角Cell，如果找不到返回null
     */
    public Cell findEmptyArea(Area area) {
        int requiredWidth = area.width();
        int requiredHeight = area.height();

        // 如果需求区域比网格还大，直接返回null
        if (requiredWidth > getWidth() || requiredHeight > getHeight()) {
            return null;
        }

        // 获取单元格占用映射，用于检查单元格是否被区域占用
        Map<Cell, Cell> cellMap = getCellMap();

        // 遍历所有可能的起始位置
        for (int startY = 0; startY <= getHeight() - requiredHeight; startY++) {
            for (int startX = 0; startX <= getWidth() - requiredWidth; startX++) {

                // 检查以(startX, startY)为左上角的区域是否完全空白
                if (isAreaEmpty(startX, startY, requiredWidth, requiredHeight, cellMap)) {
                    return getCell(startX, startY);
                }
            }
        }

        return null; // 没有找到合适的区域
    }

    /**
     * 检查指定矩形区域是否完全空白
     * @param startX 区域起始X坐标
     * @param startY 区域起始Y坐标
     * @param width 区域宽度
     * @param height 区域高度
     * @param cellMap 单元格占用映射
     * @return 如果区域完全空白返回true，否则返回false
     */
    private boolean isAreaEmpty(int startX, int startY, int width, int height, Map<Cell, Cell> cellMap) {
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                Cell cell = getCell(x, y);

                // 检查单元格是否存在、是否为空、是否被占用
                if (cell == null || !cell.isEmpty() || cellMap.containsKey(cell)) {
                    return false;
                }
            }
        }
        return true;
    }

    public record Cell(int y, int x, Slot slot) {
        public boolean isEmpty() {
            return !slot().hasItem();
        }

        private boolean inRangeX(int[] range) {
            if (range.length != 2) return false;
            return x >= range[0] && x <= range[1];
        }
        private boolean inRangeY(int[] range) {
            if (range.length != 2) return false;
            return y >= range[0] && y <= range[1];
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")[" + slot.getItem() + "]";
        }

        @Override
        public boolean equals(Object object) {
            return this.toString().equals(object.toString());
        }
    }
}
