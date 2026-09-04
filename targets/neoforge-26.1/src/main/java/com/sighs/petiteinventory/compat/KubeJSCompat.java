package com.sighs.petiteinventory.compat;

import com.sighs.petiteinventory.event.InventoryEvents;
import com.sighs.petiteinventory.inventory.AreaEvent;
import net.neoforged.fml.ModList;
import net.minecraft.world.item.ItemStack;

public class KubeJSCompat {
    private static final String MOD_ID = "kubejs";
    private static boolean INSTALLED = false;
    private static boolean SUBSCRIBED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
        if (INSTALLED && !SUBSCRIBED) {
            InventoryEvents.BUS.subscribe(AreaEvent.class, 0, event -> {
                @SuppressWarnings("unchecked")
                AreaEvent<ItemStack> typed = (AreaEvent<ItemStack>) event;
                AreaEvent<ItemStack> transformed = KubeJSCompatInner.area(typed);
                typed.width = transformed.width;
                typed.height = transformed.height;
                typed.itemStack = transformed.itemStack;
            });
            SUBSCRIBED = true;
        }
    }

    public static AreaEvent<net.minecraft.world.item.ItemStack> area(
            AreaEvent<net.minecraft.world.item.ItemStack> event) {
        if (INSTALLED) {
            return KubeJSCompatInner.area(event);
        }
        return event;
    }
}
