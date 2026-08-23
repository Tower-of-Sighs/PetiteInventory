package com.sighs.petiteinventory.inventory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative edit-mode state. */
public final class EditModeService {
    private static final Set<UUID> EDITORS = ConcurrentHashMap.newKeySet();

    private EditModeService() {
    }

    public static boolean toggle(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("player id is required");
        }
        if (!EDITORS.add(id)) {
            EDITORS.remove(id);
            return false;
        }
        return true;
    }

    public static boolean isEnabled(UUID id) {
        return id != null && EDITORS.contains(id);
    }
}
