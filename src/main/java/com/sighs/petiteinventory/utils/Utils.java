package com.sighs.petiteinventory.utils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    public static String getItemRegistryName(Item item) {
        if (item == null) {
            return null;
        }

        ResourceLocation registryName = null;

        for (var entry : ForgeRegistries.ITEMS.getEntries()) {
            if (entry.getValue().equals(item)) {
                registryName = entry.getKey().location();
            }
        }

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

            Item item = null;

            for (var entry : ForgeRegistries.ITEMS.getEntries()) {
                if (entry.getKey().location().equals(resourceLocation)) {
                    item = entry.getValue();
                }
            }

            return item;
        } catch (Exception e) {
            return null;
        }
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
}