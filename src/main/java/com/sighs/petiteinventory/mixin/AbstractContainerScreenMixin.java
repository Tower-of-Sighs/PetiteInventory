package com.sighs.petiteinventory.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.init.ContainerGrid;
import com.sighs.petiteinventory.utils.ClientUtils;
import com.sighs.petiteinventory.utils.GuiUtils;
import com.sighs.petiteinventory.utils.OperateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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

    @Shadow private ItemStack snapbackItem;

    @Shadow public abstract boolean mouseClicked(double p_97748_, double p_97749_, int p_97750_);

    @Shadow @Nullable protected abstract Slot findSlot(double p_97745_, double p_97746_);

    @Shadow public abstract boolean mouseReleased(double p_97812_, double p_97813_, int p_97814_);

    protected AbstractContainerScreenMixin(Component p_96550_) {
        super(p_96550_);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
        Set<Slot> inventorySlots = new HashSet<>();
        for (int i = 0; i < menu.slots.size() - 9; i++) {
            Slot slot = menu.getSlot(i);

        }
//        for (Slot slot : menu.slots) {
//            if (slot.container instanceof Inventory)
//        }
        OperateUtils.setContainerGrid(ContainerGrid.parse(menu.slots));
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void onRender(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (slot == null) return;
        AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
        ItemStack cursorItem = this.draggingItem.isEmpty() ? menu.getCarried() : this.draggingItem;

        int x = slot.x;
        int y = slot.y;

        ContainerGrid grid = ContainerGrid.parse(menu.slots);
        ContainerGrid.Cell currentCell = grid.getCell(slot);
        ContainerGrid.Cell hoverCell = grid.getCell(hoveredSlot);
        if (!slot.hasItem()) return;
        Area area = Area.of(slot.getItem());
        int w = 18 * area.width();
        int h = 18 * area.height();
        GuiUtils.drawNinePatch(
                guiGraphics, GuiUtils.AREA,
                x - 1, y - 1, w, h,
                18, 1
        );
//        guiGraphics.fill(x, y, x + w, y + h, 0xFF8B8B8B);
//        System.out.print(cursorCell+"\n");
//        if (!cursorItem.isEmpty() && hoverCell != null) {
//            if (ItemUtils.isToolOrWeapon(slot.getItem())) {
////                if (currentCell.x() == hoverCell.x() && currentCell.y() - hoverCell.y() == 1) {
//                    guiGraphics.fill(x, y, x + 16, y + 16 * 2 + 2, 0xFF8B8B8B);
////                }
//            }
//            if (slot.getItem().getItem() instanceof BlockItem) {
//                guiGraphics.fill(x, y, x + 16 * 2 + 2, y + 16 * 2 + 2, 0xFF8B8B8B);
//            }
//        }
    }

    @Inject(method = "findSlot", at = @At("RETURN"), cancellable = true)
    private void redirectFindSlot(double p_97745_, double p_97746_, CallbackInfoReturnable<Slot> cir) {
        cir.setReturnValue(ClientUtils.getMappedSlot(cir.getReturnValue()));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;isHighlightable()Z"))
    private void redirectHoverSlot(GuiGraphics p_283479_, int p_283661_, int p_281248_, float p_281886_, CallbackInfo ci) {
        hoveredSlot = ClientUtils.getMappedSlot(hoveredSlot);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"), remap = false)
    private void ond(GuiGraphics guiGraphics, int x, int y, int p_283504_, int color) {
        ItemStack hoverItem = hoveredSlot.getItem();
        x = hoveredSlot.x;
        y = hoveredSlot.y;
        int w = 16, h = 16;
        if (!hoverItem.isEmpty()) {
            Area area = Area.of(hoverItem);
            w += 18 * (area.width() - 1);
            h += 18 * (area.height() - 1);
        }
        guiGraphics.fillGradient(RenderType.guiOverlay(), x, y, x + w, y + h, color, color, p_283504_);
    }

    @Unique
    private Slot needReplaceSlot = null;

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onClick(double mouseX, double mouseY, int p_97750_, CallbackInfoReturnable<Boolean> cir) {
        // 两种情况成立一种可以替换，两种情况都成立时不能替换，两种。
        // 情况一：
        ItemStack cursorItem = getCursorItem();
        int a = 0;
        Slot slot = findSlot(mouseX, mouseY);
        System.out.print(++a + "\n");
        System.out.print(needReplaceSlot + "\n");
        if (cursorItem != null && slot != null && !slot.hasItem() && needReplaceSlot == null) {
            System.out.print(++a + "\n");

            ContainerGrid grid = ClientUtils.getClientContainerGrid();
            var cellMap = grid.getCellMap();
            ContainerGrid.Cell clickedCell = grid.getCell(slot);

            Area targetArea = Area.of(getCursorItem());
            var targetAreaCells = grid.getCells(clickedCell, targetArea);
            System.out.print(targetAreaCells+"\n");
            if (targetAreaCells.size() == targetArea.width() * targetArea.height()) {
                System.out.print(++a + "\n");
                // 获取到的格子数量和所需格子数一样，说明没过界。
                // 下面说的区域核心格子当然都是指非空，不然每个格子都能是一个区域。

                // 目标区域内所有的区域核心格子
                Set<ContainerGrid.Cell> AreaCells = new HashSet<>();
                for (ContainerGrid.Cell c : targetAreaCells) {
                    ContainerGrid.Cell mapped = cellMap.get(c);
                    if (mapped != null) AreaCells.add(mapped);
                }

                if (AreaCells.isEmpty()) return;
                else if (AreaCells.size() == 1) {
                    System.out.print(++a + "\n");
                    System.out.print(5555555 + "\n");
                    needReplaceSlot = AreaCells.toArray(new ContainerGrid.Cell[]{})[0].slot();
//                    int offsetX = needReplaceSlot.x - slot.x;
//                    int offsetY = needReplaceSlot.y - slot.y;
//                    // 触发需置换区域的核心格子的点击，因为核心格子非空所以不会爆堆栈。
//                    cir.setReturnValue(true);
//                    needReplaceSlot.getItem().copy();
//                    mouseReleased(mouseX + offsetX, mouseY + offsetY, p_97750_);
                }
                else cir.setReturnValue(true);

//                // 把原始格子 -> 对应的区域核心格子，的映射，调整成，区域核心格子 -> 对应的区域内格子们，的映射。
//                Map<ContainerGrid.Cell, Set<ContainerGrid.Cell>> AreaCellMap = new HashMap<>();
//                cellMap.forEach((origin, mapped) -> {
//                    if (AreaCells.contains(mapped)) {
//                        if (!AreaCellMap.containsKey(mapped)) AreaCellMap.put(mapped, new HashSet<>());
//                        var set = AreaCellMap.get(mapped);
//                        set.add(origin);
//                        AreaCellMap.put(mapped, set);
//                    }
//                });
//
//                // 完全在目标区域内的区域，的对应核心格子。
//                List<ContainerGrid.Cell> rangedAreaCells = new ArrayList<>();
//                AreaCellMap.forEach((AreaCell, cells) -> {
//                    boolean inRange = true;
//                    for (ContainerGrid.Cell cell : cells) {
//                        if (!targetAreaCells.contains(cell)) {
//                            inRange = false;
//                            break;
//                        }
//                    }
//                    if (inRange) rangedAreaCells.add(AreaCell);
//                });
            }
            else cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("RETURN"))
    private void qq(double mouseX, double mouseY, int p_97814_, CallbackInfoReturnable<Boolean> cir) {
        ItemStack cursorItem = getCursorItem();
        Slot slot = findSlot(mouseX, mouseY);
        if (slot == null) return;
        if (needReplaceSlot != null) {
            Slot targetSlot = needReplaceSlot;
            needReplaceSlot = null;
            System.out.print("redirect\n");
            int offsetX = targetSlot.x - slot.x;
            int offsetY = targetSlot.y - slot.y;
            // 触发需置换区域的核心格子的点击，因为核心格子非空所以不会爆堆栈。
            mouseReleased(mouseX + offsetX, mouseY + offsetY, p_97814_);
        }
    }

    public ItemStack getCursorItem() {
        AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
        return this.draggingItem.isEmpty() ? menu.getCarried() : this.draggingItem;
    }
}
