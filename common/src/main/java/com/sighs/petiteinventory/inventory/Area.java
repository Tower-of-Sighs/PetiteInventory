package com.sighs.petiteinventory.inventory;

import com.sighs.petiteinventory.core.ItemSize;

/** A rectangular item footprint with an optional platform-owned value. */
public final class Area<S> {
    private final int width;
    private final int height;
    private final S itemStack;

    public Area(int width, int height, S itemStack) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Area dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        this.itemStack = itemStack;
    }

    public int width() { return width; }
    public int height() { return height; }
    public S itemStack() { return itemStack; }

    public ItemSize size() {
        return new ItemSize(width, height);
    }

    public int minSize() {
        return Math.min(width, height);
    }

    public int maxSize() {
        return Math.max(width, height);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Area)) return false;
        Area<?> area = (Area<?>) other;
        return width == area.width && height == area.height
                && (itemStack == area.itemStack
                || (itemStack != null && itemStack.equals(area.itemStack)));
    }

    @Override
    public int hashCode() {
        int result = 31 * width + height;
        return 31 * result + (itemStack == null ? 0 : itemStack.hashCode());
    }

    @Override
    public String toString() {
        return "[" + itemStack + "](" + width + "," + height + ")";
    }
}
