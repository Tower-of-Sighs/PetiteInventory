package com.sighs.petiteinventory.platform.inventory;


import net.neoforged.neoforge.network.PacketDistributor;
import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.platform.ISophisticatedStorageMenu;
import com.sighs.petiteinventory.platform.NetworkChannel;
import com.sighs.petiteinventory.platform.SophisticatedQuickMovePayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Target adapter for Sophisticated Core's private menu contract. */
public final class SophisticatedQuickMoveService {
    private SophisticatedQuickMoveService() {
    }

    /** Returns null when vanilla/Core should continue, otherwise the handled result. */
    public static ItemStack handle(AbstractContainerMenu menu, Player player, int slotIndex) {
        if (!(menu instanceof ISophisticatedStorageMenu) || player == null
                || slotIndex < 0 || slotIndex >= menu.slots.size()) return null;
        ISophisticatedStorageMenu access = (ISophisticatedStorageMenu) menu;
        if (access.petiteinventory$isStorageInventorySlot(slotIndex)) return null;

        Slot source = menu.slots.get(slotIndex);
        if (!(source.container instanceof Inventory) || !source.hasItem()) return null;
        ItemStack sourceStack = source.getItem();
        List<Slot> storageSlots = new ArrayList<>();
        for (int index = 0; index < menu.slots.size(); index++) {
            if (access.petiteinventory$isStorageInventorySlot(index)) storageSlots.add(menu.slots.get(index));
        }
        if (storageSlots.isEmpty()) return null;

        // Let the private menu own its server-side transaction; the client sends
        // the selected anchor and footprint through the existing payload.
        if (!player.level().isClientSide) return ItemStack.EMPTY;

        Area area = ItemInventoryService.getArea(sourceStack);
        ContainerGrid grid = ContainerGrid.parse(storageSlots);
        int width = storageWidth(grid, storageSlots, access.petiteinventory$getNumberOfRows());
        ContainerGrid.Cell target = grid.findAreaBySlotOrder(area, width);
        boolean originalRotation = ItemInventoryService.ItemRotateHelper.isRotated(sourceStack);
        if (target == null) {
            ItemInventoryService.ItemRotateHelper.setRotated(sourceStack, !originalRotation);
            target = grid.findAreaBySlotOrder(ItemInventoryService.getArea(sourceStack), width);
            if (target == null) {
                ItemInventoryService.ItemRotateHelper.setRotated(sourceStack, originalRotation);
                return ItemStack.EMPTY;
            }
        }

        Slot targetSlot = target.slot();
        if (!targetSlot.mayPlace(sourceStack)) return ItemStack.EMPTY;
        Area targetArea = ItemInventoryService.getArea(sourceStack);
        int[] footprint = grid.getCellsBySlotOrder(target, targetArea, width).stream()
                .mapToInt(cell -> menu.slots.indexOf(cell.slot())).toArray();
        if (footprint.length != targetArea.width() * targetArea.height()) return ItemStack.EMPTY;

        ItemStack moved = sourceStack.copy();
        targetSlot.set(moved);
        targetSlot.setChanged();
        source.set(ItemStack.EMPTY);
        source.setChanged();
        source.onTake(player, moved);
        PacketDistributor.sendToServer(new SophisticatedQuickMovePayload(
                slotIndex, menu.slots.indexOf(targetSlot), footprint,
                ItemInventoryService.ItemRotateHelper.isRotated(sourceStack)));
        return moved;
    }

    private static int storageWidth(ContainerGrid grid, List<Slot> slots, int rows) {
        if (rows > 0 && slots.size() % rows == 0) return Math.max(1, slots.size() / rows);
        if (grid.getWidth() > 1) return grid.getWidth();
        return rows > 0 ? Math.max(1, (slots.size() + rows - 1) / rows) : slots.size();
    }
}
