package com.sighs.petiteinventory.compat;

import com.sighs.petiteinventory.init.AreaEvent;
import net.minecraftforge.fml.ModList;

public class KubeJSCompat {
    private static final String MOD_ID = "kubejs";
    private static boolean INSTALLED = false;

    public static void init() {
        INSTALLED = ModList.get().isLoaded(MOD_ID);
    }

    public static AreaEvent area(AreaEvent event) {
        if (INSTALLED) {
            return KubeJSCompatInner.area(event);
        }
        return event;
    }
}
