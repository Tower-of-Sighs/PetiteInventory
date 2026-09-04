package com.sighs.petiteinventory.platform.inventory;

import com.sighs.petiteinventory.core.ItemSize;
import com.sighs.petiteinventory.spi.GridSlotPort;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Forge Slot adapter for the common grid and placement algorithms. */
public final class ContainerGrid {
    private static final GridSlotPort<ItemStack, Slot> PORT = new GridSlotPort<ItemStack, Slot>() {
        @Override public int x(Slot slot) { return slot.x; }
        @Override public int y(Slot slot) { return slot.y; }
        @Override public int index(Slot slot) { return slot.index; }
        @Override public Object container(Slot slot) { return slot.container; }
        @Override public boolean isEmpty(Slot slot) { return !slot.hasItem(); }
        @Override public ItemStack stack(Slot slot) { return slot.getItem(); }
        @Override public ItemSize footprint(ItemStack stack) {
            return ItemInventoryService.getArea(stack).size();
        }
    };

    private final com.sighs.petiteinventory.inventory.ContainerGrid<ItemStack, Slot> common;

    public ContainerGrid() {
        common = new com.sighs.petiteinventory.inventory.ContainerGrid<>(PORT);
    }

    public static ContainerGrid parse(Collection<Slot> slots) {
        ContainerGrid grid = new ContainerGrid();
        grid.common.parseSlots(slots);
        return grid;
    }

    @SafeVarargs
    public static ContainerGrid parse(Collection<Slot>... layers) {
        ContainerGrid grid = new ContainerGrid();
        grid.common.parseLayers(layers);
        return grid;
    }

    public int getWidth() { return common.getWidth(); }
    public int getHeight() { return common.getHeight(); }

    public Set<Cell> getCells() { return wrap(common.getCells()); }

    public Set<Cell> getCells(Predicate<Cell> predicate) {
        Set<Cell> result = new HashSet<>();
        for (Cell cell : getCells()) if (predicate.test(cell)) result.add(cell);
        return result;
    }

    public Set<Cell> getCells(Cell cell, com.sighs.petiteinventory.inventory.Area<?> area) {
        return wrap(common.getCells(cell.delegate, area));
    }

    public Cell getCell(int x, int y) {
        return wrap(common.getCell(x, y));
    }

    public Cell getCell(Slot slot) {
        return wrap(common.getCell(slot));
    }

    public boolean isEmpty(Cell cell, com.sighs.petiteinventory.inventory.Area<?> area) {
        return common.isEmpty(cell.delegate, area);
    }

    public boolean isEmpty(int x, int y) { return common.isEmpty(x, y); }
    public boolean isEmpty(int[] first, int[] second) { return common.isEmpty(first, second); }

    public Map<Cell, Cell> getCellMap() {
        Map<Cell, Cell> result = new HashMap<>();
        for (Map.Entry<com.sighs.petiteinventory.inventory.ContainerGrid.Cell<ItemStack, Slot>,
                com.sighs.petiteinventory.inventory.ContainerGrid.Cell<ItemStack, Slot>> entry
                : common.getCellMap().entrySet()) {
            result.put(new Cell(entry.getKey()), new Cell(entry.getValue()));
        }
        return result;
    }

    public Cell findArea(com.sighs.petiteinventory.inventory.Area<?> area) {
        return wrap(common.findArea(area));
    }

    public Cell findArea(com.sighs.petiteinventory.inventory.Area<?> area, boolean sameContainer) {
        return wrap(common.findArea(area, sameContainer));
    }

    public Cell findAreaByFootprint(com.sighs.petiteinventory.inventory.Area<?> area, boolean sameContainer) {
        return wrap(common.findAreaByFootprint(area, sameContainer));
    }

    public Cell findAreaBySlotOrder(com.sighs.petiteinventory.inventory.Area<?> area) {
        return wrap(common.findAreaBySlotOrder(area));
    }

    public Cell findAreaBySlotOrder(com.sighs.petiteinventory.inventory.Area<?> area, int width) {
        return wrap(common.findAreaBySlotOrder(area, width));
    }

    public List<Cell> getCellsBySlotOrder(Cell anchor,
                                          com.sighs.petiteinventory.inventory.Area<?> area, int width) {
        List<Cell> result = new ArrayList<>();
        for (com.sighs.petiteinventory.inventory.ContainerGrid.Cell<ItemStack, Slot> cell
                : common.getCellsBySlotOrder(anchor.delegate, area, width)) {
            result.add(new Cell(cell));
        }
        return result;
    }

    public void removeRow(int row) { common.removeRow(row); }
    public void removeColumn(int column) { common.removeColumn(column); }

    private Set<Cell> wrap(Collection<com.sighs.petiteinventory.inventory.ContainerGrid.Cell<ItemStack, Slot>> source) {
        Set<Cell> result = new HashSet<>();
        for (com.sighs.petiteinventory.inventory.ContainerGrid.Cell<ItemStack, Slot> cell : source) {
            result.add(new Cell(cell));
        }
        return result;
    }

    private Cell wrap(com.sighs.petiteinventory.inventory.ContainerGrid.Cell<ItemStack, Slot> cell) {
        return cell == null ? null : new Cell(cell);
    }

    public static final class Cell {
        private final com.sighs.petiteinventory.inventory.ContainerGrid.Cell<ItemStack, Slot> delegate;

        private Cell(com.sighs.petiteinventory.inventory.ContainerGrid.Cell<ItemStack, Slot> delegate) {
            this.delegate = delegate;
        }

        public int x() { return delegate.x(); }
        public int y() { return delegate.y(); }
        public Slot slot() { return delegate.slot(); }
        public boolean isEmpty() { return !slot().hasItem(); }

        @Override public boolean equals(Object other) {
            return other instanceof Cell && delegate.equals(((Cell) other).delegate);
        }

        @Override public int hashCode() { return delegate.hashCode(); }
        @Override public String toString() { return delegate.toString(); }
    }
}
