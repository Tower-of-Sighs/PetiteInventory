package com.sighs.petiteinventory.client;


import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import com.sighs.petiteinventory.Petiteinventory;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = Petiteinventory.MODID)
public class ModKeybindings {
    public static final KeyMapping KEY = new KeyMapping("key.petiteinventory.copy_container_class",
            KeyConflictContext.GUI,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.categories.petiteinventory"
    );

    public static final KeyMapping ROTATE = new KeyMapping(
            "key.petiteinventory.rotate",
            KeyConflictContext.GUI,        // 只在 GUI 里响应
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,               // ← 这里就是 R
            "key.categories.petiteinventory"
    );

    @SubscribeEvent
    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(KEY);
        event.register(ROTATE);
    }
}
