package com.sighs.petiteinventory.event;

import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.init.Area;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Petiteinventory.MODID)
public class test {
    @SubscribeEvent
    public static void tick(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player) {
            System.out.print(Area.of(player.getMainHandItem())+"\n");
        }
    }
}
