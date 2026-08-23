package com.sighs.petiteinventory.platform.inventory;

import com.sighs.petiteinventory.inventory.InventoryAdmissionResult;

import com.sighs.petiteinventory.platform.spi.ForgeInventoryPort;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Compatibility facade; the reusable policy is implemented in common. */
public final class InventoryAdmissionService {
    private static final com.sighs.petiteinventory.service.InventoryAdmissionService<Player, ItemStack> COMMON =
            new com.sighs.petiteinventory.service.InventoryAdmissionService<>(new ForgeInventoryPort());

    private InventoryAdmissionService() {
    }

    public static InventoryAdmissionResult admit(Player player, ItemStack incoming) {
        return map(COMMON.admit(player, incoming));
    }

    public static ItemStack normalizeForSlot(int slotIndex, ItemStack stack) {
        return COMMON.normalizeForSlot(slotIndex, stack);
    }

    public static void returnCarriedItem(Player player, ItemStack carried) {
        COMMON.returnCarriedItem(player, carried);
    }

    private static InventoryAdmissionResult map(com.sighs.petiteinventory.service.AdmissionResult result) {
        switch (result) {
            case ACCEPTED:
                return InventoryAdmissionResult.ACCEPTED;
            case REJECTED:
                return InventoryAdmissionResult.REJECTED;
            default:
                return InventoryAdmissionResult.DEFER_TO_VANILLA;
        }
    }
}
