package com.sighs.petiteinventory.platform;


import net.neoforged.fml.common.EventBusSubscriber;
import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.compat.KubeJSCompat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = Petiteinventory.MODID)
public class CommonSetupEvent {
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(KubeJSCompat::init);
    }
}
