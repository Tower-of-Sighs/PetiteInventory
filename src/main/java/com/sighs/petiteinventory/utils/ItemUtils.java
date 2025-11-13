package com.sighs.petiteinventory.utils;

import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.loader.EntryCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ItemUtils {
    private static final TagKey<Item> TOOLS_TAG =
            ItemTags.create(new ResourceLocation("forge", "tools"));
    private static final TagKey<net.minecraft.world.item.Item> SWORDS_TAG =
            ItemTags.create(new ResourceLocation("forge", "swords"));

    /**
     * 检查ItemStack是否为工具或武器
     * @param stack 待检查的物品堆栈
     * @return 如果是工具或武器返回true，否则返回false
     */
    public static boolean isToolOrWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.is(ItemTags.TOOLS)) {
            return true;
        }

        if (stack.is(ItemTags.SWORDS)) {
            return true;
        }

        if (stack.is(TOOLS_TAG)) {
            return true;
        }

        if (stack.is(SWORDS_TAG)) {
            return true;
        }

        return false;
    }

    public static String getItemRegistryName(Item item) {
        if (item == null) {
            return null;
        }

        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
        if (registryName == null) {
            return null;
        }

        return registryName.toString();
    }

    public static Item getItemById(String registryName) {
        if (registryName == null || registryName.isEmpty()) {
            return null;
        }

        try {
            ResourceLocation resourceLocation = new ResourceLocation(registryName);

            if (!ForgeRegistries.ITEMS.containsKey(resourceLocation)) {
                return null;
            }

            Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);

            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public static Collection<Item> getItemsOfTag(ResourceLocation tagId) {
        TagKey<Item> tagKey = ForgeRegistries.ITEMS.tags().createTagKey(tagId);
        ITagManager<Item> tagManager = ForgeRegistries.ITEMS.tags();
        Collection<Item> result = new HashSet<>();

        if (tagManager != null && tagManager.isKnownTagName(tagKey)) {
            tagManager.getTag(tagKey).forEach(result::add);
        }
        return result;
    }

    public static boolean isTagExists(ResourceLocation tagId) {
        TagKey<Item> tagKey = ForgeRegistries.ITEMS.tags().createTagKey(tagId);
        ITagManager<Item> tagManager = ForgeRegistries.ITEMS.tags();
        return tagManager != null && tagManager.isKnownTagName(tagKey);
    }

    public static List<Item> resolveItemList(List<String> identifiers) {
        List<Item> result = new ArrayList<>();

        for (String id : identifiers) {
            if (id == null || id.isEmpty()) continue;

            if (id.startsWith("#")) {
                String tagIdString = id.substring(1);
                try {
                    ResourceLocation tagId = new ResourceLocation(tagIdString);
                    Collection<Item> tagItems = getItemsOfTag(tagId);
                    if (tagItems.isEmpty()) {
                    } else {
                        result.addAll(tagItems);
                    }
                } catch (Exception ignored) {}
            } else {
                Item item = getItemById(id);
                if (item != null) {
                    result.add(item);
                }
            }
        }

        return result;
    }

    public static List<ResourceLocation> getItemTags(Item item) {
        ITagManager<Item> tagManager = ForgeRegistries.ITEMS.tags();

        if (tagManager == null) {
            return Collections.emptyList();
        }

        return tagManager.getReverseTag(item)
                .map(reverseTag ->
                        reverseTag.getTagKeys()
                                .map(TagKey::location)
                                .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static boolean isItemIdEmpty(String id) {
        return id == null || id.equals("minecraft:air");
    }

    public static Area getArea(ItemStack itemStack) {
        int width = 1, height = 1;
        String sizeString = EntryCache.matchItem(getItemRegistryName(itemStack.getItem()));
        if (sizeString != null) {
            String[] size = sizeString.split("\\*");
            width = Integer.getInteger(size[0]);
            height = Integer.getInteger(size[1]);
        }
        return new Area(itemStack, width, height);
    }
}
