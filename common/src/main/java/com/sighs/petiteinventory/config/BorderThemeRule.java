package com.sighs.petiteinventory.config;

import java.util.List;

/** Serializable border-theme rule entry shared by all targets. */
public class BorderThemeRule {
    public List<String> match;
    public String theme;

    public List<String> getMatch() {
        return match;
    }

    public String getTheme() {
        return theme;
    }
}
