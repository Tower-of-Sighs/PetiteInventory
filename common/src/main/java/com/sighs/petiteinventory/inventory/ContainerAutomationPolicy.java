package com.sighs.petiteinventory.inventory;

import com.sighs.petiteinventory.spi.GridSlotPort;
import com.sighs.petiteinventory.spi.SlotPort;
import com.sighs.petiteinventory.spi.StackPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Shared footprint-aware insertion algorithm for hoppers and automation. */
public final class ContainerAutomationPolicy<C, S, T, D> {
    public interface Port<C, S, T, D> {
        List<T> slots(C target);
        GridSlotPort<S, T> gridSlots();
        SlotPort<S, T> slotPort();
        StackPort<S> stacks();
        boolean canPlace(C target, T slot, S stack, D direction);
        void set(C target, T anchor, S stack);
        void changed(C target);
        int width(C target, List<T> slots);
    }

    private final Port<C, S, T, D> port;

    public ContainerAutomationPolicy(Port<C, S, T, D> port) {
        if (port == null) throw new IllegalArgumentException("automation port is required");
        this.port = port;
    }

    /** Returns {@code null} for vanilla delegation, otherwise the remainder. */
    public S insert(C target, S incoming, D direction) {
        if (target == null || incoming == null || port.stacks().isEmpty(incoming)) return null;
        StackPort<S> stacks = port.stacks();
        if (stacks.footprint(incoming).width() == 1 && stacks.footprint(incoming).height() == 1) return null;
        List<T> slots = port.slots(target);
        if (slots == null || slots.isEmpty()) return incoming;

        stackExisting(target, slots, incoming, direction);
        if (stacks.isEmpty(incoming)) return incoming;

        int width = port.width(target, slots);
        T anchor = find(target, slots, incoming, width, direction);
        if (anchor == null) {
            boolean rotated = stacks.isRotated(incoming);
            stacks.setRotated(incoming, !rotated);
            anchor = find(target, slots, incoming, width, direction);
            if (anchor == null) {
                stacks.setRotated(incoming, rotated);
                return incoming;
            }
        }
        port.set(target, anchor, stacks.copy(incoming));
        port.changed(target);
        stacks.setCount(incoming, 0);
        return incoming;
    }

    private void stackExisting(C target, List<T> slots, S incoming, D direction) {
        StackPort<S> stacks = port.stacks();
        SlotPort<S, T> slotPort = port.slotPort();
        for (T slot : slots) {
            S existing = slotPort.stack(slot);
            if (stacks.isEmpty(existing) || !stacks.sameItemIgnoringRotation(existing, incoming)
                    || !port.canPlace(target, slot, incoming, direction)) continue;
            int maximum = Math.min(slotPort.maxStackSize(slot), stacks.maxStackSize(incoming));
            int amount = Math.min(stacks.count(incoming), maximum - stacks.count(existing));
            if (amount <= 0) continue;
            stacks.grow(existing, amount);
            stacks.shrink(incoming, amount);
            slotPort.changed(slot);
            if (stacks.isEmpty(incoming)) return;
        }
    }

    private T find(C target, List<T> slots, S incoming, int width, D direction) {
        if (width <= 0) return null;
        ContainerGrid<S, T> grid = new ContainerGrid<>(port.gridSlots()).parseSlots(slots);
        List<ContainerGrid.Cell<S, T>> ordered = new ArrayList<>(grid.getCells());
        ordered.sort(Comparator.comparingInt(cell -> port.gridSlots().index(cell.slot())));
        Map<ContainerGrid.Cell<S, T>, ContainerGrid.Cell<S, T>> occupied = grid.getCellMap();
        Area<?> area = new Area<>(port.stacks().footprint(incoming).width(),
                port.stacks().footprint(incoming).height(), incoming);
        for (ContainerGrid.Cell<S, T> anchor : ordered) {
            List<ContainerGrid.Cell<S, T>> cells = grid.getCellsBySlotOrder(anchor, area, width);
            if (cells.size() != area.width() * area.height()) continue;
            boolean valid = true;
            for (ContainerGrid.Cell<S, T> cell : cells) {
                if (!port.gridSlots().isEmpty(cell.slot()) || occupied.containsKey(cell)
                        || !port.canPlace(target, cell.slot(), incoming, direction)) {
                    valid = false;
                    break;
                }
            }
            if (valid) return anchor.slot();
        }
        return null;
    }
}
