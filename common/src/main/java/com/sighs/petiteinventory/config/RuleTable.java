package com.sighs.petiteinventory.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loader-neutral rule index used by item-footprint and border-theme caches.
 *
 * <p>Ownership is limited to classification, storage and persistence grouping.
 * Tag and NBT resolution stay behind target-specific SPI methods.</p>
 */
public final class RuleTable<T> {
    private final Map<String, T> exact = new LinkedHashMap<>();
    private final Map<String, T> tags = new LinkedHashMap<>();
    private final Map<String, T> nbt = new LinkedHashMap<>();

    public void clear() {
        exact.clear();
        tags.clear();
        nbt.clear();
    }

    public void putExact(String key, T value) {
        exact.put(key, value);
    }

    public void putTag(String key, T value) {
        tags.put(key, value);
    }

    public void putNbt(String key, T value) {
        nbt.put(key, value);
    }

    public T exact(String key) {
        return exact.get(key);
    }

    public T tag(String key) {
        return tags.get(key);
    }

    public T nbt(String key) {
        return nbt.get(key);
    }

    public void removeExact(String key) { exact.remove(key); }
    public void removeTag(String key) { tags.remove(key); }
    public void removeNbt(String key) { nbt.remove(key); }

    public Map<String, T> tags() {
        return Collections.unmodifiableMap(tags);
    }

    /** Builds a flat map using a target-defined prefix for persisted tag keys. */
    public Map<String, T> storedMap(String tagPrefix) {
        Map<String, T> result = new LinkedHashMap<>();
        for (Map.Entry<String, T> entry : tags.entrySet()) {
            result.put(tagPrefix + entry.getKey(), entry.getValue());
        }
        result.putAll(exact);
        result.putAll(nbt);
        return result;
    }

    /** Groups rules by value for serialization, with tag matches prefixed. */
    public List<RuleGroup<T>> groups(String tagPrefix) {
        Map<T, List<String>> grouped = new LinkedHashMap<>();
        for (Map.Entry<String, T> entry : tags.entrySet()) {
            grouped.computeIfAbsent(entry.getValue(), key -> new ArrayList<>())
                    .add(tagPrefix + entry.getKey());
        }
        for (Map.Entry<String, T> entry : exact.entrySet()) {
            grouped.computeIfAbsent(entry.getValue(), key -> new ArrayList<>())
                    .add(entry.getKey());
        }
        for (Map.Entry<String, T> entry : nbt.entrySet()) {
            grouped.computeIfAbsent(entry.getValue(), key -> new ArrayList<>())
                    .add(entry.getKey());
        }
        List<RuleGroup<T>> result = new ArrayList<>();
        for (Map.Entry<T, List<String>> entry : grouped.entrySet()) {
            result.add(new RuleGroup<>(entry.getKey(), entry.getValue()));
        }
        return result;
    }
}
