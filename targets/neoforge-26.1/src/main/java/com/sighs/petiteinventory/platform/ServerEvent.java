package com.sighs.petiteinventory.platform;


import net.neoforged.fml.common.EventBusSubscriber;
import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.platform.IAbstractContainerMenu;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = Petiteinventory.MODID)
public class ServerEvent {
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        ((IAbstractContainerMenu) event.getEntity().containerMenu).setPlayer(event.getEntity());
    }
}
