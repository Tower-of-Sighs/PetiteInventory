package com.sighs.petiteinventory.platform;


import net.neoforged.fml.common.EventBusSubscriber;
import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.platform.EditModeCommand;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = Petiteinventory.MODID)
public final class CommandRegistrationEvent {
    @SubscribeEvent
    public static void onCommandRegistration(RegisterCommandsEvent event) {
        EditModeCommand.register(event.getDispatcher());
        ScreenLayoutModeCommand.register(event.getDispatcher());
    }
}
