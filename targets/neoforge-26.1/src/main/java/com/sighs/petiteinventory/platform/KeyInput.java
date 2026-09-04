package com.sighs.petiteinventory.platform;



import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import com.sighs.petiteinventory.Petiteinventory;
import com.sighs.petiteinventory.platform.NetworkChannel;
import com.sighs.petiteinventory.platform.RotateAreaPayload;
import com.sighs.petiteinventory.client.ModKeybindings;
import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = Petiteinventory.MODID, value = Dist.CLIENT)
public class KeyInput {

    private static long lastR = 0;

    @SubscribeEvent
    public static void copy(ScreenEvent.KeyReleased.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        int key = event.getKeyCode();

        if (key == ModKeybindings.KEY.getKey().getValue()) {
            if (screen.getSlotUnderMouse() == null) return;
            String menuType = screen.getMenu().getClass().toString();
            SystemToast.add(
                    Minecraft.getInstance().getToastManager(),
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.translatable("toast.petiteinventory.copied.title"),
                    Component.translatable("toast.petiteinventory.copied.detail")
            );
            Minecraft.getInstance().keyboardHandler.setClipboard(menuType);
        }

        if (key == ModKeybindings.ROTATE.getKey().getValue()) {
            long now = System.currentTimeMillis();
            if (now - lastR < 150) return;
            lastR = now;

            rotateCarriedItem(screen);
        }
    }

    @SubscribeEvent
    public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (event.getScrollDeltaY() == 0) return;

        if (rotateCarriedItem(screen)) {
            event.setCanceled(true);
        }
    }

    /** Toggle the carried item's footprint orientation and synchronize it with the server. */
    private static boolean rotateCarriedItem(AbstractContainerScreen<?> screen) {
        ItemStack carried = screen.getMenu().getCarried();
        if (carried.isEmpty()) return false;

        Area area = ItemInventoryService.getArea(carried);
        if (area.width() == area.height()) return false;

        boolean rotated = !ItemInventoryService.ItemRotateHelper.isRotated(carried);
        ItemInventoryService.ItemRotateHelper.setRotated(carried, rotated);
        ClientPacketDistributor.sendToServer(new RotateAreaPayload(-1, rotated));
        return true;
    }
}
