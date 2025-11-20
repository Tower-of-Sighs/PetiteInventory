package com.sighs.petiteinventory.compat;

import com.sighs.petiteinventory.compat.kubejs.AreaEventJS;
import com.sighs.petiteinventory.compat.kubejs.Events;
import com.sighs.petiteinventory.init.AreaEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class KubeJSCompatInner {
    public static AreaEvent area(AreaEvent event) {
        var eventJS = new AreaEventJS(event.width, event.height, event.itemStack);
        if (!FMLEnvironment.dist.isDedicatedServer()) {
            Events.CLIENT_EVENT.post(eventJS);
        }
        Events.SERVER_EVENT.post(eventJS);
        return new AreaEvent(eventJS.width, event.height, event.itemStack);
    }
}
