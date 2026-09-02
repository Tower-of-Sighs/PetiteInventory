package com.sighs.petiteinventory.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sighs.petiteinventory.inventory.BorderTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class InventoryRenderer {
    private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int SLOT_SOURCE_X = 25;
    private static final int SLOT_SOURCE_Y = 101;
    private static final int SLOT_SIZE = 18;

    public static void drawNinePatch(GuiGraphics graphics, BorderTheme theme,
                                     int x, int y, int width, int height,
                                     int textureSize, int border) {
        RenderSystem.setShaderColor(theme.getR(), theme.getG(), theme.getB(), 1.0f);
        try {
            drawNinePatchInternal(graphics, x, y, width, height, border);
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static void drawNinePatchInternal(GuiGraphics graphics,
                                              int x, int y, int width, int height, int border) {
        int sourceRight = SLOT_SOURCE_X + SLOT_SIZE - border;
        int sourceBottom = SLOT_SOURCE_Y + SLOT_SIZE - border;
        int sourceCenterSize = SLOT_SIZE - border * 2;

        graphics.blit(INVENTORY_TEXTURE, x, y, SLOT_SOURCE_X, SLOT_SOURCE_Y, border, border, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(INVENTORY_TEXTURE, x + width - border, y, sourceRight, SLOT_SOURCE_Y, border, border, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(INVENTORY_TEXTURE, x, y + height - border, SLOT_SOURCE_X, sourceBottom, border, border, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.blit(INVENTORY_TEXTURE, x + width - border, y + height - border, sourceRight, sourceBottom, border, border, TEXTURE_SIZE, TEXTURE_SIZE);

        if (width > border * 2) {
            graphics.blit(INVENTORY_TEXTURE, x + border, y, width - border * 2, border,
                    SLOT_SOURCE_X + border, SLOT_SOURCE_Y, sourceCenterSize, border, TEXTURE_SIZE, TEXTURE_SIZE);
            graphics.blit(INVENTORY_TEXTURE, x + border, y + height - border, width - border * 2, border,
                    SLOT_SOURCE_X + border, sourceBottom, sourceCenterSize, border, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        if (height > border * 2) {
            graphics.blit(INVENTORY_TEXTURE, x, y + border, border, height - border * 2,
                    SLOT_SOURCE_X, SLOT_SOURCE_Y + border, border, sourceCenterSize, TEXTURE_SIZE, TEXTURE_SIZE);
            graphics.blit(INVENTORY_TEXTURE, x + width - border, y + border, border, height - border * 2,
                    sourceRight, SLOT_SOURCE_Y + border, border, sourceCenterSize, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        if (width > border * 2 && height > border * 2) {
            graphics.blit(INVENTORY_TEXTURE, x + border, y + border, width - border * 2, height - border * 2,
                    SLOT_SOURCE_X + border, SLOT_SOURCE_Y + border, sourceCenterSize, sourceCenterSize, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }
}
