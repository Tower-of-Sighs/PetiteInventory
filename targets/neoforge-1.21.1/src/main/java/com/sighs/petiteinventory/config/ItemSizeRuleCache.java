package com.sighs.petiteinventory.config;

import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.*;
import java.util.stream.Collectors;

public class ItemSizeRuleCache {
    public static final HashMap<String, String> UnitMapCache = new HashMap<>();
    public static final HashMap<String, String> TagMapCache = new HashMap<>();
    public static final HashMap<String, String> NBTMapCache = new HashMap<>();

    /**
     * Smart classification storage based on the match string format.
     */
    public static void putEntry(ItemSizeRule rule) {
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
        ItemSizeRuleFileStore.loadAll().forEach(ItemSizeRuleCache::putEntry);
    }

    /**
     * Core matching method supporting precise NBT item matching.
     * Priority: NBT precise match > plain ID match > tag match.
     */
    public static String matchItem(String id, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return matchItem(id);
        }

        // 1. Try precise NBT matching first (highest priority).
        String nbtKey = getNBTKey(id, stack);
        if (nbtKey != null && NBTMapCache.containsKey(nbtKey)) {
            return NBTMapCache.get(nbtKey);
        }

        // 2. Try plain ID matching.
        String size = UnitMapCache.getOrDefault(id, null);
        if (size != null) return size;

        // 3. Try tag matching.
        List<ResourceLocation> tags = ItemInventoryService.getItemTags(ItemInventoryService.getItemById(id));
        if (!tags.isEmpty()) {
            for (ResourceLocation tag : tags) {
                String tagSize = matchTag(tag);
                if (tagSize != null) return tagSize;
            }
        }

        return null;
    }

    /**
     * ItemStack-free matching for non-NBT scenarios.
     */
    public static String matchItem(String id) {
        String size = UnitMapCache.getOrDefault(id, null);
        if (size != null) return size;

        var item = ItemInventoryService.getItemById(id);
        if (item != null) {
            List<ResourceLocation> tags = ItemInventoryService.getItemTags(item);
            for (ResourceLocation tag : tags) {
                String tagSize = matchTag(tag);
                if (tagSize != null) return tagSize;
            }
        }

        return null;
    }

    /**
     * Builds the precise NBT match key from an ItemStack.
     */
    private static String getNBTKey(String itemId, ItemStack stack) {
        CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (customData.isEmpty()) return null;

        // TACZ gun support.
        if (itemId.equals("tacz:modern_kinetic_gun") && customData.contains("GunId")) {
            String gunId = customData.getString("GunId");
            if (gunId != null && !gunId.isEmpty()) {
                return itemId + "{GunId:\"" + gunId + "\"}";
            }
        }

        // Extend additional NBT matching rules here if needed.
        return null;
    }

    public static String matchTag(String tagId) {
        return TagMapCache.getOrDefault(tagId, null);
    }

    public static String matchTag(ResourceLocation tagId) {
        return tagId != null ? matchTag(tagId.toString()) : null;
    }

    /**
     * Sets size via command (immediate and persisted).
     */
    public static void setSizeByCommand(String itemId, String size) {
        if (itemId.contains("{") && itemId.contains("}")) {
            NBTMapCache.put(itemId, size);
        } else {
            UnitMapCache.put(itemId, size);
        }

        saveConfig();
    }

    /**
     * Saves current caches to the config file.
     */
    public static void saveConfig() {
        List<ItemSizeRule> entries = new ArrayList<>();

        Map<String, List<String>> sizeGroups = new HashMap<>();

        Map<String, String> allItems = new HashMap<>();
        allItems.putAll(TagMapCache);
        allItems.putAll(UnitMapCache);
        allItems.putAll(NBTMapCache);

        allItems.forEach((item, size) -> {
            sizeGroups.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
        });

        for (Map.Entry<String, List<String>> group : sizeGroups.entrySet()) {
            ItemSizeRule entry = new ItemSizeRule();
            entry.match = group.getValue();
            entry.result = group.getKey();
            entries.add(entry);
        }

        ItemSizeRuleFileStore.saveAll(entries);
    }
}