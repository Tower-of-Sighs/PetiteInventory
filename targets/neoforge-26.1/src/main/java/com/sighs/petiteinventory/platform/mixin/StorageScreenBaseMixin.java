package com.sighs.petiteinventory.platform.mixin;

import org.joml.Matrix3x2fStack;
import com.sighs.petiteinventory.client.ClientInventoryContext;
import com.sighs.petiteinventory.client.InventoryRenderer;
import com.sighs.petiteinventory.config.BorderThemeCache;
import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.inventory.BorderTheme;
import com.sighs.petiteinventory.platform.inventory.ContainerGrid;
import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import com.sighs.petiteinventory.platform.inventory.InventorySlotService;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
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
            method = "extractStack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;II)V"),
            remap = false
    )
    private void renderSizedItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        Slot slot = findGridSlot(x, y);
        if (slot == null) {
            graphics.item(stack, x, y);
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
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + offsetX, y + offsetY);
        pose.scale(scale, scale);
        pose.translate(-x, -y);
        graphics.item(stack, x, y);
        pose.popMatrix();
    }

    @Redirect(
            method = "extractStack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"),
            remap = false
    )
    private void renderSizedDecorations(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y, String text) {
        Slot slot = findGridSlot(x, y);
        if (slot == null) {
            graphics.itemDecorations(font, stack, x, y, text);
            return;
        }
        Area area = ItemInventoryService.getArea(stack);
        graphics.itemDecorations(font, stack, x + area.width() * 18 - 16, y + area.height() * 18 - 16, text);
    }

    @Inject(
            method = "extractSlotHighlightBack",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void renderSizedHighlightBack(GuiGraphicsExtractor graphics, Slot slot, CallbackInfo callback) {
        callback.cancel();
    }

    @Inject(
            method = "extractSlotHighlightFront",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void renderSizedHighlightFront(GuiGraphicsExtractor graphics, Slot slot, CallbackInfo callback) {
        if (slot == null || !slot.isHighlightable()) {
            callback.cancel();
            return;
        }
        if (!ClientInventoryContext.isClientGridSlot(slot)) {
            graphics.fillGradient(slot.x, slot.y, slot.x + 16, slot.y + 16,
                    -2130706433, -2130706433);
            callback.cancel();
            return;
        }

        Area area = ItemInventoryService.getArea(slot.getItem());
        int width = 16 + 18 * (area.width() - 1);
        int height = 16 + 18 * (area.height() - 1);
        graphics.fillGradient(slot.x, slot.y, slot.x + width, slot.y + height,
                -2130706433, -2130706433);
        callback.cancel();
    }

    @Inject(method = "extractContents", at = @At("RETURN"))
    private void renderCarriedFootprintHighlight(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
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
                renderSlotHighlight(
                        graphics,
                        screen.getLeftX() + cell.slot().x,
                        screen.getTopY() + cell.slot().y,
                        0,
                        -2130706433);
            }
        }
    }

    private static void renderSlotHighlight(GuiGraphicsExtractor graphics, int x, int y, int blitOffset, int color) {
        graphics.fillGradient(x, y, x + 16, y + 16, color, color);
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
