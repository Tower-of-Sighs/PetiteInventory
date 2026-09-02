package com.sighs.petiteinventory.spi;

/**
 * Loader boundary for configuration-backed item footprint rules.
 *
 * <p>The common service owns rule precedence, parsing, rotation and event
 * publication. Targets only translate their registry and data-component APIs
 * into a rule expression.</p>
 */
public interface ItemRulePort<S> {
    /** Returns the stable registry id for the stack's item, or {@code null}. */
    String itemId(S stack);

    /** Returns the exact item-id rule, such as {@code 2*1}, or {@code null}. */
    String exactRule(String itemId);

    /** Returns the first matching tag rule for an item, or {@code null}. */
    String tagRule(String itemId);

    /** Returns the NBT-specific rule for an item, or {@code null}. */
    String nbtRule(S stack, String itemId);

    /**
     * Resolves the configured expression using the shared precedence:
     * NBT-specific, then exact item id, then item tags.
     */
    default String rule(S stack, String itemId) {
        String rule = nbtRule(stack, itemId);
        if (rule != null) {
            return rule;
        }
        rule = exactRule(itemId);
        if (rule != null) {
            return rule;
        }
        return tagRule(itemId);
    }
}
