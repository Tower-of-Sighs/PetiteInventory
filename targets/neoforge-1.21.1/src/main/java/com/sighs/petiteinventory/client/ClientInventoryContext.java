package com.sighs.petiteinventory.client;


import net.neoforged.fml.common.EventBusSubscriber;
import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.platform.inventory.ContainerGrid;
import com.sighs.petiteinventory.platform.inventory.InventorySlotService;
import com.sighs.petiteinventory.platform.inventory.ContainerOverlapService;
import com.sighs.petiteinventory.platform.NetworkChannel;
import com.sighs.petiteinventory.platform.TidyOverlapPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = Petiteinventory.MODID, value = Dist.CLIENT)
public class ClientInventoryContext {
    private static ContainerGrid clientGrid = null;
    private static int tidyRequestCooldown;

    public static ContainerGrid getContainerGrid() {
        if (clientGrid == null) clientGrid = getClientContainerGrid();
        return clientGrid;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            clientGrid = getClientContainerGrid();
            requestTidyIfNeeded();
        }
    }

    private static void requestTidyIfNeeded() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) return;
        if (!ScreenLayoutSettings.isEnabled(containerScreen)) return;

        if (tidyRequestCooldown > 0) {
            tidyRequestCooldown--;
            return;
        }
        AbstractContainerMenu menu = containerScreen.getMenu();
        if (menu != null && ContainerOverlapService.needsTidy(menu)) {
            NetworkChannel.sendToServer(new TidyOverlapPayload(false));
            tidyRequestCooldown = 20;
        }
    }

    private static ContainerGrid getClientContainerGrid() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return new ContainerGrid();
        AbstractContainerMenu menu = minecraft.player.containerMenu;
        boolean enableContainer = true;
        if (minecraft.screen instanceof AbstractContainerScreen<?> containerScreen) {
            menu = containerScreen.getMenu();
            enableContainer = ScreenLayoutSettings.isEnabled(containerScreen);
        }
        return InventorySlotService.getContainerGrid(menu, enableContainer);
    }

    public static void invalidate() {
        clientGrid = null;
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
        if (InventorySlotService.isPlayerHotbarSlot(slot)) return false;
        return getContainerGrid().getCell(slot) != null;
    }


}
