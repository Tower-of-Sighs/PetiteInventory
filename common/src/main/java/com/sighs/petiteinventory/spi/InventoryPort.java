package com.sighs.petiteinventory.spi;

import com.sighs.petiteinventory.core.ItemSize;

/** Platform operations used by reusable inventory services. */
public interface InventoryPort<P, S> {
    StackPort<S> stacks();

    int size(P player);

    S get(P player, int slot);

    void set(P player, int slot, S stack);

    int maxStackSize(P player);

    /** Returns a slot that can hold the whole footprint, or {@code -1}. */
    int findSlotFor(P player, ItemSize footprint);

    /** Fallback for a normal one-slot stack when the policy defers to vanilla. */
    boolean placeFallback(P player, S stack);

    void drop(P player, S stack);
}
