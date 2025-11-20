package com.sighs.petiteinventory.compat.kubejs;

import dev.latvian.mods.kubejs.client.ClientEventJS;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.player.PlayerEventJS;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AreaEventJS extends PlayerEventJS {
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
