package com.sighs.petiteinventory.api;

/** Stable, loader-neutral description of an item's inventory footprint. */
public final class ItemArea {
    private final int width;
    private final int height;

    public ItemArea(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Item area dimensions must be positive");
        }
        this.width = width;
        this.height = height;
    }

    public int width() { return width; }
    public int height() { return height; }
    public int slotCount() { return width * height; }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ItemArea)) return false;
        ItemArea area = (ItemArea) other;
        return width == area.width && height == area.height;
    }

    @Override
    public int hashCode() { return 31 * width + height; }

    @Override
    public String toString() { return "ItemArea[width=" + width + ", height=" + height + "]"; }
}
