package com.sighs.petiteinventory.init;

import com.sighs.petiteinventory.loader.EntryCache;
import com.sighs.petiteinventory.utils.ItemUtils;
import net.minecraft.world.item.ItemStack;

public record Area(ItemStack itemStack, int width, int height) {
    public static Area of(ItemStack itemStack) {
        return ItemUtils.getArea(itemStack);
    }

    public int minSize() {
        return Math.min(width, height);
    }

    @Override
    public String toString() {
        return "[" + itemStack + "](" + width + "," + height + ")";
    }
}
