package com.sighs.petiteinventory.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Per-container-screen client preference for the Petite layout. */
public final class ScreenLayoutSettings {
    /** New installations start in whitelist mode: no foreign container is enabled. */
    private static final boolean DEFAULT_ENABLED = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();
    private static final Path FILE = FMLPaths.CONFIGDIR.get()
            .resolve("PetiteInventory")
            .resolve("screen_layouts.json");
    private static final Path MODE_FILE = FMLPaths.CONFIGDIR.get()
            .resolve("PetiteInventory")
            .resolve("screen_layout_mode.json");
    private static final Map<String, Boolean> ENABLED_BY_SCREEN = load();
    private static boolean defaultEnabled = loadDefaultEnabled();

    private ScreenLayoutSettings() {
    }

    public static boolean isEnabled(Screen screen) {
        return screen != null && ENABLED_BY_SCREEN.getOrDefault(screen.getClass().getName(), defaultEnabled);
    }

    public static void setEnabled(Screen screen, boolean enabled) {
        if (screen == null) return;
        ENABLED_BY_SCREEN.put(screen.getClass().getName(), enabled);
        save();
    }

    public static void setDefaultEnabled(boolean enabled) {
        defaultEnabled = enabled;
        saveDefaultEnabled();
    }

    private static Map<String, Boolean> load() {
        if (!Files.exists(FILE)) return new HashMap<>();
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Map<String, Boolean> settings = GSON.fromJson(reader, MAP_TYPE);
            return settings == null ? new HashMap<>() : new HashMap<>(settings);
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(ENABLED_BY_SCREEN, MAP_TYPE), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static boolean loadDefaultEnabled() {
        if (!Files.exists(MODE_FILE)) return DEFAULT_ENABLED;
        try (Reader reader = Files.newBufferedReader(MODE_FILE, StandardCharsets.UTF_8)) {
            Boolean enabled = GSON.fromJson(reader, Boolean.class);
            return enabled != null && enabled;
        } catch (Exception ignored) {
            return DEFAULT_ENABLED;
        }
    }

    private static void saveDefaultEnabled() {
        try {
            Files.createDirectories(MODE_FILE.getParent());
            Files.writeString(MODE_FILE, GSON.toJson(defaultEnabled), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
