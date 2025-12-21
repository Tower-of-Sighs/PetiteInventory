package com.sighs.petiteinventory;

import com.mojang.logging.LogUtils;
import com.sighs.petiteinventory.loader.BorderColorCache;
import com.sighs.petiteinventory.network.PacketHandler;
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
        PacketHandler.register();

        // 加载边框颜色配置
        BorderColorCache.load();
    }
}