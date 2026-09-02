package com.sighs.petiteinventory.config;

import java.util.List;

/** Persistence-oriented group of rules sharing one result value. */
public final class RuleGroup<T> {
    private final T value;
    private final List<String> matches;

    public RuleGroup(T value, List<String> matches) {
        this.value = value;
        this.matches = matches;
    }

    public T value() { return value; }
    public List<String> matches() { return matches; }
}
