package com.sighs.petiteinventory.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sighs.petiteinventory.init.BorderTheme;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BorderColorLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("PetiteInventory");
    private static final Path COLOR_CONFIG_FILE = CONFIG_DIR.resolve("border_colors.json");

    // 默认颜色配置
    private static final String DEFAULT_COLOR_CONFIG =
            """
            [
              {
                "match": ["minecraft:diamond"],
                "theme": "orange"
              },
              {
                "match": ["minecraft:emerald"],
                "theme": "red"
              },
              {
                "match": ["#minecraft:tools"],
                "theme": "blue"
              },
              {
                "match": ["#forge:ores"],
                "theme": "purple"
              }
            ]""";

    /**
     * 加载所有颜色配置，返回物品ID到主题的映射
     */
    public static Map<String, BorderTheme> loadColors() {
        Map<String, BorderTheme> colorMap = new HashMap<>();

        // 确保配置文件存在
        if (!Files.exists(COLOR_CONFIG_FILE)) {
            try {
                Files.createDirectories(CONFIG_DIR);
                Files.writeString(COLOR_CONFIG_FILE, DEFAULT_COLOR_CONFIG);
            } catch (IOException e) {
                e.printStackTrace();
                return colorMap;
            }
        }

        // 读取配置
        try (Reader reader = Files.newBufferedReader(COLOR_CONFIG_FILE, StandardCharsets.UTF_8)) {
            List<BorderColorEntry> entries = GSON.fromJson(reader,
                    new TypeToken<List<BorderColorEntry>>(){}.getType());

            for (BorderColorEntry entry : entries) {
                BorderTheme theme = BorderTheme.fromId(entry.getTheme());
                for (String match : entry.getMatch()) {
                    if (match.startsWith("#")) {
                        // 标签匹配，后续解析
                        colorMap.put("TAG:" + match.substring(1), theme);
                    } else {
                        // 物品ID直接匹配
                        colorMap.put(match, theme);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return colorMap;
    }

    /**
     * 保存颜色配置到文件
     */
    public static void saveColors(Map<String, BorderTheme> colorMap) {
        List<BorderColorEntry> entries = new ArrayList<>();

        // 按主题分组
        Map<BorderTheme, List<String>> themeGroups = new HashMap<>();
        for (Map.Entry<String, BorderTheme> entry : colorMap.entrySet()) {
            themeGroups.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                    .add(entry.getKey());
        }

        // 转换为配置格式
        for (Map.Entry<BorderTheme, List<String>> group : themeGroups.entrySet()) {
            BorderColorEntry entry = new BorderColorEntry();
            entry.theme = group.getKey().getId();
            entry.match = group.getValue();
            entries.add(entry);
        }

        // 写入文件
        try {
            String json = GSON.toJson(entries);
            Files.writeString(COLOR_CONFIG_FILE, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}