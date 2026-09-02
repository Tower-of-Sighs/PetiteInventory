package com.sighs.petiteinventory.config;

import com.sighs.petiteinventory.inventory.BorderTheme;
import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import com.sighs.petiteinventory.spi.ThemeRulePort;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.HashMap;
import java.util.Map;

public class BorderThemeCache {
    private static final RuleTable<BorderTheme> RULES = new RuleTable<>();
    private static final ThemeRulePort<ItemStack> THEME_RULES = new ThemeRulePort<ItemStack>() {
        @Override public String itemId(ItemStack stack) {
            return stack == null || stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
        @Override public BorderTheme exactTheme(String itemId) { return matchExact(itemId); }
        @Override public BorderTheme tagTheme(String itemId) { return matchTagForItem(itemId); }
        @Override public BorderTheme nbtTheme(ItemStack stack, String itemId) { return matchNbt(itemId, stack); }
    };

    public static void load() {
        RULES.clear();
        for (Map.Entry<String, BorderTheme> entry : BorderThemeFileStore.loadColors().entrySet()) {
            putEntry(entry.getKey(), entry.getValue());
        }
    }

    private static void putEntry(String key, BorderTheme theme) {
        if (key == null || theme == null) return;
        if (key.startsWith("TAG:")) {
            RULES.putTag(key.substring(4), theme);
        } else if (key.contains("{") && key.contains("}")) {
            RULES.putNbt(key, theme);
        } else {
            RULES.putExact(key, theme);
        }
    }

    public static BorderTheme getTheme(Item item) {
        return getTheme(item, ItemStack.EMPTY);
    }

    public static BorderTheme getTheme(Item item, ItemStack stack) {
        if (item == null) return BorderTheme.DEFAULT;
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        BorderTheme theme = THEME_RULES.theme(stack, itemId);
        return theme == null ? BorderTheme.DEFAULT : theme;
    }

    public static BorderTheme matchExact(String itemId) {
        return RULES.exact(itemId);
    }

    public static BorderTheme matchNbt(String itemId, ItemStack stack) {
        if (!"tacz:modern_kinetic_gun".equals(itemId) || stack == null || stack.isEmpty()) return null;
        CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!customData.contains("GunId")) return null;
        String gunId = customData.getString("GunId");
        if (gunId == null || gunId.isEmpty()) return null;
        return RULES.nbt(itemId + "{GunId:\"" + gunId + "\"}");
    }

    public static BorderTheme matchTagForItem(String itemId) {
        Item item = ItemInventoryService.getItemById(itemId);
        if (item == null) return null;
        for (ResourceLocation tag : ItemInventoryService.getItemTags(item)) {
            BorderTheme theme = RULES.tag(tag.toString());
            if (theme != null) return theme;
        }
        return null;
    }

    public static void setTheme(String itemId, BorderTheme theme) {
        putEntry(itemId, theme);
        saveConfig();
    }

    public static void clearTheme(String itemId) {
        String tagId = itemId != null && itemId.startsWith("TAG:") ? itemId.substring(4) : itemId;
        RULES.removeExact(itemId);
        RULES.removeNbt(itemId);
        RULES.removeTag(tagId);
        saveConfig();
    }

    public static void saveConfig() {
        BorderThemeFileStore.saveColors(RULES.storedMap("TAG:"));
    }

    public static Map<String, BorderTheme> getAllThemes() {
        return new HashMap<>(RULES.storedMap("TAG:"));
    }
}
