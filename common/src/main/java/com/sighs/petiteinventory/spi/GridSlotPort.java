package com.sighs.petiteinventory.spi;

import com.sighs.petiteinventory.core.ItemSize;

/** Exposes menu-slot geometry and occupancy without naming a game loader. */
public interface GridSlotPort<S, T> {
    int x(T slot);
    int y(T slot);
    int index(T slot);
    Object container(T slot);
    boolean isEmpty(T slot);
    S stack(T slot);
    ItemSize footprint(S stack);
}
