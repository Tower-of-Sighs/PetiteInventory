package com.sighs.petiteinventory.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.common.Mod;

import java.util.List;

public class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static ModConfigSpec.ConfigValue<List<? extends String>> WHITELIST;
    public static ModConfigSpec.ConfigValue<Boolean> ENABLE_INVENTORY;
    public static ModConfigSpec.ConfigValue<Boolean> ENABLE_OVERLAP_TIDY;

    static {
        BUILDER.push("Setting");

        WHITELIST = BUILDER
                .comment("Enabled menu.")
                .defineList("whitelist",
                        List.of(),
                        entry -> entry instanceof String
                );
        ENABLE_INVENTORY = BUILDER
                .comment("Whether inventory will be effort.")
                .define("enableInventory", true);
        ENABLE_OVERLAP_TIDY = BUILDER
                .comment("Automatically spread and repack overlapping items when a container menu opens.")
                .define("enableOverlapTidy", true);

        SPEC = BUILDER.build();
    }
}
