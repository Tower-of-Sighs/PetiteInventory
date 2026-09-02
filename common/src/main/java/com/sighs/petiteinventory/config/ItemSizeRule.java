package com.sighs.petiteinventory.config;

import java.util.List;

/** Serializable item-footprint rule entry shared by all targets. */
public class ItemSizeRule {
    public List<String> match;
    public String result;

    public List<String> getMatch() {
        return match;
    }

    public String getResult() {
        return result;
    }
}
