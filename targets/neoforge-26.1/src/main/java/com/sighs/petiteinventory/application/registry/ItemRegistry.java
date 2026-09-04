package com.sighs.petiteinventory.application.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.List;

public class ItemRegistry {
    public static String getItemRegistryName(Item item) {
        if (item == null) {
            return null;
        }

        Identifier registryName = BuiltInRegistries.ITEM.getKey(item);
        return registryName == null ? null : registryName.toString();
    }

    public static Item getItemById(String registryName) {
        if (registryName == null || registryName.isEmpty()) {
            return null;
        }

        try {
            Identifier resourceLocation = Identifier.parse(registryName);
            if (!BuiltInRegistries.ITEM.containsKey(resourceLocation)) {
                return null;
            }
            return BuiltInRegistries.ITEM.getOptional(resourceLocation).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<Identifier> getItemTags(Item item) {
        if (item == null) {
            return Collections.emptyList();
        }

        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        return BuiltInRegistries.ITEM.getTags()
                .filter(entry -> entry.contains(holder))
                .map(entry -> entry.key().location())
                .toList();
    }

    public static boolean isItemIdEmpty(String id) {
        return id == null || id.equals("minecraft:air");
    }
}
