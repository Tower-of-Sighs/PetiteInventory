package com.sighs.petiteinventory.utils;

import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.init.BorderTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;

public class GuiUtils {
    public static ResourceLocation AREA_TEXTURE = new ResourceLocation(Petiteinventory.MODID, "textures/area.png");

    /**
     * 绘制带主题的九宫格（只改变贴图颜色）
     */
    public static void drawNinePatch(GuiGraphics graphics, BorderTheme theme,
                                     int x, int y, int width, int height,
                                     int textureSize, int border) {
        // ✅ 只在这里设置颜色 - 只影响贴图
        RenderSystem.setShaderColor(theme.getR(), theme.getG(), theme.getB(), 1.0f);

        try {
            drawNinePatchInternal(graphics, AREA_TEXTURE, x, y, width, height, textureSize, border);
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    // 内部绘制方法（保持不变）
    private static void drawNinePatchInternal(GuiGraphics graphics, ResourceLocation texture,
                                              int x, int y, int width, int height,
                                              int textureSize, int border) {
        // 角落
        graphics.blit(texture, x, y, 0, 0, border, border, textureSize, textureSize);
        graphics.blit(texture, x + width - border, y, textureSize - border, 0, border, border, textureSize, textureSize);
        graphics.blit(texture, x, y + height - border, 0, textureSize - border, border, border, textureSize, textureSize);
        graphics.blit(texture, x + width - border, y + height - border, textureSize - border, textureSize - border, border, border, textureSize, textureSize);

        // 上下边
        if (width > border * 2) {
            graphics.blit(texture, x + border, y, width - border * 2, border,
                    border, 0, textureSize - border * 2, border, textureSize, textureSize);
            graphics.blit(texture, x + border, y + height - border, width - border * 2, border,
                    border, textureSize - border, textureSize - border * 2, border, textureSize, textureSize);
        }

        // 左右边
        if (height > border * 2) {
            graphics.blit(texture, x, y + border, border, height - border * 2,
                    0, border, border, textureSize - border * 2, textureSize, textureSize);
            graphics.blit(texture, x + width - border, y + border, border, height - border * 2,
                    textureSize - border, border, border, textureSize - border * 2, textureSize, textureSize);
        }

        // 中心
        if (width > border * 2 && height > border * 2) {
            graphics.blit(texture, x + border, y + border, width - border * 2, height - border * 2,
                    border, border, textureSize - border * 2, textureSize - border * 2, textureSize, textureSize);
        }
    }
}