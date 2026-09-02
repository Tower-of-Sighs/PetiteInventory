package com.sighs.petiteinventory.config;

import java.util.List;

public class BorderThemeRule {
    List<String> match;  // 支持物品ID或标签（如 "#minecraft:tools"）
    String theme;         // 主题ID

    public List<String> getMatch() {
        return match;
    }

    public String getTheme() {
        return theme;
    }
}