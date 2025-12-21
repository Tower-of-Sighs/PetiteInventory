package com.sighs.petiteinventory.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EntryLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("PetiteInventory");
    private static final Path ITEMS_CONFIG_FILE = CONFIG_DIR.resolve("border_items.json");

    // 默认配置（首次运行时生成）
    private static final String DEFAULT_ITEMS_CONFIG =
            """
            [
              {
                "match": ["minecraft:bed"],
                "result": "3*2"
              },
              {
                "match": ["#minecraft:tools"],
                "result": "1*2"
              },
              {
                "match": ["#forge:stone", "#forge:ores", "#minecraft:logs"],
                "result": "2*2"
              },
              {
                "match": ["#minecraft:doors"],
                "result": "2*3"
              },
              {
                "match": ["#minecraft:slabs"],
                "result": "2*1"
              }
            ]""";

    public static List<Entry> loadAll() {
        List<Entry> allRules = new ArrayList<>();

        // 确保配置文件存在
        if (!Files.exists(ITEMS_CONFIG_FILE)) {
            try {
                Files.createDirectories(CONFIG_DIR);
                Files.writeString(ITEMS_CONFIG_FILE, DEFAULT_ITEMS_CONFIG);
            } catch (IOException e) {
                e.printStackTrace();
                return allRules;
            }
        }

        // 加载配置
        try (Reader reader = Files.newBufferedReader(ITEMS_CONFIG_FILE, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, new TypeToken<List<Entry>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 保存所有配置到文件（用于指令设置）
     */
    public static void saveAll(List<Entry> entries) {
        try {
            String json = GSON.toJson(entries);
            Files.writeString(ITEMS_CONFIG_FILE, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}