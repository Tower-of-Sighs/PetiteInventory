package com.sighs.petiteinventory.compat;

import dev.latvian.mods.kubejs.player.KubePlayerEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AreaEventJS implements KubePlayerEvent {
    public int width;
    public int height;
    public ItemStack itemStack;

    public AreaEventJS(int width, int height, ItemStack itemStack) {
        this.width = width;
        this.height = height;
        this.itemStack = itemStack;
    }

    @Override
    public Player getEntity() {
        return null;
    }
}