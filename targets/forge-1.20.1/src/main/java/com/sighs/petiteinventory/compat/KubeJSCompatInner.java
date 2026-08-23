package com.sighs.petiteinventory.compat;

import com.sighs.petiteinventory.compat.AreaEventJS;
import com.sighs.petiteinventory.compat.Events;
import com.sighs.petiteinventory.inventory.AreaEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class KubeJSCompatInner {
    public static AreaEvent<net.minecraft.world.item.ItemStack> area(
            AreaEvent<net.minecraft.world.item.ItemStack> event) {
        var eventJS = new AreaEventJS(event.width, event.height, event.itemStack);
        if (!FMLEnvironment.dist.isDedicatedServer()) {
            Events.CLIENT_EVENT.post(eventJS);
        }
        Events.SERVER_EVENT.post(eventJS);
        return new AreaEvent<>(eventJS.width, eventJS.height, event.itemStack);
    }
}
