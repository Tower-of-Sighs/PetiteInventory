package com.sighs.petiteinventory.spi;

/** Loader-neutral operations required by stack transfer algorithms. */
public interface SlotPort<S, T> {
    S stack(T slot);

    default boolean canPlace(T slot, S stack) { return true; }

    int maxStackSize(T slot);

    void changed(T slot);
}
