package com.sighs.petiteinventory.inventory;

import com.sighs.petiteinventory.config.ItemSizeRuleCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.*;
import java.util.stream.Collectors;

public class ItemInventoryService {
    private static final TagKey<Item> TOOLS_TAG =
            ItemTags.create(new ResourceLocation("forge", "tools"));
    private static final TagKey<Item> SWORDS_TAG =
            ItemTags.create(new ResourceLocation("forge", "swords"));

    private static final String TAG = "PetiteRotated";

    public static boolean isToolOrWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.is(ItemTags.TOOLS)) return true;
        if (stack.is(ItemTags.SWORDS)) return true;
        if (stack.is(TOOLS_TAG)) return true;
        return stack.is(SWORDS_TAG);
    }

    public static boolean isSameItemIgnoreRotate(ItemStack stack1, ItemStack stack2) {
        if (stack1 == stack2) return true;
        if (stack1.isEmpty() || stack2.isEmpty()) return false;
        if (stack1.getItem() != stack2.getItem()) return false;

        ItemStack copy1 = stack1.copy();
        ItemStack copy2 = stack2.copy();
        ItemRotateHelper.setRotated(copy1, false);
        ItemRotateHelper.setRotated(copy2, false);

        return ItemStack.isSameItemSameTags(copy1, copy2);
    }

    public static class ItemRotateHelper {
        public static final String TAG = "PetiteRotated";

        public static void setRotated(ItemStack stack, boolean rotated) {
            if (rotated) {
                stack.getOrCreateTag().putBoolean(TAG, true);
            } else if (stack.hasTag()) {
                stack.getTag().remove(TAG);
                if (stack.getTag().isEmpty()) stack.setTag(null);
            }
        }

        public static boolean isRotated(ItemStack stack) {
            return stack.hasTag() && stack.getTag().getBoolean(TAG);
        }
    }

    public static String getItemRegistryName(Item item) {
        if (item == null) return null;

        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
        return registryName == null ? null : registryName.toString();
    }

    public static Item getItemById(String registryName) {
        if (registryName == null || registryName.isEmpty()) return null;

        try {
            ResourceLocation resourceLocation = new ResourceLocation(registryName);
            if (!ForgeRegistries.ITEMS.containsKey(resourceLocation)) return null;
            return ForgeRegistries.ITEMS.getValue(resourceLocation);
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
                    if (!tagItems.isEmpty()) result.addAll(tagItems);
                } catch (Exception ignored) {
                }
            } else {
                Item item = getItemById(id);
                if (item != null) result.add(item);
            }
        }
        return result;
    }

    public static List<ResourceLocation> getItemTags(Item item) {
        ITagManager<Item> tagManager = ForgeRegistries.ITEMS.tags();
        if (tagManager == null) return Collections.emptyList();

        return tagManager.getReverseTag(item)
                .map(reverseTag -> reverseTag.getTagKeys()
                        .map(TagKey::location)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static boolean isItemIdEmpty(String id) {
        return id == null || id.equals("minecraft:air");
    }

    public static Area getArea(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return new Area(1, 1, itemStack);
        }

        String itemId = getItemRegistryName(itemStack.getItem());

        // NBT exact match > normal ID > tag match.
        String configuredSize = itemId == null ? null : ItemSizeRuleCache.matchItem(itemId, itemStack);
        if (configuredSize != null) {
            String[] size = configuredSize.replace(" ", "").split("\\*");
            int width = Integer.parseInt(size[0]);
            int height = Integer.parseInt(size[1]);

            if (ItemRotateHelper.isRotated(itemStack)) {
                int tmp = width;
                width = height;
                height = tmp;
            }

            return new Area(width, height, itemStack);
        }

        return new Area(1, 1, itemStack);
    }
}
