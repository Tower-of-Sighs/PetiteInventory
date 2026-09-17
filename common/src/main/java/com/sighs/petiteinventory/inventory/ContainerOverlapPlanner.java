package com.sighs.petiteinventory.inventory;

import com.sighs.petiteinventory.core.ItemSize;
import com.sighs.petiteinventory.spi.GridSlotPort;
import com.sighs.petiteinventory.spi.StackPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plans a safe rearrangement for menus whose initial contents can overlap.
 *
 * <p>The first attempt only moves overlapping items into directly free space.
 * If the total footprint still fits but no direct space is available, the
 * planner attempts to repack the whole container. When that also fails, the
 * original overlapping layout is kept.</p>
 */
public final class ContainerOverlapPlanner<S, T> {
    public interface Port<S, T> {
        GridSlotPort<S, T> gridSlots();
        StackPort<S> stacks();
        boolean sameContainer(T left, T right);
        boolean canMove(T slot, S stack);
    }

    public static final class Move<S, T> {
        public final T from;
        public final T to;
        public final S stack;

        private Move(T from, T to, S stack) {
            this.from = from;
            this.to = to;
            this.stack = stack;
        }
    }

    public List<Move<S, T>> plan(List<T> slots, Port<S, T> port) {
        if (slots == null || slots.isEmpty() || port == null || port.gridSlots() == null) {
            return new ArrayList<>();
        }

        ContainerGrid<S, T> grid = new ContainerGrid<>(port.gridSlots()).parseSlots(slots);
        if (grid.getCells().isEmpty()) {
            return new ArrayList<>();
        }

        Map<Object, List<Occupant>> groups = new LinkedHashMap<>();
        int order = 0;
        for (T slot : slots) {
            S stack = port.gridSlots().stack(slot);
            if (stack == null || port.stacks().isEmpty(stack)) {
                continue;
            }
            ContainerGrid.Cell<S, T> cell = grid.getCell(slot);
            if (cell == null) {
                continue;
            }
            ItemSize size = port.gridSlots().footprint(stack);
            List<ContainerGrid.Cell<S, T>> covered = coveredCells(grid, cell, size);
            if (covered.isEmpty()) {
                continue;
            }
            Object key = port.gridSlots().container(slot);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(new Occupant(slot, size, stack, order++, covered));
        }

        List<Move<S, T>> allMoves = new ArrayList<>();
        for (List<Occupant> group : groups.values()) {
            allMoves.addAll(planGroup(group, grid, port));
        }
        return allMoves;
    }

    private List<Move<S, T>> planGroup(List<Occupant> group,
                                       ContainerGrid<S, T> grid, Port<S, T> port) {
        List<Occupant> occupants = new ArrayList<>(group);
        occupants.sort(Comparator.comparingInt(occupant -> occupant.order));

        boolean anyOverlap = false;
        for (Occupant occupant : occupants) {
            occupant.overlaps = hasOverlap(occupant, occupants);
            anyOverlap |= occupant.overlaps;
        }
        if (!anyOverlap) {
            return new ArrayList<>();
        }

        List<Occupant> overlapping = new ArrayList<>();
        for (Occupant occupant : occupants) {
            if (occupant.overlaps) {
                overlapping.add(occupant);
            }
        }

        List<ContainerGrid.Cell<S, T>> groupCells = groupCells(occupants.get(0).slot, grid, port);
        List<Move<S, T>> simple = planSimple(overlapping, occupants, groupCells, grid, port);
        if (simple != null) {
            return simple;
        }

        if (groupCells.size() < totalArea(occupants)) {
            return new ArrayList<>();
        }
        return planComplex(occupants, groupCells, grid, port);
    }

    private List<Move<S, T>> planSimple(List<Occupant> overlapping,
                                        List<Occupant> allOccupants,
                                        List<ContainerGrid.Cell<S, T>> groupCells,
                                        ContainerGrid<S, T> grid, Port<S, T> port) {
        Map<Occupant, Set<ContainerGrid.Cell<S, T>>> coveredBy = new LinkedHashMap<>();
        for (Occupant occupant : allOccupants) {
            coveredBy.put(occupant, new HashSet<>(occupant.covered));
        }
        List<Move<S, T>> moves = new ArrayList<>();

        for (Occupant occupant : overlapping) {
            Set<ContainerGrid.Cell<S, T>> free = new HashSet<>(groupCells);
            for (Map.Entry<Occupant, Set<ContainerGrid.Cell<S, T>>> entry : coveredBy.entrySet()) {
                if (entry.getKey() != occupant) {
                    free.removeAll(entry.getValue());
                }
            }
            Placement placement = findPlacement(occupant, free, grid, port);
            if (placement == null) {
                return null;
            }
            moves.add(placement.move(occupant));
            coveredBy.put(occupant, new HashSet<>(placement.cells));
        }
        return moves;
    }

    private List<Move<S, T>> planComplex(List<Occupant> occupants,
                                         List<ContainerGrid.Cell<S, T>> groupCells,
                                         ContainerGrid<S, T> grid, Port<S, T> port) {
        List<Occupant> sorted = new ArrayList<>(occupants);
        sorted.sort(Comparator.comparingInt((Occupant occupant) -> occupant.size.area())
                .reversed()
                .thenComparingInt(occupant -> occupant.order));

        Set<ContainerGrid.Cell<S, T>> available = new HashSet<>(groupCells);
        List<Move<S, T>> moves = new ArrayList<>();

        for (Occupant occupant : sorted) {
            Placement placement = findPlacement(occupant, available, grid, port);
            if (placement == null) {
                return new ArrayList<>();
            }
            moves.add(placement.move(occupant));
            available.removeAll(placement.cells);
        }
        return moves;
    }

    private Placement findPlacement(Occupant occupant, Set<ContainerGrid.Cell<S, T>> available,
                                    ContainerGrid<S, T> grid, Port<S, T> port) {
        List<ContainerGrid.Cell<S, T>> ordered = orderedCells(grid, port);
        for (ContainerGrid.Cell<S, T> anchor : ordered) {
            if (!port.sameContainer(occupant.slot, anchor.slot())) {
                continue;
            }

            List<ContainerGrid.Cell<S, T>> cells = cellsAt(grid, anchor, occupant.size);
            if (cells != null && fits(cells, available, occupant.slot, occupant.stack, port)) {
                return new Placement(anchor, cells, occupant.stack, false);
            }
        }

        if (occupant.size.width() == occupant.size.height()) {
            return null;
        }

        ItemSize rotatedSize = occupant.size.rotated();
        S rotatedStack = port.stacks().copy(occupant.stack);
        port.stacks().setRotated(rotatedStack, true);
        for (ContainerGrid.Cell<S, T> anchor : ordered) {
            if (!port.sameContainer(occupant.slot, anchor.slot())) {
                continue;
            }
            List<ContainerGrid.Cell<S, T>> cells = cellsAt(grid, anchor, rotatedSize);
            if (cells != null && fits(cells, available, occupant.slot, rotatedStack, port)) {
                return new Placement(anchor, cells, rotatedStack, true);
            }
        }
        return null;
    }

    private boolean fits(List<ContainerGrid.Cell<S, T>> cells,
                         Set<ContainerGrid.Cell<S, T>> available,
                         T owner, S stack, Port<S, T> port) {
        for (ContainerGrid.Cell<S, T> cell : cells) {
            if (!available.contains(cell)
                    || !port.sameContainer(owner, cell.slot())
                    || !port.canMove(cell.slot(), stack)) {
                return false;
            }
        }
        return true;
    }

    private List<ContainerGrid.Cell<S, T>> cellsAt(ContainerGrid<S, T> grid,
                                                    ContainerGrid.Cell<S, T> anchor,
                                                    ItemSize size) {
        List<ContainerGrid.Cell<S, T>> result = new ArrayList<>(size.width() * size.height());
        for (int y = 0; y < size.height(); y++) {
            for (int x = 0; x < size.width(); x++) {
                ContainerGrid.Cell<S, T> cell = grid.getCell(anchor.x + x, anchor.y + y);
                if (cell == null) {
                    return null;
                }
                result.add(cell);
            }
        }
        return result;
    }

    private List<ContainerGrid.Cell<S, T>> orderedCells(ContainerGrid<S, T> grid,
                                                        Port<S, T> port) {
        List<ContainerGrid.Cell<S, T>> ordered = new ArrayList<>(grid.getCells());
        ordered.sort(Comparator.comparingInt(cell -> port.gridSlots().index(cell.slot())));
        return ordered;
    }

    private List<ContainerGrid.Cell<S, T>> groupCells(T representative,
                                                      ContainerGrid<S, T> grid,
                                                      Port<S, T> port) {
        List<ContainerGrid.Cell<S, T>> result = new ArrayList<>();
        for (ContainerGrid.Cell<S, T> cell : grid.getCells()) {
            if (port.sameContainer(representative, cell.slot())) {
                result.add(cell);
            }
        }
        return result;
    }

    private Set<ContainerGrid.Cell<S, T>> unionCovered(List<Occupant> occupants) {
        Set<ContainerGrid.Cell<S, T>> result = new HashSet<>();
        for (Occupant occupant : occupants) {
            result.addAll(occupant.covered);
        }
        return result;
    }

    private boolean hasOverlap(Occupant occupant, List<Occupant> others) {
        for (Occupant other : others) {
            if (other == occupant) {
                continue;
            }
            for (ContainerGrid.Cell<S, T> cell : occupant.covered) {
                if (other.covered.contains(cell)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int totalArea(List<Occupant> occupants) {
        int area = 0;
        for (Occupant occupant : occupants) {
            area += occupant.size.area();
        }
        return area;
    }

    private List<ContainerGrid.Cell<S, T>> coveredCells(ContainerGrid<S, T> grid,
                                                        ContainerGrid.Cell<S, T> cell,
                                                        ItemSize size) {
        List<ContainerGrid.Cell<S, T>> result = new ArrayList<>(size.width() * size.height());
        for (int y = 0; y < size.height(); y++) {
            for (int x = 0; x < size.width(); x++) {
                ContainerGrid.Cell<S, T> covered = grid.getCell(cell.x + x, cell.y + y);
                if (covered != null) {
                    result.add(covered);
                }
            }
        }
        return result;
    }

    private final class Placement {
        private final ContainerGrid.Cell<S, T> anchor;
        private final List<ContainerGrid.Cell<S, T>> cells;
        private final S stack;

        private Placement(ContainerGrid.Cell<S, T> anchor,
                          List<ContainerGrid.Cell<S, T>> cells,
                          S stack,
                          boolean rotated) {
            this.anchor = anchor;
            this.cells = cells;
            this.stack = stack;
        }

        private Move<S, T> move(Occupant occupant) {
            return new Move<>(occupant.slot, anchor.slot(), stack);
        }
    }

    private final class Occupant {
        private final T slot;
        private final ItemSize size;
        private final S stack;
        private final int order;
        private final List<ContainerGrid.Cell<S, T>> covered;
        private boolean overlaps;

        private Occupant(T slot, ItemSize size, S stack, int order,
                         List<ContainerGrid.Cell<S, T>> covered) {
            this.slot = slot;
            this.size = size;
            this.stack = stack;
            this.order = order;
            this.covered = covered;
        }
    }
}
