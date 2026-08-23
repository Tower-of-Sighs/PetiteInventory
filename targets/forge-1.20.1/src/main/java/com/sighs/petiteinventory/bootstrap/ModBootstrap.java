package com.sighs.petiteinventory.bootstrap;

import com.sighs.petiteinventory.config.ModConfig;
import com.sighs.petiteinventory.config.BorderThemeCache;
import com.sighs.petiteinventory.platform.NetworkChannel;
import com.sighs.petiteinventory.event.InventoryEvents;
import com.sighs.petiteinventory.spi.PlatformServices;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;

/** Coordinates startup; feature modules own the work they register here. */
public final class ModBootstrap {
    private ModBootstrap() {
    }

    public static void initialize() {
        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC);
        NetworkChannel.register();
        BorderThemeCache.load();
        PlatformServices.initialize(InventoryEvents.BUS);
    }
}
