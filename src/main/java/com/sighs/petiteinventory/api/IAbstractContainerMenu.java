package com.sighs.petiteinventory.api;

import net.minecraft.world.entity.player.Player;

public interface IAbstractContainerMenu {
    Player getPlayer();
    void setPlayer(Player player);
}
