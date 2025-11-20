package com.sighs.petiteinventory.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptTypePredicate;

public interface Events {

    EventGroup GROUP = EventGroup.of("PI$Events");

    EventHandler CLIENT_EVENT = GROUP.client("clientArea", () -> AreaEventJS.class);
    EventHandler SERVER_EVENT = GROUP.server("serverArea", () -> AreaEventJS.class);

}
