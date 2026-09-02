package com.sighs.petiteinventory.client;

/** Client-side mirror of the server-authoritative editing toggle. */
public final class ClientEditMode {
    private static boolean enabled;

    private ClientEditMode() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        ClientEditMode.enabled = enabled;
    }
}
