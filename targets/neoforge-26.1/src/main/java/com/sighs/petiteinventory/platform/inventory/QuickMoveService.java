package com.sighs.petiteinventory.platform.inventory;

import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.compat.SophisticatedBackpacksCompat;
import com.sighs.petiteinventory.platform.IAbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Forge menu adapter for quick-move; the Mixin only publishes the event. */
public final class QuickMoveService {
    private QuickMoveService() {
    }

    public static boolean handle(AbstractContainerMenu menu, int slotIndex, int button,
                                 ContainerInput type, Player player) {
        if (player == null || player.isCreative() || type != ContainerInput.QUICK_MOVE
                || slotIndex < 0 || slotIndex >= menu.slots.size()
                || SophisticatedBackpacksCompat.isBackpackMenu(menu)) {
            return false;
        }

        Slot source = menu.slots.get(slotIndex);
        ItemStack item = source.getItem();
        if (item.isEmpty() || !source.mayPlace(item) || isResultLike(source)) {
            return false;
        }

        List<Slot> targets = new ArrayList<>();
        List<List<Slot>> groups = new ArrayList<>();
        boolean toHotbar = false;
        if (menu instanceof InventoryMenu) {
            if (slotIndex >= InventoryMenu.INV_SLOT_START && slotIndex < InventoryMenu.INV_SLOT_END) {
                toHotbar = true;
                for (int i = InventoryMenu.USE_ROW_SLOT_START; i < InventoryMenu.USE_ROW_SLOT_END; i++) {
                    targets.add(menu.slots.get(i));
                }
                groups.add(targets);
            } else if (slotIndex >= InventoryMenu.USE_ROW_SLOT_START
                    && slotIndex < InventoryMenu.USE_ROW_SLOT_END) {
                for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.INV_SLOT_END; i++) {
                    targets.add(menu.slots.get(i));
                }
                groups.add(targets);
            } else {
                return false;
            }
        } else if (source.container instanceof Inventory) {
            for (Slot candidate : menu.slots) {
                if (!(candidate.container instanceof Inventory) && candidate.mayPlace(item)) {
                    targets.add(candidate);
                }
            }
            groups.add(targets);
        } else {
            List<Slot> hotbar = new ArrayList<>();
            List<Slot> main = new ArrayList<>();
            for (Slot candidate : menu.slots) {
                if (!candidate.mayPlace(item)) continue;
                if (InventorySlotService.isPlayerHotbarSlot(candidate)) hotbar.add(candidate);
                else if (InventorySlotService.isPlayerMainInventorySlot(candidate)) main.add(candidate);
            }
            targets.addAll(hotbar);
            targets.addAll(main);
            groups.add(hotbar);
            groups.add(main);
        }
        if (!toHotbar && groups.stream().noneMatch(group -> !group.isEmpty())) return false;

        ItemStack original = item.copy();
        int originalCount = item.getCount();
        ContainerStackingService.stackIntoExisting(item, targets);
        if (item.isEmpty()) {
            source.set(ItemStack.EMPTY);
            notifyTaken(source, player, original, originalCount, item);
            return true;
        }

        if (toHotbar) {
            ItemInventoryService.ItemRotateHelper.setRotated(item, false);
            int start = targets.get(0).index;
            int end = targets.get(targets.size() - 1).index + 1;
            ((IAbstractContainerMenu) menu).petiteinventory$moveItemStackTo(item, start, end, false);
            notifyTaken(source, player, original, originalCount, item);
            return true;
        }

        Area area = ItemInventoryService.getArea(item);
        Slot target = findArea(groups, area);
        if (target == null) {
            boolean rotated = ItemInventoryService.ItemRotateHelper.isRotated(item);
            ItemInventoryService.ItemRotateHelper.setRotated(item, !rotated);
            target = findArea(groups, ItemInventoryService.getArea(item));
            if (target == null) ItemInventoryService.ItemRotateHelper.setRotated(item, rotated);
        }
        if (target != null) {
            ((IAbstractContainerMenu) menu).petiteinventory$moveItemStackTo(item, target.index, target.index + 1, false);
        }
        notifyTaken(source, player, original, originalCount, item);
        return true;
    }

    private static Slot findArea(List<List<Slot>> groups, Area area) {
        for (List<Slot> group : groups) {
            if (group.isEmpty()) continue;
            ContainerGrid.Cell cell = ContainerGrid.parse(group).findArea(area);
            if (cell != null) return cell.slot();
        }
        return null;
    }

    private static boolean isResultLike(Slot slot) {
        String name = slot.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith("resultslot") || name.endsWith("outputslot")
                || name.contains("merchantresult") || name.contains("traderesult");
    }

    private static void notifyTaken(Slot source, Player player, ItemStack original,
                                    int originalCount, ItemStack remaining) {
        int moved = originalCount - remaining.getCount();
        if (moved <= 0) return;
        ItemStack taken = original.copy();
        taken.setCount(Math.min(moved, original.getMaxStackSize()));
        source.onTake(player, taken);
    }
}
