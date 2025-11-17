package com.sighs.petiteinventory.loader;

import com.sighs.petiteinventory.Petiteinventory;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = Petiteinventory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LoadEvent {
    @SubscribeEvent
    public static void onConfigLoad(FMLCommonSetupEvent event) {
        event.enqueueWork(EntryCache::loadAllRule);
    }
}
