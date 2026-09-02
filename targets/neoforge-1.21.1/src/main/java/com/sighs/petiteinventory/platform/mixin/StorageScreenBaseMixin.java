package com.sighs.petiteinventory.platform.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sighs.petiteinventory.client.ClientInventoryContext;
import com.sighs.petiteinventory.client.InventoryRenderer;
import com.sighs.petiteinventory.config.BorderThemeCache;
import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.inventory.BorderTheme;
import com.sighs.petiteinventory.platform.inventory.ContainerGrid;
import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import com.sighs.petiteinventory.platform.inventory.InventorySlotService;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Minimal rendering adapter for Sophisticated Core's private renderStack method. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
public abstract class StorageScreenBaseMixin {
    // Core exposes this hook under different mapped names in dev and runtime.
    @Inject(
            method = {"isHovering", "isMouseOverSlot", "m_97774_"},
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void mapSizedItemHitbox(Slot slot, double mouseX, double mouseY,
                                    CallbackInfoReturnable<Boolean> callback) {
        if (!ClientInventoryContext.isClientGridSlot(slot)) return;
        ContainerGrid grid = ClientInventoryContext.getContainerGrid();
        ContainerGrid.Cell cell = grid.getCell(slot);
        ContainerGrid.Cell owner = cell == null ? null : grid.getCellMap().get(cell);
        if (owner == null) return;
        if (!owner.slot().equals(slot)) {
            callback.setReturnValue(false);
            return;
        }
        Area area = ItemInventoryService.getArea(slot.getItem());
        StorageScreenBase<?> screen = (StorageScreenBase<?>) (Object) this;
        int x = screen.getLeftX() + slot.x;
        int y = screen.getTopY() + slot.y;
        callback.setReturnValue(mouseX >= x && mouseX < x + area.width() * 18
                && mouseY >= y && mouseY < y + area.height() * 18);
    }

    @Redirect(
            method = "renderStack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"),
            remap = false
    )
    private void renderSizedItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        Slot slot = findGridSlot(x, y);
        if (slot == null) {
            graphics.renderItem(stack, x, y);
            return;
        }

        Area area = ItemInventoryService.getArea(stack);
        BorderTheme theme = BorderThemeCache.getTheme(stack.getItem(), stack);
        int width = area.width() * 18;
        int height = area.height() * 18;
        InventoryRenderer.drawNinePatch(graphics, theme, x - 1, y - 1, width, height, 18, 1);

        float scale = area.minSize() > 1 ? area.minSize() * 0.8F : 1.0F;
        float renderedSize = 16 * scale;
        float offsetX = (width - 2 - renderedSize) / 2.0F;
        float offsetY = (height - 2 - renderedSize) / 2.0F;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + offsetX, y + offsetY, 0);
        pose.scale(scale, scale, 1.0F);
        pose.translate(-x, -y, 0);
        graphics.renderItem(stack, x, y);
        pose.popPose();
    }

    @Redirect(
            method = "renderStack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"),
            remap = false
    )
    private void renderSizedDecorations(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, String text) {
        Slot slot = findGridSlot(x, y);
        if (slot == null) {
            graphics.renderItemDecorations(font, stack, x, y, text);
            return;
        }
        Area area = ItemInventoryService.getArea(stack);
        graphics.renderItemDecorations(font, stack, x + area.width() * 18 - 16, y + area.height() * 18 - 16, text);
    }

    @Redirect(
            method = "renderSuper",
            at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedcore/client/gui/StorageScreenBase;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"),
            remap = false
    )
    private void renderSizedPlayerHighlight(GuiGraphics graphics, int x, int y, int blitOffset, int color) {
        renderSizedHighlight(graphics, x, y, blitOffset, color);
    }

    @Redirect(
            method = "renderStorageInventorySlots(Lnet/minecraft/client/gui/GuiGraphics;IIZ)V",
            at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedcore/client/gui/StorageScreenBase;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"),
            remap = false
    )
    private void renderSizedStorageHighlight(GuiGraphics graphics, int x, int y, int blitOffset, int color) {
        renderSizedHighlight(graphics, x, y, blitOffset, color);
    }

    @Redirect(
            method = "renderUpgradeSlots",
            at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedcore/client/gui/StorageScreenBase;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"),
            remap = false
    )
    private void renderSizedUpgradeHighlight(GuiGraphics graphics, int x, int y, int blitOffset, int color) {
        renderSizedHighlight(graphics, x, y, blitOffset, color);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void renderCarriedFootprintHighlight(GuiGraphics graphics, int mouseX, int mouseY,
                                                  float partialTick, CallbackInfo callback) {
        StorageScreenBase<?> screen = (StorageScreenBase<?>) (Object) this;
        ItemStack cursorItem = screen.getMenu().getCarried();
        if (cursorItem.isEmpty()) return;

        Slot hovered = findSlotAt(screen, mouseX, mouseY);
        if (!ClientInventoryContext.isClientGridSlot(hovered)) return;

        ContainerGrid grid = ClientInventoryContext.getContainerGrid();
        ContainerGrid.Cell hoverCell = grid.getCell(hovered);
        if (hoverCell == null) return;

        Area area = ItemInventoryService.getArea(cursorItem);
        for (ContainerGrid.Cell cell : grid.getCells(hoverCell, area)) {
            if (cell.slot().container.equals(hoverCell.slot().container)) {
                AbstractContainerScreen.renderSlotHighlight(
                        graphics,
                        screen.getLeftX() + cell.slot().x,
                        screen.getTopY() + cell.slot().y,
                        0,
                        -2130706433);
            }
        }
    }

    private void renderSizedHighlight(GuiGraphics graphics, int x, int y, int blitOffset, int color) {
        Slot slot = findGridSlot(x, y);
        if (slot == null || !ClientInventoryContext.isClientGridSlot(slot)) {
            AbstractContainerScreen.renderSlotHighlight(graphics, x, y, blitOffset, color);
            return;
        }

        Area area = ItemInventoryService.getArea(slot.getItem());
        int width = 16 + 18 * (area.width() - 1);
        int height = 16 + 18 * (area.height() - 1);
        graphics.fillGradient(RenderType.guiOverlay(), x, y, x + width, y + height,
                color, color, blitOffset);
    }

    private Slot findGridSlot(int x, int y) {
        ContainerGrid grid = ClientInventoryContext.getContainerGrid();
        for (ContainerGrid.Cell cell : grid.getCells()) {
            Slot slot = cell.slot();
            if (!InventorySlotService.isPlayerHotbarSlot(slot)
                    && slot.x == x && slot.y == y && slot.hasItem()) return slot;
        }
        return null;
    }

    private Slot findSlotAt(StorageScreenBase<?> screen, double mouseX, double mouseY) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive() && screen.isMouseOverSlot(slot, mouseX, mouseY)) return slot;
        }
        return null;
    }
}
