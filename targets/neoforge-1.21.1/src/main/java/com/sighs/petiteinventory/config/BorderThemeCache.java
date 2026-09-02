package com.sighs.petiteinventory.config;

import com.sighs.petiteinventory.inventory.BorderTheme;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

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

    public static BorderTheme getTheme(Item item) {
        return getTheme(item, ItemStack.EMPTY);
    }

    public static BorderTheme getTheme(Item item, ItemStack stack) {
        if (item == null) return BorderTheme.DEFAULT;

        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        BorderTheme theme = COLOR_MAP.get(itemId);

        if (theme == null && itemId.equals("tacz:modern_kinetic_gun")) {
            CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (customData.contains("GunId")) {
                String gunId = customData.getString("GunId");
                if (gunId != null && !gunId.isEmpty()) {
                    String preciseKey = itemId + "{GunId:\"" + gunId + "\"}";
                    theme = COLOR_MAP.get(preciseKey);
                }
            }
        }
        if (theme != null) return theme;

        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        for (Map.Entry<String, BorderTheme> tagEntry : COLOR_MAP.entrySet()) {
            if (tagEntry.getKey().startsWith("TAG:")) {
                String tagName = tagEntry.getKey().substring(4);
                TagKey<Item> tagKey = ItemTags.create(ResourceLocation.parse(tagName));
                if (BuiltInRegistries.ITEM.getTag(tagKey).map(tag -> tag.contains(holder)).orElse(false)) {
                    TAG_CACHE.put(itemId, tagEntry.getValue());
                    return tagEntry.getValue();
                }
            }
        }

        theme = TAG_CACHE.get(itemId);
        if (theme != null) return theme;

        return BorderTheme.DEFAULT;
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