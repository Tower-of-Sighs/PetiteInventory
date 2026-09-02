package com.sighs.petiteinventory.api;

import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import net.minecraft.world.item.ItemStack;

/** Supported integration boundary. Internal config, cache and platform types stay hidden. */
public final class PetiteInventoryApi {
    private PetiteInventoryApi() {
    }

    public static ItemArea getItemArea(ItemStack stack) {
        var area = ItemInventoryService.getArea(stack);
        return new ItemArea(area.width(), area.height());
    }

    public static boolean isRotated(ItemStack stack) {
        return ItemInventoryService.ItemRotateHelper.isRotated(stack);
    }

    public static void setRotated(ItemStack stack, boolean rotated) {
        ItemInventoryService.ItemRotateHelper.setRotated(stack, rotated);
    }
}
