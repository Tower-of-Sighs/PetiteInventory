package com.sighs.petiteinventory.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EntryLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("PetiteInventory");

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
