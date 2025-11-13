package com.sighs.petiteinventory.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.init.ContainerGrid;
import com.sighs.petiteinventory.utils.GuiUtils;
import com.sighs.petiteinventory.utils.ItemUtils;
import com.sighs.petiteinventory.utils.OperateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@Mixin(value = AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow @Nullable protected Slot hoveredSlot;

    @Shadow private ItemStack draggingItem;

    @Shadow private ItemStack snapbackItem;

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

    @Redirect(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void selected(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int color) {
        AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
        ItemStack hoverItem = hoveredSlot.getItem();
        System.out.print(hoverItem+"\n");
        int w = 0, h = 0;
        if (!hoverItem.isEmpty()) {
            Area area = Area.of(hoverItem);
            w = 18 * (area.width() - 1);
            h = 18 * (area.height() - 1);
        }
//        guiGraphics.fill(x0, y0, x1 + w, y1 + h, color);
    }

//    @Redirect(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V"))
//    private void modify(GuiGraphics instance, ItemStack itemStack, int x, int y, int p_283435_) {
//
//        renderItem(instance, itemStack, x, y, p_283435_);
//    }
//
//    @Redirect(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
//    private void modify(GuiGraphics instance, Font l, ItemStack i, int x, int y, String i1) {
//        int scale = 1;
//
//        PoseStack poseStack = instance.pose();
//        poseStack.pushPose();
//        scale(poseStack, x, y, 2);
//    }
//
//    private static void renderItem(GuiGraphics instance, ItemStack itemStack, int x, int y, int p_283435_) {
//        if (ItemUtils.isToolOrWeapon(itemStack)) {
//            instance.renderItem(itemStack, x, y + 9, p_283435_);
//        }
//        else if (itemStack.getItem() instanceof BlockItem) {
//            PoseStack poseStack = instance.pose();
//            poseStack.pushPose();
//
//            scale(poseStack, x, y, 2);
//            instance.renderItem(itemStack, x, y, p_283435_);
//
//            poseStack.popPose();
//        }
//        else instance.renderItem(itemStack, x, y, p_283435_);
//    }
//
//    private static void scale(PoseStack poseStack, int x, int y, float scale) {
//        poseStack.translate(x, y, 0);
//        poseStack.scale(scale, scale, 1.0f);
//        poseStack.translate(-x, -y, 0);
//    }

    private void renderAdditionalSlot(GuiGraphics guiGraphics, Slot slot) {
        Pair<ResourceLocation, ResourceLocation> pair = slot.getNoItemIcon();
        if (pair != null) {
            TextureAtlasSprite textureatlassprite = this.minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());
            guiGraphics.blit(slot.x, slot.y, 0, 16, 16, textureatlassprite);
        }
    }
}
