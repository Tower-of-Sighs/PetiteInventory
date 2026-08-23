package com.sighs.petiteinventory.inventory;

import com.sighs.petiteinventory.core.ItemSize;
import com.sighs.petiteinventory.spi.GridSlotPort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Loader-neutral rectangular grid and footprint placement algorithms. */
public class ContainerGrid<S, T> {
    private final GridSlotPort<S, T> port;
    private Set<Cell<S, T>> cells = new HashSet<>();

    public ContainerGrid(GridSlotPort<S, T> port) {
        if (port == null) throw new IllegalArgumentException("grid slot port is required");
        this.port = port;
    }

    public ContainerGrid<S, T> parseSlots(Collection<T> slots) {
        cells.clear();
        if (slots == null || slots.isEmpty()) return this;
        Set<Integer> xCoordinates = new TreeSet<>();
        Set<Integer> yCoordinates = new TreeSet<>();
        for (T slot : slots) {
            xCoordinates.add(port.x(slot));
            yCoordinates.add(port.y(slot));
        }
        List<Integer> sortedX = new ArrayList<>(xCoordinates);
        List<Integer> sortedY = new ArrayList<>(yCoordinates);
        Map<Integer, Integer> xToColumn = new HashMap<>();
        Map<Integer, Integer> yToRow = new HashMap<>();
        for (int i = 0; i < sortedX.size(); i++) xToColumn.put(sortedX.get(i), i);
        for (int i = 0; i < sortedY.size(); i++) yToRow.put(sortedY.get(i), i);
        for (T slot : slots) {
            Integer x = xToColumn.get(port.x(slot));
            Integer y = yToRow.get(port.y(slot));
            if (x != null && y != null) cells.add(new Cell<>(y, x, slot));
        }
        return this;
    }

    @SafeVarargs
    public final ContainerGrid<S, T> parseLayers(Collection<T>... layers) {
        cells.clear();
        int lineStart = 0;
        if (layers == null) return this;
        for (Collection<T> layer : layers) {
            if (layer == null || layer.isEmpty()) continue;
            ContainerGrid<S, T> parsed = new ContainerGrid<>(port).parseSlots(layer);
            int offset = lineStart;
            for (Cell<S, T> cell : parsed.getCells()) {
                lineStart = Math.max(lineStart, cell.y);
                cells.add(new Cell<>(cell.y + offset, cell.x, cell.slot));
            }
            lineStart++;
        }
        return this;
    }

    public int getWidth() {
        Set<Integer> values = new HashSet<>();
        for (Cell<S, T> cell : cells) values.add(cell.x);
        return values.size();
    }

    public int getHeight() {
        Set<Integer> values = new HashSet<>();
        for (Cell<S, T> cell : cells) values.add(cell.y);
        return values.size();
    }

    public Set<Cell<S, T>> getCells() { return cells; }

    public Set<Cell<S, T>> getCells(Predicate<Cell<S, T>> predicate) {
        Set<Cell<S, T>> result = new HashSet<>();
        for (Cell<S, T> cell : cells) if (predicate.test(cell)) result.add(cell);
        return result;
    }

    public Set<Cell<S, T>> getCells(Cell<S, T> cell, Area<?> area) {
        if (cell == null || area == null) return new HashSet<>();
        return getCells(candidate -> candidate.x >= cell.x
                && candidate.x < cell.x + area.width()
                && candidate.y >= cell.y
                && candidate.y < cell.y + area.height());
    }

    public Cell<S, T> getCell(int x, int y) {
        for (Cell<S, T> cell : cells) if (cell.x == x && cell.y == y) return cell;
        return null;
    }

    public Cell<S, T> getCell(T slot) {
        for (Cell<S, T> cell : cells) if (cell.slot == slot || cell.slot.equals(slot)) return cell;
        return null;
    }

    public boolean isEmpty(Cell<S, T> cell, Area<?> area) {
        for (Cell<S, T> candidate : getCells(cell, area)) if (!port.isEmpty(candidate.slot)) return false;
        return true;
    }

    public boolean isEmpty(int x, int y) {
        Cell<S, T> cell = getCell(x, y);
        return cell != null && port.isEmpty(cell.slot);
    }

    public boolean isEmpty(int[] first, int[] second) {
        int minX = Math.min(first[0], second[0]);
        int maxX = Math.max(first[0], second[0]);
        int minY = Math.min(first[1], second[1]);
        int maxY = Math.max(first[1], second[1]);
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) {
            if (!isEmpty(x, y)) return false;
        }
        return true;
    }

    public Map<Cell<S, T>, Cell<S, T>> getCellMap() {
        Map<Cell<S, T>, Cell<S, T>> result = new HashMap<>();
        for (Cell<S, T> cell : cells) {
            if (port.isEmpty(cell.slot)) continue;
            ItemSize size = port.footprint(port.stack(cell.slot));
            for (int x = 0; x < size.width(); x++) for (int y = 0; y < size.height(); y++) {
                Cell<S, T> covered = getCell(cell.x + x, cell.y + y);
                if (covered != null) result.put(covered, cell);
            }
        }
        return result;
    }

    public Cell<S, T> findArea(Area<?> area) { return findArea(area, true); }

    public Cell<S, T> findArea(Area<?> area, boolean requireSameContainer) {
        List<Cell<S, T>> ordered = orderedCells();
        Map<Cell<S, T>, Cell<S, T>> occupied = getCellMap();
        int size = area.width() * area.height();
        for (Cell<S, T> anchor : ordered) {
            Set<Cell<S, T>> candidate = getCells(anchor, area);
            if (candidate.size() < size) continue;
            boolean valid = true;
            for (Cell<S, T> cell : candidate) {
                if (!port.isEmpty(cell.slot) || occupied.containsKey(cell)
                        || (requireSameContainer && !sameContainer(cell, anchor))) {
                    valid = false;
                    break;
                }
            }
            if (valid) return anchor;
        }
        return null;
    }

    public Cell<S, T> findAreaByFootprint(Area<?> area, boolean requireSameContainer) {
        List<Cell<S, T>> ordered = orderedCells();
        for (Cell<S, T> candidate : ordered) {
            Set<Cell<S, T>> candidateCells = getCells(candidate, area);
            if (candidateCells.size() != area.width() * area.height()) continue;
            boolean valid = true;
            for (Cell<S, T> cell : candidateCells) {
                if (!port.isEmpty(cell.slot) || (requireSameContainer && !sameContainer(cell, candidate))) {
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;
            for (Cell<S, T> occupied : cells) {
                if (port.isEmpty(occupied.slot)) continue;
                ItemSize occupiedSize = port.footprint(port.stack(occupied.slot));
                if (candidate.x < occupied.x + occupiedSize.width()
                        && candidate.x + area.width() > occupied.x
                        && candidate.y < occupied.y + occupiedSize.height()
                        && candidate.y + area.height() > occupied.y) {
                    valid = false;
                    break;
                }
            }
            if (valid) return candidate;
        }
        return null;
    }

    public Cell<S, T> findAreaBySlotOrder(Area<?> area) { return findAreaBySlotOrder(area, getWidth()); }

    public Cell<S, T> findAreaBySlotOrder(Area<?> area, int width) {
        List<Cell<S, T>> ordered = orderedCells();
        if (width <= 0) return null;
        for (int anchorIndex = 0; anchorIndex < ordered.size(); anchorIndex++) {
            int row = anchorIndex / width;
            int column = anchorIndex % width;
            if (column + area.width() > width) continue;
            boolean valid = true;
            for (int y = 0; y < area.height() && valid; y++) for (int x = 0; x < area.width(); x++) {
                int index = (row + y) * width + column + x;
                if (index >= ordered.size() || !port.isEmpty(ordered.get(index).slot)) {
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;
            for (int occupiedIndex = 0; occupiedIndex < ordered.size(); occupiedIndex++) {
                Cell<S, T> occupied = ordered.get(occupiedIndex);
                if (port.isEmpty(occupied.slot)) continue;
                int occupiedRow = occupiedIndex / width;
                int occupiedColumn = occupiedIndex % width;
                ItemSize occupiedSize = port.footprint(port.stack(occupied.slot));
                if (column < occupiedColumn + occupiedSize.width()
                        && column + area.width() > occupiedColumn
                        && row < occupiedRow + occupiedSize.height()
                        && row + area.height() > occupiedRow) {
                    valid = false;
                    break;
                }
            }
            if (valid) return ordered.get(anchorIndex);
        }
        return null;
    }

    public List<Cell<S, T>> getCellsBySlotOrder(Cell<S, T> anchor, Area<?> area, int width) {
        List<Cell<S, T>> ordered = orderedCells();
        int anchorIndex = ordered.indexOf(anchor);
        if (anchorIndex < 0 || width <= 0) return new ArrayList<>();
        int row = anchorIndex / width;
        int column = anchorIndex % width;
        if (column + area.width() > width) return new ArrayList<>();
        List<Cell<S, T>> result = new ArrayList<>(area.width() * area.height());
        for (int y = 0; y < area.height(); y++) for (int x = 0; x < area.width(); x++) {
            int index = (row + y) * width + column + x;
            if (index < 0 || index >= ordered.size()) return new ArrayList<>();
            result.add(ordered.get(index));
        }
        return result;
    }

    public void removeRow(int row) {
        List<T> remaining = new ArrayList<>();
        for (Cell<S, T> cell : cells) if (cell.y != row) remaining.add(cell.slot);
        parseSlots(remaining);
    }

    public void removeColumn(int column) {
        List<T> remaining = new ArrayList<>();
        for (Cell<S, T> cell : cells) if (cell.x != column) remaining.add(cell.slot);
        parseSlots(remaining);
    }

    private List<Cell<S, T>> orderedCells() {
        List<Cell<S, T>> ordered = new ArrayList<>(cells);
        ordered.sort(Comparator.comparingInt(cell -> port.index(cell.slot)));
        return ordered;
    }

    private boolean sameContainer(Cell<S, T> left, Cell<S, T> right) {
        Object leftContainer = port.container(left.slot);
        Object rightContainer = port.container(right.slot);
        return leftContainer == rightContainer || (leftContainer != null && leftContainer.equals(rightContainer));
    }

    public static final class Cell<S, T> {
        public final int y;
        public final int x;
        public final T slot;

        public Cell(int y, int x, T slot) {
            this.y = y;
            this.x = x;
            this.slot = slot;
        }

        public T slot() { return slot; }
        public int x() { return x; }
        public int y() { return y; }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Cell)) return false;
            Cell<?, ?> cell = (Cell<?, ?>) other;
            return x == cell.x && y == cell.y && (slot == cell.slot || (slot != null && slot.equals(cell.slot)));
        }

        @Override
        public int hashCode() { return 31 * (31 * x + y) + (slot == null ? 0 : slot.hashCode()); }

        @Override
        public String toString() { return "(" + x + "," + y + ")[" + slot + "]"; }
    }
}
