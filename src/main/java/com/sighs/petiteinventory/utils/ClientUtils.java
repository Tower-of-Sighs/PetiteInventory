package com.sighs.petiteinventory.utils;

import com.sighs.petiteinventory.init.ContainerGrid;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class ClientUtils {
    public static ContainerGrid getClientContainerGrid() {
        AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
        return ContainerGrid.parse(menu.slots);
    }

    public static Slot getMappedSlot(Slot originCell) {
        AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
        ContainerGrid grid = ContainerGrid.parse(menu.slots);
        ContainerGrid.Cell hoverCell = grid.getCell(originCell);
        ContainerGrid.Cell targetCell = grid.getCellMap().get(hoverCell);
        return targetCell != null ? targetCell.slot() : originCell;
    }
}
