package com.sighs.petiteinventory.inventory;

import com.sighs.petiteinventory.spi.SlotPort;
import com.sighs.petiteinventory.spi.StackPort;

import java.util.List;

/** Shared stacking policy used by menu, hopper and inventory transfer adapters. */
public final class ContainerStackingService<S, T> {
    private final StackPort<S> stacks;
    private final SlotPort<S, T> slots;

    public ContainerStackingService(StackPort<S> stacks, SlotPort<S, T> slots) {
        if (stacks == null || slots == null) {
            throw new IllegalArgumentException("stack and slot ports are required");
        }
        this.stacks = stacks;
        this.slots = slots;
    }

    public boolean stackIntoExisting(S source, List<T> targets) {
        if (source == null || stacks.isEmpty(source) || !stacks.isStackable(source) || targets == null) {
            return false;
        }

        boolean changed = false;
        for (T target : targets) {
            if (!slots.canPlace(target, source)) continue;
            S targetStack = slots.stack(target);
            if (targetStack == null || stacks.isEmpty(targetStack)
                    || !stacks.sameItemIgnoringRotation(targetStack, source)) {
                continue;
            }
            changed |= stack(source, targetStack, target);
            if (stacks.isEmpty(source)) {
                return true;
            }
        }
        return changed;
    }

    public boolean stack(S source, S target, T targetSlot) {
        int maximum = Math.min(slots.maxStackSize(targetSlot), stacks.maxStackSize(source));
        int amount = Math.min(stacks.count(source), maximum - stacks.count(target));
        if (amount <= 0) {
            return false;
        }
        stacks.grow(target, amount);
        stacks.shrink(source, amount);
        slots.changed(targetSlot);
        return true;
    }
}
