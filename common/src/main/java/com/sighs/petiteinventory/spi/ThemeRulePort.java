package com.sighs.petiteinventory.spi;

import com.sighs.petiteinventory.inventory.BorderTheme;

/**
 * Loader boundary for border-theme rules.
 *
 * <p>The common service owns rule precedence; targets only translate their
 * registry, tag and NBT APIs into the three lookups below.</p>
 */
public interface ThemeRulePort<S> {
    String itemId(S stack);

    BorderTheme exactTheme(String itemId);

    BorderTheme tagTheme(String itemId);

    BorderTheme nbtTheme(S stack, String itemId);

    default BorderTheme theme(S stack, String itemId) {
        BorderTheme theme = nbtTheme(stack, itemId);
        if (theme != null) {
            return theme;
        }
        theme = exactTheme(itemId);
        if (theme != null) {
            return theme;
        }
        return tagTheme(itemId);
    }
}
