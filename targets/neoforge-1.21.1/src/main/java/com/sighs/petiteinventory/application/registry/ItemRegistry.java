package com.sighs.petiteinventory.application.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.List;

public class ItemRegistry {
    public static String getItemRegistryName(Item item) {
        if (item == null) {
            return null;
        }

        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
        return registryName == null ? null : registryName.toString();
    }

    public static Item getItemById(String registryName) {
        if (registryName == null || registryName.isEmpty()) {
            return null;
        }

        try {
            ResourceLocation resourceLocation = ResourceLocation.parse(registryName);
            if (!BuiltInRegistries.ITEM.containsKey(resourceLocation)) {
                return null;
            }
            return BuiltInRegistries.ITEM.get(resourceLocation);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<ResourceLocation> getItemTags(Item item) {
        if (item == null) {
            return Collections.emptyList();
        }

        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        return BuiltInRegistries.ITEM.getTags()
                .filter(entry -> entry.getSecond().contains(holder))
                .map(entry -> entry.getFirst().location())
                .toList();
    }

    public static boolean isItemIdEmpty(String id) {
        return id == null || id.equals("minecraft:air");
    }
}