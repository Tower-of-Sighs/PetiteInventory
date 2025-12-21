package com.sighs.petiteinventory.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.init.BorderTheme;
import com.sighs.petiteinventory.init.ContainerGrid;
import com.sighs.petiteinventory.loader.BorderColorCache;
import com.sighs.petiteinventory.network.PacketHandler;
import com.sighs.petiteinventory.network.PlaceItemPayload;
import com.sighs.petiteinventory.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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

import javax.annotation.Nullable;
import java.util.*;

@Mixin(value = AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow @Nullable
    public Slot hoveredSlot;

    @Shadow private ItemStack draggingItem;

    @Shadow public abstract boolean mouseClicked(double p_97748_, double p_97749_, int p_97750_);

    @Shadow @Nullable protected abstract Slot findSlot(double p_97745_, double p_97746_);

    @Shadow public abstract boolean mouseReleased(double p_97812_, double p_97813_, int p_97814_);

    @Shadow protected int leftPos;

    @Shadow protected int topPos;

    protected AbstractContainerScreenMixin(Component p_96550_) {
        super(p_96550_);
    }

    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void onRender(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (!ClientUtils.isClientGridSlot(slot)) return;
        if (!slot.hasItem()) return;

        Area area = Area.of(slot.getItem());
        int x = slot.x;
        int y = slot.y;
        int w = 18 * area.width();
        int h = 18 * area.height();

// ✅ 修正：传入slot.getItem()而不是只传Item类型
        BorderTheme theme = BorderColorCache.getTheme(slot.getItem().getItem(), slot.getItem());

        GuiUtils.drawNinePatch(guiGraphics, theme, x - 1, y - 1, w, h, 18, 1);
    }

    @Unique
    private int getThemeColor(BorderTheme theme) {
        return switch (theme) {
            case BLUE -> -16776961;    // 蓝色
            case PURPLE -> -65281;     // 紫色
            case ORANGE -> -256;       // 橙色
            case RED -> -65536;        // 红色
            default -> -2130706433;    // 默认
        };
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void highlight(GuiGraphics guiGraphics, int p_283661_, int p_281248_, float p_281886_, CallbackInfo ci) {
        ItemStack cursorItem = getCursorItem();
        if (ClientUtils.isClientGridSlot(hoveredSlot) && !cursorItem.isEmpty()) {
            Area area = getRotatedArea(cursorItem);
            ContainerGrid grid = ClientUtils.getContainerGrid();
            ContainerGrid.Cell hoverCell = grid.getCell(hoveredSlot);
            for (ContainerGrid.Cell cell : grid.getCells(hoverCell, area)) {
                if (cell.slot().container.equals(hoverCell.slot().container)) {
                    AbstractContainerScreen.renderSlotHighlight(guiGraphics, cell.slot().x + leftPos, cell.slot().y + topPos, 0, -2130706433);
                }
            }
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"), remap = false)
    private void ond(GuiGraphics guiGraphics, int x, int y, int p_283504_, int color) {
        int w = 16, h = 16;
        ItemStack cursorItem = getCursorItem();
        if (ClientUtils.isClientGridSlot(hoveredSlot) && !cursorItem.isEmpty()) {

        } else {
            if (!((Object) this instanceof CreativeModeInventoryScreen)) {
                hoveredSlot = ClientUtils.getMappedSlot(hoveredSlot);
            }
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

    @Unique
    private boolean firstClicked = true;

    @Inject(method = "mouseReleased",
            at = @At("HEAD"),
            cancellable = true)
    private void onReleased(double mouseX, double mouseY, int button,
                            CallbackInfoReturnable<Boolean> cir) {

        /* ---------- 原“首次点击”保护逻辑 ---------- */
        if (firstClicked) {
            cir.setReturnValue(true);
            firstClicked = false;
            return;
        }

        Slot slot = findSlot(mouseX, mouseY);
        ItemStack cursorItem = getCursorItem();

        /* ====================== 右键放置 ====================== */
        if (button == 1 && !cursorItem.isEmpty()
                && slot != null && ClientUtils.isClientGridSlot(slot)) {

            ContainerGrid grid   = ClientUtils.getContainerGrid();
            ContainerGrid.Cell cell = grid.getCell(slot);
            if (cell == null) {          // 找不到单元格 → 禁止
                cir.setReturnValue(true);
                return;
            }

            Area area = getRotatedArea(cursorItem);
            Set<ContainerGrid.Cell> targetCells = grid.getCells(cell, area);

            /* 1. 区域必须完整（边缘越界直接失败） */
            if (targetCells.size() != area.width() * area.height()) {
                cir.setReturnValue(true);
                return;
            }

            /* 2. 同容器 */
            boolean sameContainer = targetCells.stream()
                    .allMatch(c -> c.slot().container.equals(cell.slot().container));
            if (!sameContainer) {
                cir.setReturnValue(true);
                return;
            }

            /* 3. 空或同种且可堆叠 */
            boolean canStack = targetCells.stream().allMatch(c -> {
                ItemStack s = c.slot().getItem();
                return s.isEmpty()
                        || (ItemStack.isSameItemSameTags(s, cursorItem)
                        && s.getCount() < s.getMaxStackSize());
            });
            if (!canStack) {
                cir.setReturnValue(true);
                return;
            }

            /* 4. 不被其它大件占用 */
            Map<ContainerGrid.Cell, ContainerGrid.Cell> cellMap = grid.getCellMap();
            boolean blocked = targetCells.stream()
                    .anyMatch(c -> {
                        ContainerGrid.Cell owner = cellMap.get(c);
                        return owner != null && !owner.equals(cell);
                    });
            if (blocked) {
                cir.setReturnValue(true);
                return;
            }
            /* 全部通过 → 放行，让后续逻辑真正放置 */
            return;
        }

        /* ====================== 以下为原左键逻辑，保持不变 ====================== */
        if (button == 0 && !cursorItem.isEmpty() && ClientUtils.isClientGridSlot(slot)
                && needReplaceSlot == null) {

            ContainerGrid grid = ClientUtils.getContainerGrid();
            ContainerGrid.Cell clickedCell = grid.getCell(slot);
            if (clickedCell == null) {
                cir.setReturnValue(true);
                return;
            }

            Area area = getRotatedArea(cursorItem);
            Set<ContainerGrid.Cell> targetCells = grid.getCells(clickedCell, area);

            /* 1. 必须同容器 */
            boolean sameContainer = targetCells.stream()
                    .allMatch(c -> c.slot().container.equals(clickedCell.slot().container));
            if (!sameContainer) {
                cir.setReturnValue(true);
                return;
            }

            /* 2. 是否同种物品 */
            boolean sameKind = targetCells.stream()
                    .anyMatch(c -> {
                        ItemStack s = c.slot().getItem();
                        return !s.isEmpty() && ItemStack.isSameItemSameTags(s, cursorItem);
                    });

            if (sameKind) {
                /* ===== 同种物品：仅左上角可堆叠 ===== */
                ContainerGrid.Cell topLeft = targetCells.stream()
                        .min(java.util.Comparator
                                .comparingInt(ContainerGrid.Cell::x)
                                .thenComparingInt(ContainerGrid.Cell::y))
                        .orElse(null);
                if (topLeft == null || !topLeft.equals(clickedCell)) {
                    cir.setReturnValue(true);
                    return;
                }

                /* 3. 全区域可堆叠检测 */
                boolean canStack = targetCells.stream().allMatch(c -> {
                    ItemStack s = c.slot().getItem();
                    return s.isEmpty()
                            || (ItemStack.isSameItemSameTags(s, cursorItem)
                            && s.getCount() < s.getMaxStackSize());
                });
                if (!canStack) {
                    cir.setReturnValue(true);
                    return;
                }

                /* 4. 不被其它大件占用 */
                Map<ContainerGrid.Cell, ContainerGrid.Cell> cellMap = grid.getCellMap();
                boolean blocked = targetCells.stream()
                        .anyMatch(c -> {
                            ContainerGrid.Cell owner = cellMap.get(c);
                            return owner != null && !owner.equals(clickedCell);
                        });
                if (blocked) {
                    cir.setReturnValue(true);
                    return;
                }
                /* 通过检测 → 放行，让原版堆叠 */
                return;
            }

            /* ===== 不同物品：走原替换逻辑 ===== */
            Area targetArea = Area.of(cursorItem);
            Set<ContainerGrid.Cell> targetAreaCells = new HashSet<>();
            for (ContainerGrid.Cell cell : grid.getCells(clickedCell, targetArea)) {
                if (cell.slot().container.equals(clickedCell.slot().container)) {
                    targetAreaCells.add(cell);
                }
            }

            if (targetAreaCells.size() == targetArea.width() * targetArea.height()) {
                Set<ContainerGrid.Cell> areaCells = new HashSet<>();
                Map<ContainerGrid.Cell, ContainerGrid.Cell> cellMap = grid.getCellMap();
                for (ContainerGrid.Cell c : targetAreaCells) {
                    ContainerGrid.Cell mapped = cellMap.get(c);
                    if (mapped != null) areaCells.add(mapped);
                }

                if (areaCells.isEmpty()) return;
                else if (areaCells.size() == 1) {
                    needReplaceSlot = areaCells.iterator().next().slot();
                } else {
                    cir.setReturnValue(true);
                }
            } else {
                cir.setReturnValue(true);
            }
        }

        /* ---------- 原有左键“替换/交换”逻辑 ---------- */
        if (cursorItem != null && ClientUtils.isClientGridSlot(slot)
                && needReplaceSlot == null) {

            ContainerGrid grid = ClientUtils.getContainerGrid();
            if (grid.getCell(slot) == null) return;

            var cellMap = grid.getCellMap();
            ContainerGrid.Cell clickedCell = grid.getCell(slot);

            Area targetArea = Area.of(cursorItem);
            Set<ContainerGrid.Cell> targetAreaCells = new HashSet<>();
            for (ContainerGrid.Cell cell : grid.getCells(clickedCell, targetArea)) {
                if (cell.slot().container.equals(clickedCell.slot().container)) {
                    targetAreaCells.add(cell);
                }
            }

            if (targetAreaCells.size() == targetArea.width() * targetArea.height()) {
                /* 0. 同种物品提前拦截：只要区域里出现同种物品就禁止左键放置 */
                boolean anySameItem = targetAreaCells.stream()
                        .map(c -> c.slot().getItem())
                        .anyMatch(s -> !s.isEmpty() && ItemStack.isSameItemSameTags(s, cursorItem));
                if (anySameItem) {
                    cir.setReturnValue(true);
                    return;
                }

                Set<ContainerGrid.Cell> areaCells = new HashSet<>();
                for (ContainerGrid.Cell c : targetAreaCells) {
                    ContainerGrid.Cell mapped = cellMap.get(c);
                    if (mapped != null) areaCells.add(mapped);
                }

                if (areaCells.isEmpty()) return;
                else if (areaCells.size() == 1) {
                    needReplaceSlot = areaCells.iterator().next().slot();
                } else {
                    cir.setReturnValue(true);
                }
            } else {
                cir.setReturnValue(true);
            }
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

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void cancel(double p_97752_, double p_97753_, int p_97754_, double p_97755_, double p_97756_, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = this.findSlot(p_97752_, p_97753_);
        if (ClientUtils.isClientGridSlot(slot)) cir.setReturnValue(false);
    }

    @Inject(method = "renderSlot", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void scale(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (!ClientUtils.isClientGridSlot(slot)) return;
        scale(guiGraphics, Area.of(slot.getItem()), slot.x, slot.y);
    }
    @Inject(method = "renderFloatingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private void scale(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, String p_282568_, CallbackInfo ci) {
        if (!ClientUtils.isClientGridSlot(hoveredSlot)) return;
        scale(guiGraphics, getRotatedArea(itemStack), x, y);
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

    @Unique
    private Area getRotatedArea(ItemStack stack) {
        return Area.of(stack); // ✅ 这里已经内部读取了 NBT 旋转状态
    }

    @Inject(method = "checkHotbarKeyPressed", at = @At("HEAD"), cancellable = true)
    private void cancel(int p_97806_, int p_97807_, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
    }
}
