package com.sighs.petiteinventory.init;

import com.sighs.petiteinventory.loader.EntryCache;
import net.minecraft.world.item.ItemStack;

public class Area {
    private ItemStack itemStack;
    private int width;
    private int height;

    public Area(ItemStack itemStack) {
        EntryCache
    }

    public int size() {
        return Math.min(width, height);
    }
}
