package com.sighs.petiteinventory.event;

import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.api.IAbstractContainerMenu;
import com.sighs.petiteinventory.init.Area;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Petiteinventory.MODID)
public class test {
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        ((IAbstractContainerMenu) event.player.containerMenu).setPlayer(event.player);
    }
}
