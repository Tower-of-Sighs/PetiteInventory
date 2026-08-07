package com.sighs.petiteinventory.config;

import com.sighs.petiteinventory.inventory.BorderTheme;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class BorderThemeCache {
    private static final Map<String, BorderTheme> COLOR_MAP = new HashMap<>();
    private static final Map<String, BorderTheme> TAG_CACHE = new HashMap<>();

    public static void load() {
        COLOR_MAP.clear();
        TAG_CACHE.clear();
        COLOR_MAP.putAll(BorderThemeFileStore.loadColors());
    }

    /* Entry point used when no ItemStack is available. */
    public static BorderTheme getTheme(Item item) {
        return getTheme(item, ItemStack.EMPTY);
    }

    /* Supports exact NBT matching for TaCZ weapons, magazines and attachments. */
    public static BorderTheme getTheme(Item item, ItemStack stack) {
        if (item == null) return BorderTheme.DEFAULT;

        ResourceLocation registryId = ForgeRegistries.ITEMS.getKey(item);
        if (registryId == null) return BorderTheme.DEFAULT;

        String itemId = registryId.toString();

        // 1. Exact NBT rule has priority.
        String preciseKey = getNBTKey(itemId, stack);
        BorderTheme theme = preciseKey == null ? null : COLOR_MAP.get(preciseKey);
        if (theme != null) return theme;

        // 2. Generic item ID rule.
        theme = COLOR_MAP.get(itemId);
        if (theme != null) return theme;

        // 3. Tag rules.
        for (Map.Entry<String, BorderTheme> tagEntry : COLOR_MAP.entrySet()) {
            if (!tagEntry.getKey().startsWith("TAG:")) continue;

            String tagName = tagEntry.getKey().substring(4);
            ResourceLocation tagId = new ResourceLocation(tagName);
            if (ForgeRegistries.ITEMS.tags() != null
                    && ForgeRegistries.ITEMS.tags().getTag(
                    net.minecraft.tags.ItemTags.create(tagId)
            ).contains(item)) {
                TAG_CACHE.put(itemId, tagEntry.getValue());
                return tagEntry.getValue();
            }
        }

        // 4. Cached tag match.
        theme = TAG_CACHE.get(itemId);
        return theme != null ? theme : BorderTheme.DEFAULT;
    }

    private static String getNBTKey(String itemId, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return null;

        var tag = stack.getTag();
        String nbtField;

        if (itemId.equals("taczmagazines:magazine") && tag.contains("MagazineFamily")) {
            nbtField = "MagazineFamily";
        } else if (itemId.equals("tacz:modern_kinetic_gun") && tag.contains("GunId")) {
            nbtField = "GunId";
        } else if (itemId.equals("tacz:attachment") && tag.contains("AttachmentId")) {
            nbtField = "AttachmentId";
        } else {
            return null;
        }

        String value = tag.getString(nbtField);
        if (value == null || value.isEmpty()) return null;

        return itemId + "{" + nbtField + ":\"" + value + "\"}";
    }

    public static void setTheme(String itemId, BorderTheme theme) {
        COLOR_MAP.put(itemId, theme);
        TAG_CACHE.remove(itemId);
        BorderThemeFileStore.saveColors(COLOR_MAP);
    }

    public static void clearTheme(String itemId) {
        COLOR_MAP.remove(itemId);
        TAG_CACHE.remove(itemId);
        BorderThemeFileStore.saveColors(COLOR_MAP);
    }

    public static Map<String, BorderTheme> getAllThemes() {
        return new HashMap<>(COLOR_MAP);
    }
}
