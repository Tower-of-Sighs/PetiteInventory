package com.sighs.petiteinventory.loader;

import com.sighs.petiteinventory.init.BorderTheme;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BorderColorCache {
    private static final Map<String, BorderTheme> COLOR_MAP = new HashMap<>();
    private static final Map<String, BorderTheme> TAG_CACHE = new HashMap<>();

    public static void load() {
        COLOR_MAP.clear();
        TAG_CACHE.clear();
        COLOR_MAP.putAll(BorderColorLoader.loadColors());
    }

    /* 对外统一入口：无 ItemStack 时退化为仅按 Item 匹配 */
    public static BorderTheme getTheme(Item item) {
        return getTheme(item, ItemStack.EMPTY);
    }

    /* 核心实现：带 ItemStack 可读取 NBT 做精确匹配 */
    public static BorderTheme getTheme(Item item, ItemStack stack) {
        if (item == null) return BorderTheme.DEFAULT;

        // 1. 精确 ID 匹配（含 NBT 片段 key）
        String itemId = ForgeRegistries.ITEMS.getKey(item).toString();
        BorderTheme theme = COLOR_MAP.get(itemId);

// 2. TACZ 枪械按 GunId 精确匹配（直接读根层级）
        if (theme == null && itemId.equals("tacz:modern_kinetic_gun") && stack.hasTag()) {
            String gunId = stack.getTag().getString("GunId");
            if (gunId != null && !gunId.isEmpty()) {
                // ✅ 修正：NBT键格式与命令生成器保持一致
                String preciseKey = itemId + "{GunId:\"" + gunId + "\"}";
                theme = COLOR_MAP.get(preciseKey);
            }
        }
        if (theme != null) return theme;

        // 3. 标签匹配
        ResourceLocation itemIdRL = ForgeRegistries.ITEMS.getKey(item);
        if (itemIdRL != null) {
            for (Map.Entry<String, BorderTheme> tagEntry : COLOR_MAP.entrySet()) {
                if (tagEntry.getKey().startsWith("TAG:")) {
                    String tagName = tagEntry.getKey().substring(4);
                    ResourceLocation tagId = new ResourceLocation(tagName);
                    if (ForgeRegistries.ITEMS.tags() != null &&
                            ForgeRegistries.ITEMS.tags().getTag(
                                    net.minecraft.tags.ItemTags.create(tagId)
                            ).contains(item)) {
                        TAG_CACHE.put(itemId, tagEntry.getValue());
                        return tagEntry.getValue();
                    }
                }
            }
        }

        // 4. 缓存的标签匹配
        theme = TAG_CACHE.get(itemId);
        if (theme != null) return theme;

        return BorderTheme.DEFAULT;
    }

    /* 供指令调用：key 支持带 NBT 片段 */
    public static void setTheme(String itemId, BorderTheme theme) {
        if (theme == BorderTheme.DEFAULT) {
            COLOR_MAP.remove(itemId);
        } else {
            COLOR_MAP.put(itemId, theme);
        }
        BorderColorLoader.saveColors(COLOR_MAP);
    }

    public static Map<String, BorderTheme> getAllThemes() {
        return new HashMap<>(COLOR_MAP);
    }
}