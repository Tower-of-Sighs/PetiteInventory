package com.sighs.petiteinventory.utils;

import com.sighs.petiteinventory.compat.KubeJSCompat;
import com.sighs.petiteinventory.init.Area;
import com.sighs.petiteinventory.init.AreaEvent;
import com.sighs.petiteinventory.loader.EntryCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
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

    private static final String TAG = "PetiteRotated";

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

    public static class ItemRotateHelper {
        private static final String TAG = "PetiteRotated";

        /** 写：客户端用 */
        public static void setRotated(ItemStack stack, boolean rotated) {
            if (rotated) {
                stack.getOrCreateTag().putBoolean(TAG, true);
            } else {
                if (stack.hasTag()) {
                    stack.getTag().remove(TAG);
                    if (stack.getTag().isEmpty()) stack.setTag(null);
                }
            }
        }

        /** 读：两端都用 */
        public static boolean isRotated(ItemStack stack) {
            return stack.hasTag() && stack.getTag().getBoolean(TAG);
        }
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
        // 1. 首先检查NBT精确匹配（最高优先级）
        String itemId = getItemRegistryName(itemStack.getItem());
        if (itemId != null && !itemStack.isEmpty()) {
            // 生成NBT键格式，检查是否为NBT物品
            String nbtKey = getNBTKey(itemId, itemStack);
            if (nbtKey != null) {
                String nbtSize = EntryCache.NBTMapCache.get(nbtKey);
                if (nbtSize != null) {
                    // 解析尺寸并应用旋转
                    String[] size = nbtSize.replace(" ", "").split("\\*");
                    int width = Integer.parseInt(size[0]);
                    int height = Integer.parseInt(size[1]);

                    boolean rotated = ItemRotateHelper.isRotated(itemStack);
                    if (rotated) {
                        int tmp = width;
                        width = height;
                        height = tmp;
                    }

                    return new Area(width, height, itemStack);
                }
            }
        }

        // 2. 尝试普通ID匹配
        String normalSize = EntryCache.matchItem(itemId);
        if (normalSize != null) {
            String[] size = normalSize.replace(" ", "").split("\\*");
            int width = Integer.parseInt(size[0]);
            int height = Integer.parseInt(size[1]);

            boolean rotated = ItemRotateHelper.isRotated(itemStack);
            if (rotated) {
                int tmp = width;
                width = height;
                height = tmp;
            }

            return new Area(width, height, itemStack);
        }

        // 3. 默认1×1
        return new Area(1, 1, itemStack);
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
}
