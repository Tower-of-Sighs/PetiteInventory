package com.sighs.petiteinventory.init;

public enum BorderTheme {
    DEFAULT("default", "默认", 0.8f, 0.8f, 0.8f),      // 灰色
    BLUE("blue", "高贵蓝", 0.3f, 0.5f, 1.0f),          // 蓝色
    PURPLE("purple", "高贵紫", 0.7f, 0.3f, 1.0f),      // 紫色
    ORANGE("orange", "高贵橙", 1.0f, 0.6f, 0.0f),      // 橙色
    RED("red", "高贵红", 1.0f, 0.2f, 0.2f);            // 红色

    private final String id;
    private final String displayName;
    private final float r, g, b;  // RGB颜色值（0.0-1.0）

    BorderTheme(String id, String displayName, float r, float g, float b) {
        this.id = id;
        this.displayName = displayName;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getR() {
        return r;
    }

    public float getG() {
        return g;
    }

    public float getB() {
        return b;
    }

    public static BorderTheme fromId(String id) {
        for (BorderTheme theme : values()) {
            if (theme.id.equalsIgnoreCase(id)) {
                return theme;
            }
        }
        return DEFAULT;
    }
}