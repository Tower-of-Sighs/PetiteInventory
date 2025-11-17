package com.sighs.petiteinventory;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Petiteinventory.MODID)
public class Petiteinventory {
    public static final String MODID = "petiteinventory";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Petiteinventory() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
