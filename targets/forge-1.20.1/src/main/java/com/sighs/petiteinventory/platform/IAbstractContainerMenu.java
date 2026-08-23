package com.sighs.petiteinventory.platform;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IAbstractContainerMenu {
    Player getPlayer();
    void setPlayer(Player player);

    boolean petiteinventory$moveItemStackTo(ItemStack stack, int start, int end, boolean reverse);
}
