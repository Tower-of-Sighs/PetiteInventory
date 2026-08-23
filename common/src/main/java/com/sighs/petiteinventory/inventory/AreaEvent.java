package com.sighs.petiteinventory.inventory;

import com.sighs.petiteinventory.event.InternalEvent;

/** Mutable footprint event shared by item-rule integrations. */
public class AreaEvent<S> implements InternalEvent {
    public int width;
    public int height;
    public S itemStack;

    public AreaEvent(int width, int height, S itemStack) {
        this.width = width;
        this.height = height;
        this.itemStack = itemStack;
    }
}
