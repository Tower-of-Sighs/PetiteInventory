package com.sighs.petiteinventory.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;

public class JSPlugin extends KubeJSPlugin {

    public void registerEvents() {
        Events.GROUP.register();
    }

}
