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

    /** Returns the configured expression, such as {@code 2*1}, or {@code null}. */
    String rule(S stack, String itemId);
}
