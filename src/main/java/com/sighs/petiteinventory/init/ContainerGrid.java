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

    public static record Cell(int y, int x, Slot slot) {
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
    }
}
