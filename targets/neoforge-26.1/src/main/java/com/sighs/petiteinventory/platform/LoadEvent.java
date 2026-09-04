package com.sighs.petiteinventory.platform;


import net.neoforged.fml.common.EventBusSubscriber;
import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.config.ItemSizeRuleCache;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = Petiteinventory.MODID)
public class LoadEvent {
    @SubscribeEvent
    public static void onConfigLoad(FMLCommonSetupEvent event) {
        event.enqueueWork(ItemSizeRuleCache::loadAllRule);
    }
}
