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
import java.util.Collection;
import java.util.List;

public class EntryLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("PetiteInventory");
    // 默认配置的 JSON 字符串常量
    private static final String DEFAULT_CONFIG =
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
        List<Entry> allRule = new ArrayList<>();

        allRule.addAll(loadFromDir(CONFIG_DIR));

        return allRule;
    }

    private static List<Entry> loadFromDir(Path path) {
        List<Entry> allRule = new ArrayList<>();

        // 确保目录存在
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                // 创建默认配置文件
                Path defaultFile = path.resolve("default.json"); // 默认文件名
                if (!Files.exists(defaultFile)) {
                    Files.writeString(defaultFile, DEFAULT_CONFIG);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return allRule;
        }

        // 遍历所有JSON文件
        try (var stream = Files.newDirectoryStream(path, "*.json")) {
            for (Path file : stream) {
                allRule.addAll(loadRecipesFromFile(file));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return allRule;
    }

    private static Collection<? extends Entry> loadRecipesFromFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return GSON.fromJson(reader, new TypeToken<List<Entry>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
