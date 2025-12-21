package com.sighs.petiteinventory.loader;

import com.sighs.petiteinventory.utils.ItemUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class EntryCache {
    public static final HashMap<String, String> UnitMapCache = new HashMap<>();
    public static final HashMap<String, String> TagMapCache = new HashMap<>();
    public static final HashMap<String, String> NBTMapCache = new HashMap<>();

    /**
     * 智能分类存储：根据match字符串格式决定存入哪个缓存
     */
    public static void putEntry(Entry rule) {
        String result = rule.result;
        for (String match : rule.match) {
            if (match.startsWith("#")) {
                TagMapCache.put(match.replace("#", ""), result);
            } else if (match.contains("{") && match.contains("}")) {
                NBTMapCache.put(match, result);
            } else {
                UnitMapCache.put(match, result);
            }
        }
    }

    public static void clearCache() {
        UnitMapCache.clear();
        TagMapCache.clear();
        NBTMapCache.clear();
    }

    public static void loadAllRule() {
        clearCache();
        EntryLoader.loadAll().forEach(EntryCache::putEntry);
    }

    /**
     * 核心匹配方法：支持NBT物品精确匹配
     * 优先级：NBT精确匹配 > 普通ID匹配 > 标签匹配
     */
    public static String matchItem(String id, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return matchItem(id);
        }

        // 1. 首先尝试NBT精确匹配（指令设置的最高优先级）
        String nbtKey = getNBTKey(id, stack);
        if (nbtKey != null && NBTMapCache.containsKey(nbtKey)) {
            return NBTMapCache.get(nbtKey);
        }

        // 2. 尝试普通ID匹配
        String size = UnitMapCache.getOrDefault(id, null);
        if (size != null) return size;

        // 3. 尝试标签匹配
        List<ResourceLocation> tags = ItemUtils.getItemTags(ItemUtils.getItemById(id));
        if (!tags.isEmpty()) {
            for (ResourceLocation tag : tags) {
                String tagSize = matchTag(tag);
                if (tagSize != null) return tagSize;
            }
        }

        return null;
    }

    /**
     * 不带ItemStack的匹配（仅用于非NBT场景）
     */
    public static String matchItem(String id) {
        // 尝试普通ID匹配
        String size = UnitMapCache.getOrDefault(id, null);
        if (size != null) return size;

        // 尝试标签匹配
        var item = ItemUtils.getItemById(id);
        if (item != null) {
            List<ResourceLocation> tags = ItemUtils.getItemTags(item);
            for (ResourceLocation tag : tags) {
                String tagSize = matchTag(tag);
                if (tagSize != null) return tagSize;
            }
        }

        return null;
    }

    /**
     * 从ItemStack生成NBT精确匹配键
     */
    private static String getNBTKey(String itemId, ItemStack stack) {
        if (!stack.hasTag()) return null;

        // TACZ枪械支持
        if (itemId.equals("tacz:modern_kinetic_gun") && stack.getTag().contains("GunId")) {
            String gunId = stack.getTag().getString("GunId");
            if (gunId != null && !gunId.isEmpty()) {
                return itemId + "{GunId:\"" + gunId + "\"}";
            }
        }

        // 可以扩展其他模组的NBT匹配规则
        return null;
    }

    public static String matchTag(String tagId) {
        return TagMapCache.getOrDefault(tagId, null);
    }

    public static String matchTag(ResourceLocation tagId) {
        return tagId != null ? matchTag(tagId.toString()) : null;
    }

    /**
     * 通过指令设置尺寸（即时生效并持久化）
     */
    public static void setSizeByCommand(String itemId, String size) {
        // 1. 智能分类存储
        if (itemId.contains("{") && itemId.contains("}")) {
            NBTMapCache.put(itemId, size);
        } else {
            UnitMapCache.put(itemId, size);
        }

        // 2. 立即保存到文件
        saveConfig();
    }

    /**
     * 将当前缓存保存到配置文件
     */
    public static void saveConfig() {
        List<Entry> entries = new ArrayList<>();

        // 按尺寸分组
        Map<String, List<String>> sizeGroups = new HashMap<>();

        // 合并所有缓存（NBT优先级最高，覆盖其他）
        Map<String, String> allItems = new HashMap<>();
        allItems.putAll(TagMapCache);      // 标签配置
        allItems.putAll(UnitMapCache);     // 普通物品
        allItems.putAll(NBTMapCache);      // NBT物品（优先级最高）

        // 按尺寸分组
        allItems.forEach((item, size) -> {
            sizeGroups.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
        });

        // 转换为Entry列表
        for (Map.Entry<String, List<String>> group : sizeGroups.entrySet()) {
            Entry entry = new Entry();
            entry.match = group.getValue();
            entry.result = group.getKey();
            entries.add(entry);
        }

        // 保存到文件
        EntryLoader.saveAll(entries);
    }
}