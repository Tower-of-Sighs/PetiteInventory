package com.sighs.petiteinventory.event;

import com.sighs.petiteinventory.command.BorderColorCommand;
import com.sighs.petiteinventory.command.SizeCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.sighs.petiteinventory.Petiteinventory;

@Mod.EventBusSubscriber(modid = Petiteinventory.MODID)
public class CommandRegistrationEvent {
    @SubscribeEvent
    public static void onCommandRegistration(RegisterCommandsEvent event) {
        BorderColorCommand.register(event.getDispatcher());
        SizeCommand.register(event.getDispatcher());  // ← 新增这行
    }
}