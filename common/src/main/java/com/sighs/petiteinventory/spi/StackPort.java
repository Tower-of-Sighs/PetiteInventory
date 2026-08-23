package com.sighs.petiteinventory.spi;

import com.sighs.petiteinventory.core.ItemSize;

/**
 * The small platform boundary required by the inventory policy. Implementations
 * translate a loader's ItemStack API into these operations; the policy never
 * imports Minecraft classes.
 */
public interface StackPort<S> {
    boolean isEmpty(S stack);

    boolean isStackable(S stack);

    S copy(S stack);

    int count(S stack);

    void setCount(S stack, int count);

    void shrink(S stack, int amount);

    void grow(S stack, int amount);

    int maxStackSize(S stack);

    boolean sameItemIgnoringRotation(S first, S second);

    boolean isRotated(S stack);

    void setRotated(S stack, boolean rotated);

    ItemSize footprint(S stack);
}
