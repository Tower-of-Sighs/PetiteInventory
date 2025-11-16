package com.sighs.petiteinventory.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.init.ContainerGrid;
import com.sighs.petiteinventory.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.*;

@Mixin(value = AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow @Nullable protected Slot hoveredSlot;

    @Shadow private ItemStack draggingItem;

    @Shadow public abstract boolean mouseClicked(double p_97748_, double p_97749_, int p_97750_);

    @Shadow @Nullable protected abstract Slot findSlot(double p_97745_, double p_97746_);

    @Shadow public abstract boolean mouseReleased(double p_97812_, double p_97813_, int p_97814_);

    protected AbstractContainerScreenMixin(Component p_96550_) {
        super(p_96550_);
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void onRender(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (!ClientUtils.isClientGridSlot(slot)) return;
        if (!slot.hasItem()) return;
        Area area = Area.of(slot.getItem());
        int x = slot.x;
        int y = slot.y;
        int w = 18 * area.width();
        int h = 18 * area.height();
        GuiUtils.drawNinePatch(
                guiGraphics, GuiUtils.AREA,
                x - 1, y - 1, w, h,
                18, 1
        );
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"), remap = false)
    private void ond(GuiGraphics guiGraphics, int x, int y, int p_283504_, int color) {
        int w = 16, h = 16;
        ItemStack cursorItem = getCursorItem();
        if (ClientUtils.isClientGridSlot(hoveredSlot) && !cursorItem.isEmpty()) {
            Area area = Area.of(cursorItem);
            ContainerGrid grid = ClientUtils.getContainerGrid();
            ContainerGrid.Cell hoverCell = grid.getCell(hoveredSlot);
            for (ContainerGrid.Cell cell : grid.getCells(hoverCell, area)) {
                AbstractContainerScreen.renderSlotHighlight(guiGraphics, cell.slot().x, cell.slot().y, 0, color);
            }
        } else {
            hoveredSlot = ClientUtils.getMappedSlot(hoveredSlot);
            ItemStack hoverItem = hoveredSlot.getItem();
            if (!hoverItem.isEmpty() && ClientUtils.isClientGridSlot(hoveredSlot)) {
                Area area = Area.of(hoverItem);
                w += 18 * (area.width() - 1);
                h += 18 * (area.height() - 1);
            }
            x = hoveredSlot.x;
            y = hoveredSlot.y;
            guiGraphics.fillGradient(RenderType.guiOverlay(), x, y, x + w, y + h, color, color, p_283504_);
        }
    }

    @Unique
    private Slot needReplaceSlot = null;

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onClick(double mouseX, double mouseY, int p_97750_, CallbackInfoReturnable<Boolean> cir) {
        ItemStack cursorItem = getCursorItem();
        Slot slot = findSlot(mouseX, mouseY);
        if (cursorItem != null && ClientUtils.isClientGridSlot(slot) && needReplaceSlot == null) {
            ContainerGrid grid = ClientUtils.getContainerGrid();

            if (grid.getCell(slot) == null) return;

            var cellMap = grid.getCellMap();
            ContainerGrid.Cell clickedCell = grid.getCell(slot);

            Area targetArea = Area.of(getCursorItem());
            var targetAreaCells = grid.getCells(clickedCell, targetArea);
            if (targetAreaCells.size() == targetArea.width() * targetArea.height()) {
                // 获取到的格子数量和所需格子数一样，说明没过界。
                // 目标区域内所有的区域核心格子
                Set<ContainerGrid.Cell> AreaCells = new HashSet<>();
                for (ContainerGrid.Cell c : targetAreaCells) {
                    ContainerGrid.Cell mapped = cellMap.get(c);
                    if (mapped != null) AreaCells.add(mapped);
                }

                if (AreaCells.isEmpty()) return;
                else if (AreaCells.size() == 1) {
                    needReplaceSlot = AreaCells.toArray(new ContainerGrid.Cell[]{})[0].slot();
                }
                else cir.setReturnValue(true);
            }
            else cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("RETURN"))
    private void qq(double mouseX, double mouseY, int p_97814_, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = findSlot(mouseX, mouseY);
        if (needReplaceSlot != null && ClientUtils.isClientGridSlot(slot)) {
            int offsetX = needReplaceSlot.x - slot.x;
            int offsetY = needReplaceSlot.y - slot.y;
            needReplaceSlot = null;
            mouseClicked(mouseX + offsetX, mouseY + offsetY, p_97814_);
        }
    }

    @Inject(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void scale(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (!ClientUtils.isClientGridSlot(slot)) return;
        scale(guiGraphics, Area.of(slot.getItem()), slot.x, slot.y);
    }
    @Inject(method = "renderFloatingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private void scale(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, String p_282568_, CallbackInfo ci) {
        if (!ClientUtils.isClientGridSlot(hoveredSlot)) return;
        scale(guiGraphics, Area.of(itemStack), x, y);
    }

    private void scale(GuiGraphics guiGraphics, Area area, int x, int y) {
        int w = area.width(), h = area.height();
        float minSize = area.minSize();
        float offsetX = 9 * (w - minSize);
        float offsetY = 9 * (h - minSize);
        float scale = minSize > 1 ? minSize * 0.8F : minSize;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.translate(x, y, 0);
        poseStack.scale(scale, scale, 1.0f);
        poseStack.translate(-x, -y, 0);
        if (minSize > 1) {
            poseStack.translate(offsetX * 0.8, offsetY * 0.8, 0);
            float offset = 16 * 0.1F * minSize;
            poseStack.translate(offset * 0.8, offset * 0.8, 0);
        } else {
            poseStack.translate(offsetX, offsetY, 0);
        }
    }

    private ItemStack getCursorItem() {
        AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
        return this.draggingItem.isEmpty() ? menu.getCarried() : this.draggingItem;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void doTick(CallbackInfo ci) {
        ItemStack cursorItem = getCursorItem();
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        if (!cursorItem.isEmpty() && Area.of(cursorItem).maxSize() > 1 && ClientUtils.isClientGridSlot(hoveredSlot)) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        } else GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }
}
