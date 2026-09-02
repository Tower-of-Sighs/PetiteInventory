package com.sighs.petiteinventory.config;

import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemSizeRuleCache {
    private static final RuleTable<String> RULES = new RuleTable<>();

    public static void putEntry(ItemSizeRule rule) {
        for (String match : rule.match) {
            if (match.startsWith("#")) {
                RULES.putTag(match.replace("#", ""), rule.result);
            } else if (match.contains("{") && match.contains("}")) {
                RULES.putNbt(match, rule.result);
            } else {
                RULES.putExact(match, rule.result);
            }
        }
    }

    public static void clearCache() {
        RULES.clear();
    }

    public static void loadAllRule() {
        clearCache();
        ItemSizeRuleFileStore.loadAll().forEach(ItemSizeRuleCache::putEntry);
    }

    public static String matchItem(String id, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return matchItem(id);
        String rule = matchNbt(id, stack);
        if (rule != null) return rule;
        rule = matchExact(id);
        if (rule != null) return rule;
        return matchTagForItem(id);
    }

    public static String matchItem(String id) {
        String rule = matchExact(id);
        if (rule != null) return rule;
        return matchTagForItem(id);
    }

    public static String matchExact(String id) {
        return RULES.exact(id);
    }

    public static String matchTagForItem(String id) {
        var item = ItemInventoryService.getItemById(id);
        if (item == null) return null;
        List<ResourceLocation> tags = ItemInventoryService.getItemTags(item);
        for (ResourceLocation tag : tags) {
            String rule = matchTag(tag);
            if (rule != null) return rule;
        }
        return null;
    }

    public static String matchNbt(String itemId, ItemStack stack) {
        String nbtKey = getNBTKey(itemId, stack);
        return nbtKey == null ? null : RULES.nbt(nbtKey);
    }

    private static String getNBTKey(String itemId, ItemStack stack) {
        if (stack == null || !stack.hasTag()) return null;
        if (itemId.equals("tacz:modern_kinetic_gun") && stack.getTag().contains("GunId")) {
            String gunId = stack.getTag().getString("GunId");
            if (gunId != null && !gunId.isEmpty()) {
                return itemId + "{GunId:\"" + gunId + "\"}";
            }
        }
        return null;
    }

    public static String matchTag(String tagId) {
        return RULES.tag(tagId);
    }

    public static String matchTag(ResourceLocation tagId) {
        return tagId != null ? matchTag(tagId.toString()) : null;
    }

    public static void setSizeByCommand(String itemId, String size) {
        if (itemId.contains("{") && itemId.contains("}")) {
            RULES.putNbt(itemId, size);
        } else {
            RULES.putExact(itemId, size);
        }
        saveConfig();
    }

    public static void saveConfig() {
        List<ItemSizeRule> entries = new ArrayList<>();
        for (RuleGroup<String> group : RULES.groups("#")) {
            ItemSizeRule entry = new ItemSizeRule();
            entry.match = group.matches();
            entry.result = group.value();
            entries.add(entry);
        }
        ItemSizeRuleFileStore.saveAll(entries);
    }
}
