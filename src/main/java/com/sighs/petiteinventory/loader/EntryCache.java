package com.sighs.petiteinventory.loader;

import com.sighs.petiteinventory.utils.ItemUtils;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;

public class EntryCache {
    public static final HashMap<String, String> UnitMapCache = new HashMap<>();
    public static final HashMap<String, String> TagMapCache = new HashMap<>();

    public static void putEntry(Entry rule) {
        String result = rule.getResult();
        for (String match : rule.getMatch()) {
            if (match.startsWith("#")) {
                TagMapCache.put(match.replace("#", ""), result);
            } else {
                UnitMapCache.put(match, result);
            }
        }
    }

    public static void clearCache() {
        UnitMapCache.clear();
        TagMapCache.clear();
    }

    public static void loadAllRule() {
        clearCache();
        EntryLoader.loadAll().forEach(EntryCache::putEntry);
    }

    public static String matchItem(String id) {
        List<ResourceLocation> tags = ItemUtils.getItemTags(ItemUtils.getItemById(id));
        if (tags.isEmpty()) return UnitMapCache.getOrDefault(id, null);
        for (ResourceLocation tag : tags) {
            String item = matchTag(tag);
            if (item != null) return item;
        }
        return UnitMapCache.getOrDefault(id, null);
    }

    public static String matchTag(String tagId) {
        return TagMapCache.getOrDefault(tagId, null);
    }

    public static String matchTag(ResourceLocation tagId) {
        return tagId != null ? matchTag(tagId.toString()) : null;
    }
}