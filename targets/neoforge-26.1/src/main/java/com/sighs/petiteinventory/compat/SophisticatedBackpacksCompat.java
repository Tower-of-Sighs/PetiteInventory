package com.sighs.petiteinventory.compat;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SophisticatedBackpacksCompat {
    private static final String BACKPACK_MENU = "net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer";

    private SophisticatedBackpacksCompat() {
    }

    public static boolean isBackpackMenu(AbstractContainerMenu menu) {
        return menu != null && BACKPACK_MENU.equals(menu.getClass().getName());
    }

    public static List<Slot> getStorageSlots(AbstractContainerMenu menu) {
        if (!isBackpackMenu(menu)) return Collections.emptyList();
        List<Slot> storageSlots = new ArrayList<>();
        try {
            Method isStorageSlot = menu.getClass().getMethod("isStorageInventorySlot", int.class);
            for (int index = 0; index < menu.slots.size(); index++) {
                if ((boolean) isStorageSlot.invoke(menu, index)) storageSlots.add(menu.slots.get(index));
            }
        } catch (ReflectiveOperationException exception) {
            // Older and newer Sophisticated Core releases keep storage slots at
            // the start of the menu. Use their stable public count as a fallback.
        }

        if (!storageSlots.isEmpty()) return storageSlots;

        try {
            Method getStorageSlotCount = menu.getClass().getMethod("getNumberOfStorageInventorySlots");
            int count = (int) getStorageSlotCount.invoke(menu);
            for (int index = 0; index < Math.min(count, menu.slots.size()); index++) {
                storageSlots.add(menu.slots.get(index));
            }
        } catch (ReflectiveOperationException ignored) {
        }
        if (!storageSlots.isEmpty()) return storageSlots;

        // StorageContainerMenuBase always adds storage slots before the player
        // inventory. This remains reliable when dependency method names differ.
        for (Slot slot : menu.slots) {
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory) break;
            storageSlots.add(slot);
        }
        return storageSlots;
    }

    public static boolean isStorageSlot(AbstractContainerMenu menu, int index) {
        if (!isBackpackMenu(menu) || index < 0 || index >= menu.slots.size()) return false;
        try {
            Method isStorageSlot = menu.getClass().getMethod("isStorageInventorySlot", int.class);
            return (boolean) isStorageSlot.invoke(menu, index);
        } catch (ReflectiveOperationException ignored) {
            try {
                Method getStorageSlotCount = menu.getClass().getMethod("getNumberOfStorageInventorySlots");
                return index < (int) getStorageSlotCount.invoke(menu);
            } catch (ReflectiveOperationException ignoredAgain) {
                return index < getStorageSlots(menu).size();
            }
        }
    }

    public static List<Slot> getPlayerMainInventorySlots(AbstractContainerMenu menu) {
        return getPlayerSlots(menu, 0, 27);
    }

    public static List<Slot> getPlayerHotbarSlots(AbstractContainerMenu menu) {
        return getPlayerSlots(menu, 27, 9);
    }

    private static List<Slot> getPlayerSlots(AbstractContainerMenu menu, int offset, int count) {
        if (!isBackpackMenu(menu)) return Collections.emptyList();
        int firstPlayerSlot = 0;
        while (firstPlayerSlot < menu.slots.size()
                && !(menu.slots.get(firstPlayerSlot).container instanceof net.minecraft.world.entity.player.Inventory)) {
            firstPlayerSlot++;
        }
        int start = firstPlayerSlot + offset;
        int end = Math.min(start + count, menu.slots.size());
        if (start >= end) return Collections.emptyList();
        return new ArrayList<>(menu.slots.subList(start, end));
    }
}
