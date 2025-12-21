package com.sighs.petiteinventory.event;

import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.network.PacketHandler;
import com.sighs.petiteinventory.network.RotateAreaPayload;
import com.sighs.petiteinventory.registry.ModKeybindings;
import com.sighs.petiteinventory.utils.ItemUtils;
import com.sighs.petiteinventory.utils.OperateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Petiteinventory.MODID, value = Dist.CLIENT)
public class KeyInput {

    private static long lastR = 0;

    @SubscribeEvent
    public static void copy(ScreenEvent.KeyReleased event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        int key = event.getKeyCode();

        /* --------------- 原来能工作的 U --------------- */
        if (key == ModKeybindings.KEY.getKey().getValue()) {   // U
            if (screen.hoveredSlot == null) return;
            String menuType = screen.getMenu().getClass().toString();
            SystemToast.add(
                    Minecraft.getInstance().getToasts(),
                    SystemToast.SystemToastIds.TUTORIAL_HINT,
                    Component.translatable("toast.petiteinventory.copied.title"),
                    Component.translatable("toast.petiteinventory.copied.detail")
            );
            Minecraft.getInstance().keyboardHandler.setClipboard(menuType);
            return;                       // 注意 return，别走到下面
        }

        /* --------------- 直接复用同一段事件 --------------- */

        /* ---------------  R 键旋转 --------------- */
        if (key == GLFW.GLFW_KEY_R) {
            long now = System.currentTimeMillis();
            if (now - lastR < 150) return;
            lastR = now;

            ItemStack carried = Minecraft.getInstance().player.containerMenu.getCarried();
            if (carried.isEmpty()) return;

            boolean nowRot = !ItemUtils.ItemRotateHelper.isRotated(carried);
            ItemUtils.ItemRotateHelper.setRotated(carried, nowRot);

            // -1 表示鼠标上的物品
            PacketHandler.CHANNEL.sendToServer(new RotateAreaPayload(-1, nowRot));
        }
    }
}