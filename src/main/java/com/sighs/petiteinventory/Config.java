package com.sighs.petiteinventory;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST;
    public static ForgeConfigSpec.ConfigValue<Boolean> ENABLE_INVENTORY;

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

        SPEC = BUILDER.build();
    }
}
