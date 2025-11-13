package com.sighs.petiteinventory.utils;

import com.sighs.petiteinventory.Petiteinventory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class GuiUtils {
    public static ResourceLocation AREA = new ResourceLocation(Petiteinventory.MODID, "textures/area.png");

    public static void drawNinePatch(GuiGraphics graphics, ResourceLocation texture,
                                      int x, int y, int width, int height,
                                      int textureSize, int border) {
        // 角落（不拉伸）
        graphics.blit(texture, x, y, 0, 0, border, border, textureSize, textureSize); // 左上
        graphics.blit(texture, x + width - border, y, textureSize - border, 0, border, border, textureSize, textureSize); // 右上
        graphics.blit(texture, x, y + height - border, 0, textureSize - border, border, border, textureSize, textureSize); // 左下
        graphics.blit(texture, x + width - border, y + height - border, textureSize - border, textureSize - border, border, border, textureSize, textureSize); // 右下

        // 上边（横向拉伸）
        if (width > border * 2) {
            graphics.blit(texture,
                    x + border, y,
                    width - border * 2, border,           // 目标尺寸
                    border, 0,                             // 源UV起点
                    textureSize - border * 2, border,      // 源区尺寸（只取顶部边框带）
                    textureSize, textureSize);
        }

        // 下边（横向拉伸）
        if (width > border * 2) {
            graphics.blit(texture,
                    x + border, y + height - border,
                    width - border * 2, border,
                    border, textureSize - border,
                    textureSize - border * 2, border,
                    textureSize, textureSize);
        }

        // 左边（纵向拉伸）
        if (height > border * 2) {
            graphics.blit(texture,
                    x, y + border,
                    border, height - border * 2,
                    0, border,
                    border, textureSize - border * 2,
                    textureSize, textureSize);
        }

        // 右边（纵向拉伸）
        if (height > border * 2) {
            graphics.blit(texture,
                    x + width - border, y + border,
                    border, height - border * 2,
                    textureSize - border, border,
                    border, textureSize - border * 2,
                    textureSize, textureSize);
        }

        // 中心（双轴拉伸）
        if (width > border * 2 && height > border * 2) {
            graphics.blit(texture,
                    x + border, y + border,
                    width - border * 2, height - border * 2,
                    border, border,
                    textureSize - border * 2, textureSize - border * 2,
                    textureSize, textureSize);
        }
    }
}