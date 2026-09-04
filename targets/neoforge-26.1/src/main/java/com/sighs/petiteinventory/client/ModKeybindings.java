package com.sighs.petiteinventory.client;


import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import com.sighs.petiteinventory.Petiteinventory;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT, modid = Petiteinventory.MODID)
public class ModKeybindings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Petiteinventory.MODID, "petiteinventory"));

    public static final KeyMapping KEY = createKey("key.petiteinventory.copy_container_class", GLFW.GLFW_KEY_U);
    public static final KeyMapping ROTATE = createKey("key.petiteinventory.rotate", GLFW.GLFW_KEY_R);

    private static KeyMapping createKey(String name, int key) {
        KeyMapping mapping = new KeyMapping(name, InputConstants.Type.KEYSYM, key, CATEGORY);
        mapping.setKeyConflictContext(KeyConflictContext.GUI);
        return mapping;
    }

    @SubscribeEvent
    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(KEY);
        event.register(ROTATE);
    }
}