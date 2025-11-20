package com.sighs.petiteinventory.init;

import com.sighs.petiteinventory.utils.ItemUtils;
import net.minecraft.world.item.ItemStack;

public record Area(int width, int height, ItemStack itemStack) {
    public static Area of(ItemStack itemStack) {
        return ItemUtils.getArea(itemStack);
    }

    public int minSize() {
        return Math.min(width, height);
    }

    public int maxSize() {
        return Math.max(width, height);
    }

    @Override
    public String toString() {
        return "[" + itemStack + "](" + width + "," + height + ")";
    }
}
