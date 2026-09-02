package com.sighs.petiteinventory.platform.inventory;

import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.service.ItemRotation;
import com.sighs.petiteinventory.spi.ItemRulePort;
import com.sighs.petiteinventory.spi.StackPort;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** NeoForge translation facade for the common item-rule service. */
public final class ItemInventoryService {
    private static final TagKey<Item> FORGE_TOOLS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "tools"));
    private static final TagKey<Item> FORGE_SWORDS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "swords"));

    private static final StackPort<ItemStack> STACKS = new NeoForgeStackPort();
    private static final ItemRulePort<ItemStack> RULES = new ItemRulePort<ItemStack>() {
        @Override public String itemId(ItemStack stack) {
            return stack == null ? null : getItemRegistryName(stack.getItem());
        }
        @Override public String exactRule(String itemId) {
            return com.sighs.petiteinventory.config.ItemSizeRuleCache.matchExact(itemId);
        }
        @Override public String tagRule(String itemId) {
            return com.sighs.petiteinventory.config.ItemSizeRuleCache.matchTagForItem(itemId);
        }
        @Override public String nbtRule(ItemStack stack, String itemId) {
            return com.sighs.petiteinventory.config.ItemSizeRuleCache.matchNbt(itemId, stack);
        }
    };
    private static final com.sighs.petiteinventory.inventory.ItemInventoryService<ItemStack> COMMON =
            new com.sighs.petiteinventory.inventory.ItemInventoryService<>(STACKS, RULES);

    private ItemInventoryService() {
    }

    public static StackPort<ItemStack> stackPort() { return STACKS; }
    public static Area<ItemStack> getArea(ItemStack stack) { return COMMON.getArea(stack); }
    public static boolean isSameItemIgnoreRotate(ItemStack first, ItemStack second) {
        return COMMON.isSameItemIgnoreRotate(first, second);
    }

    public static boolean isToolOrWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return (stack.is(ItemTags.AXES) || stack.is(ItemTags.HOES) || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS))
                || stack.is(FORGE_TOOLS) || stack.is(FORGE_SWORDS);
    }

    public static final class ItemRotateHelper {
        private ItemRotateHelper() { }
        public static void setRotated(ItemStack stack, boolean rotated) { STACKS.setRotated(stack, rotated); }
        public static boolean isRotated(ItemStack stack) { return STACKS.isRotated(stack); }
    }

    public static String getItemRegistryName(Item item) {
        if (item == null) return null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? null : id.toString();
    }

    public static Item getItemById(String registryName) {
        if (registryName == null || registryName.isEmpty()) return null;
        try {
            ResourceLocation id = ResourceLocation.parse(registryName);
            return BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
        } catch (Exception ignored) { return null; }
    }

    public static Collection<Item> getItemsOfTag(ResourceLocation tagId) {
        if (tagId == null) return Collections.emptyList();
        TagKey<Item> key = ItemTags.create(tagId);
        return BuiltInRegistries.ITEM.getTag(key)
                .map(tag -> {
                    Collection<Item> result = new HashSet<>();
                    for (Holder<Item> holder : tag) {
                        result.add(holder.value());
                    }
                    return result;
                })
                .orElse(Collections.emptyList());
    }

    public static boolean isTagExists(ResourceLocation tagId) {
        if (tagId == null) return false;
        return BuiltInRegistries.ITEM.getTag(ItemTags.create(tagId)).isPresent();
    }

    public static List<Item> resolveItemList(List<String> identifiers) {
        List<Item> result = new ArrayList<>();
        if (identifiers == null) return result;
        for (String id : identifiers) {
            if (id == null || id.isEmpty()) continue;
            if (id.startsWith("#")) {
                try { result.addAll(getItemsOfTag(ResourceLocation.parse(id.substring(1)))); }
                catch (Exception ignored) { }
            } else {
                Item item = getItemById(id);
                if (item != null) result.add(item);
            }
        }
        return result;
    }

    public static List<ResourceLocation> getItemTags(Item item) {
        if (item == null) return Collections.emptyList();
        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        return BuiltInRegistries.ITEM.getTags()
                .filter(entry -> entry.getSecond().contains(holder))
                .map(entry -> entry.getFirst().location())
                .toList();
    }

    public static boolean isItemIdEmpty(String id) { return id == null || "minecraft:air".equals(id); }

    private static final class NeoForgeStackPort implements StackPort<ItemStack> {
        @Override public boolean isEmpty(ItemStack stack) { return stack == null || stack.isEmpty(); }
        @Override public boolean isStackable(ItemStack stack) { return stack != null && stack.isStackable(); }
        @Override public ItemStack copy(ItemStack stack) { return stack.copy(); }
        @Override public int count(ItemStack stack) { return stack.getCount(); }
        @Override public void setCount(ItemStack stack, int count) { stack.setCount(count); }
        @Override public void shrink(ItemStack stack, int amount) { stack.shrink(amount); }
        @Override public void grow(ItemStack stack, int amount) { stack.grow(amount); }
        @Override public int maxStackSize(ItemStack stack) { return stack.getMaxStackSize(); }
        @Override public boolean sameItemIgnoringRotation(ItemStack first, ItemStack second) {
            if (first == second) return true;
            if (isEmpty(first) || isEmpty(second) || first.getItem() != second.getItem()) return false;
            ItemStack left = first.copy(), right = second.copy();
            setRotated(left, false); setRotated(right, false);
            return ItemStack.isSameItemSameComponents(left, right);
        }
        @Override public boolean isRotated(ItemStack stack) {
            if (stack == null) return false;
            CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            return customData.getBoolean(ItemRotation.TAG);
        }
        @Override public void setRotated(ItemStack stack, boolean rotated) {
            if (stack == null) return;
            CompoundTag customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (rotated) {
                customData.putBoolean(ItemRotation.TAG, true);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
            } else {
                customData.remove(ItemRotation.TAG);
                if (customData.isEmpty()) {
                    stack.remove(DataComponents.CUSTOM_DATA);
                } else {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
                }
            }
        }
        @Override public com.sighs.petiteinventory.core.ItemSize footprint(ItemStack stack) {
            return COMMON.getArea(stack).size();
        }
    }
}
