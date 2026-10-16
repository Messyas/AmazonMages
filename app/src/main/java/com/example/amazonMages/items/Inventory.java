package com.example.amazonMages.items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Quantity-based inventory with bounded per-item stacks; no save persistence yet. */
public final class Inventory {
    private final ItemRegistry registry;
    private final Map<String, Integer> amounts = new LinkedHashMap<>();
    public Inventory(ItemRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("registry");
        this.registry = registry;
    }
    public int count(String id) { Integer n = amounts.get(id); return n == null ? 0 : n; }
    public int add(String id, int quantity) {
        ItemDefinition definition = registry.get(id);
        if (definition == null || quantity < 1) throw new IllegalArgumentException("item/quantity");
        int accepted = Math.min(quantity, definition.maxStack - count(id));
        if (accepted > 0) amounts.put(id, count(id) + accepted);
        return accepted;
    }
    public boolean remove(String id, int quantity) {
        if (quantity < 1 || count(id) < quantity) return false;
        int remaining = count(id) - quantity;
        if (remaining == 0) amounts.remove(id);
        else amounts.put(id, remaining);
        return true;
    }
    /** Restore only known, positive item stacks; unknown IDs are ignored. */
    public void restore(Map<String, Integer> saved) {
        amounts.clear();
        if (saved == null) return;
        for (Map.Entry<String, Integer> entry : saved.entrySet()) {
            ItemDefinition definition = registry.get(entry.getKey());
            Integer count = entry.getValue();
            if (definition != null && count != null && count > 0)
                amounts.put(entry.getKey(), Math.min(count, definition.maxStack));
        }
    }

    public Map<String, Integer> snapshot() { return Collections.unmodifiableMap(new LinkedHashMap<>(amounts)); }
}
