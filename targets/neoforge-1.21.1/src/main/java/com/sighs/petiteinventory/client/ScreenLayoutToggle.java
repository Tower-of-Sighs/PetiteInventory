package com.sighs.petiteinventory.client;


import net.neoforged.fml.common.EventBusSubscriber;
import com.sighs.petiteinventory.Petiteinventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = Petiteinventory.MODID, value = Dist.CLIENT)
public final class ScreenLayoutToggle {
    private static final int Y = 5;
    private static final int SIZE = 11;

    private ScreenLayoutToggle() {
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!ClientEditMode.isEnabled() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen.getMenu() instanceof InventoryMenu) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int x = getX(graphics.guiWidth());
        boolean enabled = ScreenLayoutSettings.isEnabled(screen);
        boolean hovered = event.getMouseX() >= x && event.getMouseX() < x + SIZE
                && event.getMouseY() >= Y && event.getMouseY() < Y + SIZE;
        graphics.fill(x, Y, x + SIZE, Y + SIZE, hovered ? 0xFF3A3A3A : 0xFF202020);
        graphics.fill(x + 1, Y + 1, x + 10, Y + 10, 0xFFBFBFBF);
        graphics.fill(x + 2, Y + 2, x + 9, Y + 9, 0xFF4A4A4A);
        if (enabled) {
            graphics.fill(x + 3, Y + 5, x + 5, Y + 8, 0xFFFFFFFF);
            graphics.fill(x + 5, Y + 7, x + 8, Y + 9, 0xFFFFFFFF);
            graphics.fill(x + 7, Y + 3, x + 9, Y + 7, 0xFFFFFFFF);
        }
        graphics.drawString(Minecraft.getInstance().font, Component.literal("Enable Petite layout"), x + 15, Y + 2, 0xFFFFFFFF, true);
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!ClientEditMode.isEnabled() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen.getMenu() instanceof InventoryMenu) return;
        int x = getX(Minecraft.getInstance().getWindow().getGuiScaledWidth());
        if (event.getButton() != 0 || event.getMouseX() < x || event.getMouseX() >= x + SIZE
                || event.getMouseY() < Y || event.getMouseY() >= Y + SIZE) return;

        ScreenLayoutSettings.setEnabled(screen, !ScreenLayoutSettings.isEnabled(screen));
        ClientInventoryContext.invalidate();
        event.setCanceled(true);
    }

    private static int getX(int screenWidth) {
        return Math.max(4, screenWidth / 2 - 74);
    }
}
