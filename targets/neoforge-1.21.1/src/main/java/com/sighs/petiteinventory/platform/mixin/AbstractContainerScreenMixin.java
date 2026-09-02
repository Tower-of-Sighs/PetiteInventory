package com.sighs.petiteinventory.platform.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.core.ItemSize;
import com.sighs.petiteinventory.core.ResizeBounds;
import com.sighs.petiteinventory.core.ResizeDirection;
import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.inventory.BorderTheme;
import com.sighs.petiteinventory.platform.inventory.ContainerGrid;
import com.sighs.petiteinventory.config.BorderThemeCache;
import com.sighs.petiteinventory.platform.NetworkChannel;
import com.sighs.petiteinventory.platform.PlaceItemPayload;
import com.sighs.petiteinventory.client.ClientInventoryContext;
import com.sighs.petiteinventory.client.ClientEditMode;
import com.sighs.petiteinventory.client.InventoryRenderer;
import com.sighs.petiteinventory.config.ItemSizeRuleCache;
import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import net.minecraft.core.registries.BuiltInRegistries;
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

    @Shadow protected AbstractContainerMenu menu;

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
        if (!ClientInventoryContext.isClientGridSlot(slot)) return;
        if (!slot.hasItem()) return;

        Area area = ItemInventoryService.getArea(slot.getItem());
        int x = slot.x;
        int y = slot.y;
        int w = 18 * area.width();
        int h = 18 * area.height();

// ✅ 修正：传入slot.getItem()而不是只传Item类型
        BorderTheme theme = BorderThemeCache.getTheme(slot.getItem().getItem(), slot.getItem());

        InventoryRenderer.drawNinePatch(guiGraphics, theme, x - 1, y - 1, w, h, 18, 1);
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
        if (ClientInventoryContext.isClientGridSlot(hoveredSlot) && !cursorItem.isEmpty()) {
            Area area = getRotatedArea(cursorItem);
            ContainerGrid grid = ClientInventoryContext.getContainerGrid();
            ContainerGrid.Cell hoverCell = grid.getCell(hoveredSlot);
            for (ContainerGrid.Cell cell : grid.getCells(hoverCell, area)) {
                if (cell.slot().container.equals(hoverCell.slot().container)) {
                    AbstractContainerScreen.renderSlotHighlight(guiGraphics, cell.slot().x + leftPos, cell.slot().y + topPos, 0, -2130706433);
                }
            }
        }
    }

    @Redirect(method = "renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"), remap = false)
    private void ond(GuiGraphics guiGraphics, int x, int y, int p_283504_, int color) {
        int w = 16, h = 16;
        ItemStack cursorItem = getCursorItem();
        if (ClientInventoryContext.isClientGridSlot(hoveredSlot) && !cursorItem.isEmpty()) {

        } else {
            if (!((Object) this instanceof CreativeModeInventoryScreen)) {
                hoveredSlot = ClientInventoryContext.getMappedSlot(hoveredSlot);
            }
            ItemStack hoverItem = hoveredSlot.getItem();
            if (!hoverItem.isEmpty() && ClientInventoryContext.isClientGridSlot(hoveredSlot)) {
                Area area = ItemInventoryService.getArea(hoverItem);
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

    @Unique private Slot resizeSlot;
    @Unique private int resizeDirection = -1;
    @Unique private int resizeStartX, resizeStartY, resizeWidth, resizeHeight;
    @Unique private final Set<Slot> selectedSlots = new LinkedHashSet<>();
    @Unique private Slot selectionAnchor;
    @Unique private ContainerGrid.Cell selectionStartCell;
    @Unique private boolean selectionDragging;
    @Unique private boolean selectionAdditive;
    @Unique private final Set<Slot> colorMenuTargets = new LinkedHashSet<>();
    @Unique private int colorMenuX, colorMenuY;
    @Unique private boolean consumeColorMenuRelease;
    @Unique private Slot pendingRightSlot;
    @Unique private final Set<Slot> pendingRightTargets = new LinkedHashSet<>();
    @Unique private ContainerGrid.Cell batchStartCell;
    @Unique private ContainerGrid.Cell batchEndCell;
    @Unique private long rightPressStartedAt;
    @Unique private boolean batchSizing;

    @Inject(method = "render", at = @At("RETURN"))
    private void renderColorPalette(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (colorMenuTargets.isEmpty() || !ClientEditMode.isEnabled()) return;

        BorderTheme[] themes = BorderTheme.values();
        int paletteWidth = 12;
        int entryHeight = 10;
        int paletteHeight = themes.length * entryHeight + 2;
        int x = Math.max(0, Math.min(colorMenuX, width - paletteWidth));
        int y = Math.max(0, Math.min(colorMenuY, height - paletteHeight));
        Slot previewSlot = colorMenuTargets.iterator().next();
        BorderTheme current = BorderThemeCache.getTheme(previewSlot.getItem().getItem(), previewSlot.getItem());

        graphics.fill(x, y, x + paletteWidth, y + paletteHeight, 0xFF202020);
        for (int index = 0; index < themes.length; index++) {
            BorderTheme theme = themes[index];
            int swatchY = y + 1 + index * entryHeight;
            int color = 0xFF000000
                    | ((int) (theme.getR() * 255) << 16)
                    | ((int) (theme.getG() * 255) << 8)
                    | (int) (theme.getB() * 255);
            graphics.fill(x + 2, swatchY + 1, x + paletteWidth - 2, swatchY + entryHeight - 1, color);
            if (theme == current) {
                graphics.fill(x + 1, swatchY, x + paletteWidth - 1, swatchY + 1, 0xFFFFFFFF);
                graphics.fill(x + 1, swatchY + entryHeight - 1, x + paletteWidth - 1, swatchY + entryHeight, 0xFFFFFFFF);
                graphics.fill(x + 1, swatchY, x + 2, swatchY + entryHeight, 0xFFFFFFFF);
                graphics.fill(x + paletteWidth - 2, swatchY, x + paletteWidth - 1, swatchY + entryHeight, 0xFFFFFFFF);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void handleColorPaletteClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (!ClientEditMode.isEnabled()) return;

        if (!colorMenuTargets.isEmpty()) {
            if (button == 0 && selectPaletteTheme(mouseX, mouseY)) {
                consumeColorMenuRelease = true;
                callback.setReturnValue(true);
                return;
            }
            colorMenuTargets.clear();
            if (button != 1) {
                consumeColorMenuRelease = true;
                callback.setReturnValue(true);
                return;
            }
        }

        if (button == 0) return;
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void consumeColorPaletteRelease(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (!consumeColorMenuRelease) return;
        consumeColorMenuRelease = false;
        callback.setReturnValue(true);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void renderSelectedItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (!ClientEditMode.isEnabled()) return;
        ContainerGrid grid = ClientInventoryContext.getContainerGrid();
        for (Slot slot : selectedSlots) {
            if (!slot.hasItem()) continue;
            ContainerGrid.Cell origin = grid.getCell(slot);
            if (origin == null) continue;
            Area area = ItemInventoryService.getArea(slot.getItem());
            for (ContainerGrid.Cell cell : grid.getCells(origin, area)) {
                if (cell.slot().container.equals(slot.container)) {
                    AbstractContainerScreen.renderSlotHighlight(graphics, cell.slot().x + leftPos, cell.slot().y + topPos, 0, -2130706433);
                }
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void beginRightEditorAction(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (!ClientEditMode.isEnabled() || button != 1 || !colorMenuTargets.isEmpty()) return;
        Slot slot = findEditorItemSlot(mouseX, mouseY);
        if (slot == null) return;

        pendingRightSlot = slot;
        pendingRightTargets.clear();
        if (selectedSlots.contains(slot) && !selectedSlots.isEmpty()) pendingRightTargets.addAll(selectedSlots);
        else pendingRightTargets.add(slot);
        rightPressStartedAt = System.currentTimeMillis();
        batchStartCell = findGridCell(mouseX, mouseY);
        batchEndCell = batchStartCell;
        batchSizing = false;
        callback.setReturnValue(true);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void beginBatchSizingAfterHold(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (pendingRightSlot != null && !batchSizing && System.currentTimeMillis() - rightPressStartedAt >= 500L) {
            batchSizing = batchStartCell != null;
        }
        if (!batchSizing || batchStartCell == null) return;
        ContainerGrid.Cell currentCell = findGridCell(mouseX, mouseY);
        if (currentCell != null && currentCell.slot().container.equals(batchStartCell.slot().container)) batchEndCell = currentCell;
        drawRedFrame(graphics, batchStartCell, batchEndCell);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void dragBatchSizing(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> callback) {
        if (!batchSizing || button != 1) return;
        ContainerGrid.Cell currentCell = findGridCell(mouseX, mouseY);
        if (currentCell != null && currentCell.slot().container.equals(batchStartCell.slot().container)) batchEndCell = currentCell;
        callback.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void finishRightEditorAction(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (pendingRightSlot == null || button != 1) return;

        if (batchSizing && batchStartCell != null && batchEndCell != null) {
            int width = Math.abs(batchEndCell.x() - batchStartCell.x()) + 1;
            int height = Math.abs(batchEndCell.y() - batchStartCell.y()) + 1;
            for (Slot slot : pendingRightTargets) {
                String itemId = BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).toString();
                ItemSizeRuleCache.setSizeByCommand(itemId, width + "*" + height);
            }
        } else {
            colorMenuTargets.clear();
            colorMenuTargets.addAll(pendingRightTargets);
            colorMenuX = (int) mouseX + 8;
            colorMenuY = (int) mouseY + 8;
        }

        pendingRightSlot = null;
        pendingRightTargets.clear();
        batchStartCell = null;
        batchEndCell = null;
        batchSizing = false;
        callback.setReturnValue(true);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void renderEditorHandles(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (!ClientEditMode.isEnabled()) return;
        Slot slot = findResizeSlot(mouseX, mouseY);
        if (slot == null) return;
        Area area = ItemInventoryService.getArea(slot.getItem());
        // Keep this rectangle exactly aligned with the normal nine-patch item border.
        int x = leftPos + slot.x - 1, y = topPos + slot.y - 1;
        int w = area.width() * 18, h = area.height() * 18;
        int red = 0xFFFF3030;
        graphics.fill(x, y, x + w, y + 1, red);
        graphics.fill(x, y + h - 1, x + w, y + h, red);
        graphics.fill(x, y, x + 1, y + h, red);
        graphics.fill(x + w - 1, y, x + w, y + h, red);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void beginResize(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (!ClientEditMode.isEnabled() || button != 0) return;
        Slot slot = findResizeSlot(mouseX, mouseY);
        if (slot == null) return;
        Area area = ItemInventoryService.getArea(slot.getItem());
        resizeDirection = getResizeDirection(slot, mouseX, mouseY);
        resizeSlot = slot; resizeStartX = (int) mouseX; resizeStartY = (int) mouseY; resizeWidth = area.width(); resizeHeight = area.height();
        callback.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void resize(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> callback) {
        if (resizeSlot == null) return;
        int dx = Math.round(((int) mouseX - resizeStartX) / 18.0f), dy = Math.round(((int) mouseY - resizeStartY) / 18.0f);
        int[] size = resize(resizeWidth, resizeHeight, resizeDirection, dx, dy);
        String id = BuiltInRegistries.ITEM.getKey(resizeSlot.getItem().getItem()).toString();
        ItemSizeRuleCache.setSizeByCommand(id, size[0] + "*" + size[1]);
        callback.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void endResize(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (resizeSlot == null) return;
        resizeSlot = null; resizeDirection = -1; callback.setReturnValue(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void beginSelection(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (!ClientEditMode.isEnabled() || button != 0 || !Screen.hasControlDown()
                || !colorMenuTargets.isEmpty()) return;
        ContainerGrid.Cell cell = findGridCell(mouseX, mouseY);
        if (cell == null) return;

        boolean additive = Screen.hasControlDown();
        Slot slot = findEditorItemSlot(mouseX, mouseY);
        ContainerGrid.Cell anchorCell = selectionAnchor == null ? null : ClientInventoryContext.getContainerGrid().getCell(selectionAnchor);
        if (Screen.hasShiftDown() && anchorCell != null) {
            selectGridRegion(anchorCell, cell, additive);
        } else if (slot != null) {
            if (additive && selectedSlots.contains(slot)) selectedSlots.remove(slot);
            else {
                if (!additive) selectedSlots.clear();
                selectedSlots.add(slot);
            }
            selectionAnchor = slot;
        } else if (!additive) {
            selectedSlots.clear();
            selectionAnchor = null;
        }

        selectionStartCell = cell;
        selectionAdditive = additive;
        selectionDragging = true;
        callback.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void dragSelection(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> callback) {
        if (!selectionDragging || button != 0 || selectionStartCell == null) return;
        ContainerGrid.Cell currentCell = findGridCell(mouseX, mouseY);
        if (currentCell != null && currentCell.slot().container.equals(selectionStartCell.slot().container)) {
            selectGridRegion(selectionStartCell, currentCell, selectionAdditive);
        }
        callback.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void endSelection(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (!selectionDragging || button != 0) return;
        selectionDragging = false;
        selectionStartCell = null;
        callback.setReturnValue(true);
    }

    @Unique
    private int resizeDirection(int x, int y, int width, int height) {
        boolean west = x <= 3, east = x >= width - 3, north = y <= 3, south = y >= height - 3;
        if (north && west) return 0; if (north && east) return 1;
        if (south && west) return 2; if (south && east) return 3;
        if (north) return 4; if (south) return 5; if (west) return 6; if (east) return 7;
        return -1;
    }

    @Unique
    private Slot findResizeSlot(double mouseX, double mouseY) {
        ContainerGrid grid = ClientInventoryContext.getContainerGrid();
        for (ContainerGrid.Cell cell : grid.getCells()) {
            Slot slot = cell.slot();
            if (!slot.hasItem()) continue;
            if (getResizeDirection(slot, mouseX, mouseY) != -1) return slot;
        }
        return null;
    }

    @Unique
    private Slot findEditorItemSlot(double mouseX, double mouseY) {
        ContainerGrid grid = ClientInventoryContext.getContainerGrid();
        for (ContainerGrid.Cell cell : grid.getCells()) {
            Slot slot = cell.slot();
            if (!slot.hasItem()) continue;
            Area area = ItemInventoryService.getArea(slot.getItem());
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            if (mouseX >= x && mouseX < x + area.width() * 18
                    && mouseY >= y && mouseY < y + area.height() * 18) return slot;
        }
        return null;
    }

    @Unique
    private ContainerGrid.Cell findGridCell(double mouseX, double mouseY) {
        ContainerGrid grid = ClientInventoryContext.getContainerGrid();
        for (ContainerGrid.Cell cell : grid.getCells()) {
            int x = leftPos + cell.slot().x;
            int y = topPos + cell.slot().y;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) return cell;
        }
        return null;
    }

    @Unique
    private void selectGridRegion(ContainerGrid.Cell start, ContainerGrid.Cell end, boolean additive) {
        if (!additive) selectedSlots.clear();
        int minX = Math.min(start.x(), end.x());
        int maxX = Math.max(start.x(), end.x());
        int minY = Math.min(start.y(), end.y());
        int maxY = Math.max(start.y(), end.y());
        ContainerGrid grid = ClientInventoryContext.getContainerGrid();
        Map<ContainerGrid.Cell, ContainerGrid.Cell> owners = grid.getCellMap();
        for (ContainerGrid.Cell cell : grid.getCells()) {
            if (cell.x() < minX || cell.x() > maxX || cell.y() < minY || cell.y() > maxY) continue;
            if (!cell.slot().container.equals(start.slot().container)) continue;
            ContainerGrid.Cell owner = owners.get(cell);
            if (owner != null && owner.slot().hasItem()) selectedSlots.add(owner.slot());
        }
        selectionAnchor = start.slot();
    }

    @Unique
    private void drawRedFrame(GuiGraphics graphics, ContainerGrid.Cell start, ContainerGrid.Cell end) {
        if (start == null || end == null) return;
        int minX = Math.min(start.slot().x, end.slot().x);
        int maxX = Math.max(start.slot().x, end.slot().x);
        int minY = Math.min(start.slot().y, end.slot().y);
        int maxY = Math.max(start.slot().y, end.slot().y);
        int x = leftPos + minX - 1;
        int y = topPos + minY - 1;
        int width = maxX - minX + 18;
        int height = maxY - minY + 18;
        int red = 0xFFFF3030;
        graphics.fill(x, y, x + width, y + 1, red);
        graphics.fill(x, y + height - 1, x + width, y + height, red);
        graphics.fill(x, y, x + 1, y + height, red);
        graphics.fill(x + width - 1, y, x + width, y + height, red);
    }

    @Unique
    private boolean selectPaletteTheme(double mouseX, double mouseY) {
        int paletteWidth = 12;
        int entryHeight = 10;
        int paletteHeight = BorderTheme.values().length * entryHeight + 2;
        int x = Math.max(0, Math.min(colorMenuX, width - paletteWidth));
        int y = Math.max(0, Math.min(colorMenuY, height - paletteHeight));
        if (mouseX < x || mouseX >= x + paletteWidth || mouseY < y + 1 || mouseY >= y + paletteHeight - 1) return false;

        int index = ((int) mouseY - y - 1) / entryHeight;
        BorderTheme[] themes = BorderTheme.values();
        if (index < 0 || index >= themes.length) return false;

        for (Slot slot : colorMenuTargets) {
            String itemId = BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).toString();
            BorderThemeCache.setTheme(itemId, themes[index]);
        }
        colorMenuTargets.clear();
        return true;
    }

    @Unique
    private int getResizeDirection(Slot slot, double mouseX, double mouseY) {
        Area area = ItemInventoryService.getArea(slot.getItem());
        int width = area.width() * 18;
        int height = area.height() * 18;
        int localX = (int) mouseX - leftPos - slot.x;
        int localY = (int) mouseY - topPos - slot.y;
        if (localX < 0 || localY < 0 || localX >= width || localY >= height) return -1;
        return resizeDirection(localX, localY, width, height);
    }

    @Unique
    private int[] resize(int width, int height, int direction, int dx, int dy) {
        if (direction < 0 || direction > 7) return new int[] {width, height};
        ResizeDirection[] directions = {
                ResizeDirection.NORTH_WEST, ResizeDirection.NORTH_EAST,
                ResizeDirection.SOUTH_WEST, ResizeDirection.SOUTH_EAST,
                ResizeDirection.NORTH, ResizeDirection.SOUTH,
                ResizeDirection.WEST, ResizeDirection.EAST
        };
        ItemSize resized = ResizeBounds.resize(new ItemSize(width, height),
                directions[direction], dx, dy, 9);
        return new int[] {resized.width(), resized.height()};
    }

    /**
     * Vanilla commits a pickup action from mouseClicked, before mouseReleased
     * runs. Reject an invalid footprint at the earlier event so a failed right
     * click cannot place one item into an otherwise blocked multi-slot area.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void preventInvalidRightPlacement(double mouseX, double mouseY, int button,
                                               CallbackInfoReturnable<Boolean> callback) {
        if (ClientEditMode.isEnabled() || button != 1) return;

        ItemStack cursorItem = getCursorItem();
        if (cursorItem.isEmpty()) return;

        Slot slot = findSlot(mouseX, mouseY);
        if (!ClientInventoryContext.isClientGridSlot(slot)) return;

        if (!canPlaceCursorItem(slot, cursorItem)) {
            callback.setReturnValue(true);
        }
    }

    @Unique
    private boolean canPlaceCursorItem(Slot slot, ItemStack cursorItem) {
        if (slot == null || cursorItem.isEmpty()) return false;

        ContainerGrid grid = ClientInventoryContext.getContainerGrid();
        ContainerGrid.Cell cell = grid.getCell(slot);
        if (cell == null) return false;

        Area area = getRotatedArea(cursorItem);
        Set<ContainerGrid.Cell> targetCells = grid.getCells(cell, area);
        if (targetCells.size() != area.width() * area.height()) return false;

        Map<ContainerGrid.Cell, ContainerGrid.Cell> cellMap = grid.getCellMap();
        for (ContainerGrid.Cell target : targetCells) {
            if (!target.slot().container.equals(cell.slot().container)
                    || !target.slot().mayPlace(cursorItem)) {
                return false;
            }

            ItemStack existing = target.slot().getItem();
            boolean stackable = existing.isEmpty()
                    || (ItemStack.isSameItemSameComponents(existing, cursorItem)
                    && existing.getCount() < existing.getMaxStackSize());
            if (!stackable) return false;

            ContainerGrid.Cell owner = cellMap.get(target);
            if (owner != null && !owner.equals(cell)) return false;
        }
        return true;
    }

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
                && slot != null && ClientInventoryContext.isClientGridSlot(slot)) {

            ContainerGrid grid   = ClientInventoryContext.getContainerGrid();
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
                        || (ItemStack.isSameItemSameComponents(s, cursorItem)
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
        if (button == 0 && !cursorItem.isEmpty() && ClientInventoryContext.isClientGridSlot(slot)
                && needReplaceSlot == null) {

            ContainerGrid grid = ClientInventoryContext.getContainerGrid();
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
                        return !s.isEmpty() && ItemStack.isSameItemSameComponents(s, cursorItem);
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
                            || (ItemStack.isSameItemSameComponents(s, cursorItem)
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
            Area targetArea = ItemInventoryService.getArea(cursorItem);
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
        if (cursorItem != null && ClientInventoryContext.isClientGridSlot(slot)
                && needReplaceSlot == null) {

            ContainerGrid grid = ClientInventoryContext.getContainerGrid();
            if (grid.getCell(slot) == null) return;

            var cellMap = grid.getCellMap();
            ContainerGrid.Cell clickedCell = grid.getCell(slot);

            Area targetArea = ItemInventoryService.getArea(cursorItem);
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
                        .anyMatch(s -> !s.isEmpty() && ItemStack.isSameItemSameComponents(s, cursorItem));
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
        if (needReplaceSlot != null && ClientInventoryContext.isClientGridSlot(slot)) {
            int offsetX = needReplaceSlot.x - slot.x;
            int offsetY = needReplaceSlot.y - slot.y;
            needReplaceSlot = null;
            mouseClicked(mouseX + offsetX, mouseY + offsetY, p_97814_);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void cancel(double p_97752_, double p_97753_, int p_97754_, double p_97755_, double p_97756_, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = this.findSlot(p_97752_, p_97753_);
        if (ClientInventoryContext.isClientGridSlot(slot)) cir.setReturnValue(false);
    }

    @Inject(method = "renderSlot", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void scale(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (!ClientInventoryContext.isClientGridSlot(slot)) return;
        scale(guiGraphics, ItemInventoryService.getArea(slot.getItem()), slot.x, slot.y);
    }
    @Inject(method = "renderFloatingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private void scale(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, String p_282568_, CallbackInfo ci) {
        if (!ClientInventoryContext.isClientGridSlot(hoveredSlot)) return;
        scale(guiGraphics, getRotatedArea(itemStack), x, y);
    }

    private void scale(GuiGraphics guiGraphics, Area area, int x, int y) {
        int w = area.width(), h = area.height();
        float minSize = area.minSize();
        float scale = minSize > 1 ? minSize * 0.8F : 1.0F;
        float renderedSize = 16 * scale;
        float offsetX = (w * 18 - 2 - renderedSize) / 2.0F;
        float offsetY = (h * 18 - 2 - renderedSize) / 2.0F;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.translate(x + offsetX, y + offsetY, 0);
        poseStack.scale(scale, scale, 1.0f);
        poseStack.translate(-x, -y, 0);
    }

    private ItemStack getCursorItem() {
        return this.draggingItem.isEmpty() ? menu.getCarried() : this.draggingItem;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void doTick(CallbackInfo ci) {
        ItemStack cursorItem = getCursorItem();
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        if (!cursorItem.isEmpty() && ItemInventoryService.getArea(cursorItem).maxSize() > 1 && ClientInventoryContext.isClientGridSlot(hoveredSlot)) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        } else GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    @Unique
    private Area getRotatedArea(ItemStack stack) {
        return ItemInventoryService.getArea(stack); // ✅ 这里已经内部读取了 NBT 旋转状态
    }

    @Inject(method = "checkHotbarKeyPressed", at = @At("HEAD"), cancellable = true)
    private void cancel(int p_97806_, int p_97807_, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
    }
}
