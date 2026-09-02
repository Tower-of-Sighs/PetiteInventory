package com.sighs.petiteinventory.platform;


import net.neoforged.fml.common.EventBusSubscriber;
import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.config.ItemSizeRuleCache;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = Petiteinventory.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ReloadEvent {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCommand(CommandEvent event) {
        String rawCommand = event.getParseResults().getReader().getString();
        if (rawCommand.equals("reload")) ItemSizeRuleCache.loadAllRule();
    }

    @SubscribeEvent
    public static void onLoad(LevelEvent.Load event) {
        ItemSizeRuleCache.loadAllRule();
    }
}
