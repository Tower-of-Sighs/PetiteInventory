package com.sighs.petiteinventory.utils;

import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.init.ContainerGrid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Petiteinventory.MODID, value = Dist.CLIENT)
public class ClientUtils {
    private static ContainerGrid clientGrid = null;

    public static ContainerGrid getContainerGrid() {
        if (clientGrid == null) clientGrid = getClientContainerGrid();
        return clientGrid;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Player player = Minecraft.getInstance().player;
        if (event.phase == TickEvent.Phase.END && player != null) {
            clientGrid = getClientContainerGrid();
        }
    }

    private static ContainerGrid getClientContainerGrid() {
        AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
        return SlotUtils.getContainerGrid(menu);
    }

    public static Slot getMappedSlot(Slot originCell) {
        ContainerGrid grid = getContainerGrid();
        ContainerGrid.Cell hoverCell = grid.getCell(originCell);
        ContainerGrid.Cell targetCell = grid.getCellMap().get(hoverCell);
        return targetCell != null ? targetCell.slot() : originCell;
    }

    public static boolean isClientGridSlot(Slot slot) {
        if (Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen) return false;
        if (slot == null) return false;
        return getContainerGrid().getCell(slot) != null;
    }


}
