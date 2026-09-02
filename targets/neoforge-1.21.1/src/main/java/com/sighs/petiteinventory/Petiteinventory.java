package com.sighs.petiteinventory;

import com.mojang.logging.LogUtils;
import com.sighs.petiteinventory.bootstrap.ModBootstrap;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Forge entry point. Feature initialization lives in the bootstrap module. */
@Mod(Petiteinventory.MODID)
public final class Petiteinventory {
    public static final String MODID = "petiteinventory";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Petiteinventory() {
        ModBootstrap.initialize();
    }
}
