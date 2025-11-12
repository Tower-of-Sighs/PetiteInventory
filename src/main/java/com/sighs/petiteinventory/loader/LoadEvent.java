package com.sighs.petiteinventory.loader;

import com.sighs.petiteinventory.Petiteinventory;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Petiteinventory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LoadEvent {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(Petiteinventory.MODID)) EntryCache.loadAllRule();
    }
}
