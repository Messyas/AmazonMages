package com.example.amazonMages.items;

/** Immutable content definition; instances only store ID and quantity. */
public final class ItemDefinition {
    public final String id;
    public final String name;
    public final int maxStack;
    public ItemDefinition(String id, String name, int maxStack) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()
            || maxStack < 1) throw new IllegalArgumentException("item");
        this.id = id;
        this.name = name;
        this.maxStack = maxStack;
    }
}
