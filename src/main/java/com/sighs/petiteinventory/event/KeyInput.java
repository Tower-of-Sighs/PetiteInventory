package com.sighs.petiteinventory.event;

import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.registry.ModKeybindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Petiteinventory.MODID, value = Dist.CLIENT)
public class KeyInput {
    @SubscribeEvent
    public static void copy(ScreenEvent.KeyReleased event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            if (event.getKeyCode() == ModKeybindings.KEY.getKey().getValue()) {
                if (screen.hoveredSlot == null) return;
                String menuType = screen.getMenu().getClass().toString();
                SystemToast.add(Minecraft.getInstance().getToasts(), SystemToast.SystemToastIds.PERIODIC_NOTIFICATION, Component.translatable("toast.petiteinventory.copied.title"), Component.translatable("toast.petiteinventory.copied.detail"));
                Minecraft.getInstance().keyboardHandler.setClipboard(menuType);
            }
        }
    }
}
