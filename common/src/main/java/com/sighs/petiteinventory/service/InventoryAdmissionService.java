package com.sighs.petiteinventory.service;

import com.sighs.petiteinventory.core.ItemSize;
import com.sighs.petiteinventory.spi.InventoryPort;
import com.sighs.petiteinventory.spi.StackPort;

/**
 * Loader-independent admission policy. Every target entry point should route
 * item pickup and carried-stack return through this service.
 */
public final class InventoryAdmissionService<P, S> {
    private static final int HOTBAR_END = 9;
    private static final int MAIN_INVENTORY_START = 9;
    private static final int MAIN_INVENTORY_END = 36;

    private final InventoryPort<P, S> inventory;
    private final StackPort<S> stacks;

    public InventoryAdmissionService(InventoryPort<P, S> inventory) {
        if (inventory == null || inventory.stacks() == null) {
            throw new IllegalArgumentException("inventory and inventory.stacks() are required");
        }
        this.inventory = inventory;
        this.stacks = inventory.stacks();
    }

    public AdmissionResult admit(P player, S incoming) {
        if (player == null || incoming == null || stacks.isEmpty(incoming)) {
            return AdmissionResult.ACCEPTED;
        }
        if (stackIntoExisting(player, incoming)) {
            return AdmissionResult.ACCEPTED;
        }

        ItemSize footprint = stacks.footprint(incoming);
        if (isUnit(footprint)) {
            stacks.setRotated(incoming, false);
            return stackIntoExisting(player, incoming)
                    ? AdmissionResult.ACCEPTED
                    : AdmissionResult.DEFER_TO_VANILLA;
        }

        int targetSlot = inventory.findSlotFor(player, footprint);
        if (targetSlot < 0) {
            boolean rotated = stacks.isRotated(incoming);
            stacks.setRotated(incoming, !rotated);
            ItemSize rotatedFootprint = stacks.footprint(incoming);
            targetSlot = inventory.findSlotFor(player, rotatedFootprint);
            if (targetSlot < 0) {
                stacks.setRotated(incoming, rotated);
                return AdmissionResult.REJECTED;
            }
        }

        inventory.set(player, targetSlot, stacks.copy(incoming));
        stacks.setCount(incoming, 0);
        return AdmissionResult.ACCEPTED;
    }

    /** Applies the same normalization rules when a trusted action sets a slot. */
    public S normalizeForSlot(int slotIndex, S stack) {
        S normalized = stacks.copy(stack);
        if (slotIndex >= 0 && slotIndex < HOTBAR_END) {
            stacks.setRotated(normalized, false);
        }
        return normalized;
    }

    /** Returns a carried stack without silently losing it. */
    public void returnCarriedItem(P player, S carried) {
        if (carried == null || stacks.isEmpty(carried)) {
            return;
        }
        S remaining = stacks.copy(carried);
        AdmissionResult result = admit(player, remaining);
        if (result == AdmissionResult.ACCEPTED) {
            return;
        }
        if (result == AdmissionResult.DEFER_TO_VANILLA && inventory.placeFallback(player, remaining)) {
            return;
        }
        inventory.drop(player, remaining);
    }

    private boolean stackIntoExisting(P player, S incoming) {
        for (int slot = 0; slot < Math.min(inventory.size(player), MAIN_INVENTORY_END); slot++) {
            S existing = inventory.get(player, slot);
            if (existing == null || stacks.isEmpty(existing)
                    || !stacks.sameItemIgnoringRotation(existing, incoming)) {
                continue;
            }
            int maximum = Math.min(inventory.maxStackSize(player), stacks.maxStackSize(incoming));
            int amount = Math.min(stacks.count(incoming), maximum - stacks.count(existing));
            if (amount <= 0) {
                continue;
            }
            stacks.grow(existing, amount);
            stacks.shrink(incoming, amount);
            if (stacks.isEmpty(incoming)) {
                return true;
            }
        }
        return stacks.isEmpty(incoming);
    }

    private static boolean isUnit(ItemSize size) {
        return size == null || (size.width() == 1 && size.height() == 1);
    }
}
