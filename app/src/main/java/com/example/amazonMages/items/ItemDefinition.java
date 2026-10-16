package com.example.amazonMages.items;

/** Immutable content definition; instances only store ID and quantity. */
public final class ItemDefinition {
    public final String id;
    public final String name;
    public final int maxStack;
    public final String description;
    public final com.example.amazonMages.effects.GameplayEffect effect;
    public final com.example.amazonMages.combat.WeaponDefinition weapon;
    public ItemDefinition(String id, String name, int maxStack) {
        this(id, name, "", maxStack, null);
    }
    public ItemDefinition(String id, String name, String description, int maxStack,
            com.example.amazonMages.combat.WeaponDefinition weapon) {
        this(id,name,description,maxStack,weapon,null);
    }
    public ItemDefinition(String id,String name,String description,int maxStack,
            com.example.amazonMages.combat.WeaponDefinition weapon, com.example.amazonMages.effects.GameplayEffect effect) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()
            || maxStack < 1) throw new IllegalArgumentException("item");
        this.id = id;
        this.name = name;
        this.maxStack = maxStack;
        this.description = description == null ? "" : description;
        this.weapon = weapon;
        this.effect = effect;
    }
}
