package com.sighs.petiteinventory.init;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

public class AreaEvent extends Event {
    public int width;
    public int height;
    public ItemStack itemStack;

    public AreaEvent(int width, int height, ItemStack itemStack) {
        this.width = width;
        this.height = height;
        this.itemStack = itemStack;
    }
}