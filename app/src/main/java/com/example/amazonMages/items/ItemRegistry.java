package com.example.amazonMages.items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit registry: unknown IDs are rejected rather than silently invented. */
public final class ItemRegistry {
    private final Map<String, ItemDefinition> definitions = new LinkedHashMap<>();
    public ItemRegistry register(ItemDefinition definition) {
        if (definition == null || definitions.containsKey(definition.id))
            throw new IllegalArgumentException("duplicate or null item");
        definitions.put(definition.id, definition);
        return this;
    }
    public ItemDefinition get(String id) { return definitions.get(id); }
    public Map<String, ItemDefinition> all() { return Collections.unmodifiableMap(definitions); }
}
