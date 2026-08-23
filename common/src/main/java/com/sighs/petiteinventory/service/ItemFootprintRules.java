package com.sighs.petiteinventory.service;

import com.sighs.petiteinventory.core.ItemSize;

/** Pure parsing and rotation helpers for configuration-backed item footprints. */
public final class ItemFootprintRules {
    private ItemFootprintRules() {
    }

    /** Parses the persisted {@code width*height} form. */
    public static ItemSize parse(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("Footprint expression is required");
        }
        String normalized = expression.replace(" ", "").replace('x', '*').replace('X', '*');
        String[] dimensions = normalized.split("\\*", -1);
        if (dimensions.length != 2) {
            throw new IllegalArgumentException("Footprint must use width*height: " + expression);
        }
        try {
            return new ItemSize(Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1]));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid footprint: " + expression, exception);
        }
    }

    public static ItemSize rotate(ItemSize size, boolean rotated) {
        return rotated ? size.rotated() : size;
    }
}
