package com.sighs.petiteinventory.platform.inventory;

import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.service.ItemRotation;
import com.sighs.petiteinventory.spi.ItemRulePort;
import com.sighs.petiteinventory.spi.StackPort;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/** Forge translation facade for the common item-rule service. */
public final class ItemInventoryService {
    private static final TagKey<Item> FORGE_TOOLS =
            ItemTags.create(new ResourceLocation("forge", "tools"));
    private static final TagKey<Item> FORGE_SWORDS =
            ItemTags.create(new ResourceLocation("forge", "swords"));

    private static final StackPort<ItemStack> STACKS = new ForgeStackPort();
    private static final ItemRulePort<ItemStack> RULES = new ItemRulePort<ItemStack>() {
        @Override public String itemId(ItemStack stack) {
            return stack == null ? null : getItemRegistryName(stack.getItem());
        }
        @Override public String rule(ItemStack stack, String itemId) {
            return com.sighs.petiteinventory.config.ItemSizeRuleCache.matchItem(itemId, stack);
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
        return stack.is(ItemTags.TOOLS) || stack.is(ItemTags.SWORDS)
                || stack.is(FORGE_TOOLS) || stack.is(FORGE_SWORDS);
    }

    public static final class ItemRotateHelper {
        private ItemRotateHelper() { }
        public static void setRotated(ItemStack stack, boolean rotated) { STACKS.setRotated(stack, rotated); }
        public static boolean isRotated(ItemStack stack) { return STACKS.isRotated(stack); }
    }

    public static String getItemRegistryName(Item item) {
        if (item == null) return null;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? null : id.toString();
    }

    public static Item getItemById(String registryName) {
        if (registryName == null || registryName.isEmpty()) return null;
        try {
            ResourceLocation id = new ResourceLocation(registryName);
            return ForgeRegistries.ITEMS.containsKey(id) ? ForgeRegistries.ITEMS.getValue(id) : null;
        } catch (Exception ignored) { return null; }
    }

    public static Collection<Item> getItemsOfTag(ResourceLocation tagId) {
        if (tagId == null || ForgeRegistries.ITEMS.tags() == null) return Collections.emptyList();
        TagKey<Item> key = ForgeRegistries.ITEMS.tags().createTagKey(tagId);
        ITagManager<Item> manager = ForgeRegistries.ITEMS.tags();
        Collection<Item> result = new HashSet<>();
        if (manager.isKnownTagName(key)) manager.getTag(key).forEach(result::add);
        return result;
    }

    public static boolean isTagExists(ResourceLocation tagId) {
        if (tagId == null || ForgeRegistries.ITEMS.tags() == null) return false;
        TagKey<Item> key = ForgeRegistries.ITEMS.tags().createTagKey(tagId);
        return ForgeRegistries.ITEMS.tags().isKnownTagName(key);
    }

    public static List<Item> resolveItemList(List<String> identifiers) {
        List<Item> result = new ArrayList<>();
        if (identifiers == null) return result;
        for (String id : identifiers) {
            if (id == null || id.isEmpty()) continue;
            if (id.startsWith("#")) {
                try { result.addAll(getItemsOfTag(new ResourceLocation(id.substring(1)))); }
                catch (Exception ignored) { }
            } else {
                Item item = getItemById(id);
                if (item != null) result.add(item);
            }
        }
        return result;
    }

    public static List<ResourceLocation> getItemTags(Item item) {
        ITagManager<Item> manager = ForgeRegistries.ITEMS.tags();
        if (manager == null || item == null) return Collections.emptyList();
        return manager.getReverseTag(item)
                .map(reverse -> reverse.getTagKeys().map(TagKey::location).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static boolean isItemIdEmpty(String id) { return id == null || "minecraft:air".equals(id); }

    private static final class ForgeStackPort implements StackPort<ItemStack> {
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
            return ItemStack.isSameItemSameTags(left, right);
        }
        @Override public boolean isRotated(ItemStack stack) {
            return stack != null && stack.hasTag() && stack.getTag().getBoolean(ItemRotation.TAG);
        }
        @Override public void setRotated(ItemStack stack, boolean rotated) {
            if (stack == null) return;
            if (rotated) stack.getOrCreateTag().putBoolean(ItemRotation.TAG, true);
            else if (stack.hasTag()) {
                stack.getTag().remove(ItemRotation.TAG);
                if (stack.getTag().isEmpty()) stack.setTag(null);
            }
        }
        @Override public com.sighs.petiteinventory.core.ItemSize footprint(ItemStack stack) {
            return COMMON.getArea(stack).size();
        }
    }
}
