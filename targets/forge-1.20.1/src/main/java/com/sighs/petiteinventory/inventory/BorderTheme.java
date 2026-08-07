package com.sighs.petiteinventory.inventory;

public enum BorderTheme {
    DEFAULT("default", "默认", 1.0f, 1.0f, 1.0f),
    GREEN("green", "绿色", 0.2f, 0.8f, 0.2f),
    CYAN("cyan", "青色", 0.0f, 0.8f, 0.8f),
    BLUE("blue", "高贵蓝", 0.3f, 0.5f, 1.0f),
    PURPLE("purple", "高贵紫", 0.7f, 0.3f, 1.0f),
    PINK("pink", "粉色", 1.0f, 0.4f, 0.7f),
    ORANGE("orange", "高贵橙", 1.0f, 0.6f, 0.0f),
    RED("red", "高贵红", 1.0f, 0.2f, 0.2f);

    private final String id;
    private final String displayName;
    private final float r;
    private final float g;
    private final float b;

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
