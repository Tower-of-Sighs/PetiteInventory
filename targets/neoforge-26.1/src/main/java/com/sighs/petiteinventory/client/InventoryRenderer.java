package com.sighs.petiteinventory.client;

import com.sighs.petiteinventory.inventory.BorderTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class InventoryRenderer {
    private static final Identifier INVENTORY_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int SLOT_SOURCE_X = 25;
    private static final int SLOT_SOURCE_Y = 101;
    private static final int SLOT_SIZE = 18;

    public static void drawNinePatch(GuiGraphicsExtractor graphics, BorderTheme theme,
                                     int x, int y, int width, int height,
                                     int textureSize, int border) {
        int color = ARGB.colorFromFloat(1.0f, theme.getR(), theme.getG(), theme.getB());
        drawNinePatchInternal(graphics, color, x, y, width, height, border);
    }

    private static void drawNinePatchInternal(GuiGraphicsExtractor graphics, int color,
                                              int x, int y, int width, int height, int border) {
        int sourceRight = SLOT_SOURCE_X + SLOT_SIZE - border;
        int sourceBottom = SLOT_SOURCE_Y + SLOT_SIZE - border;
        int sourceCenterSize = SLOT_SIZE - border * 2;

        blit(graphics, color, x, y, SLOT_SOURCE_X, SLOT_SOURCE_Y, border, border, border, border);
        blit(graphics, color, x + width - border, y, sourceRight, SLOT_SOURCE_Y, border, border, border, border);
        blit(graphics, color, x, y + height - border, SLOT_SOURCE_X, sourceBottom, border, border, border, border);
        blit(graphics, color, x + width - border, y + height - border, sourceRight, sourceBottom, border, border, border, border);

        if (width > border * 2) {
            blit(graphics, color, x + border, y, SLOT_SOURCE_X + border, SLOT_SOURCE_Y,
                    width - border * 2, border, sourceCenterSize, border);
            blit(graphics, color, x + border, y + height - border, SLOT_SOURCE_X + border, sourceBottom,
                    width - border * 2, border, sourceCenterSize, border);
        }

        if (height > border * 2) {
            blit(graphics, color, x, y + border, SLOT_SOURCE_X, SLOT_SOURCE_Y + border,
                    border, height - border * 2, border, sourceCenterSize);
            blit(graphics, color, x + width - border, y + border, sourceRight, SLOT_SOURCE_Y + border,
                    border, height - border * 2, border, sourceCenterSize);
        }

        if (width > border * 2 && height > border * 2) {
            blit(graphics, color, x + border, y + border, SLOT_SOURCE_X + border, SLOT_SOURCE_Y + border,
                    width - border * 2, height - border * 2, sourceCenterSize, sourceCenterSize);
        }
    }

    private static void blit(GuiGraphicsExtractor graphics, int color, int x, int y,
                             int u, int v, int width, int height, int sourceWidth, int sourceHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, x, y, u, v,
                width, height, sourceWidth, sourceHeight, TEXTURE_SIZE, TEXTURE_SIZE, color);
    }
}
