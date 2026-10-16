package com.example.amazonMages.combat;

public final class WeaponDefinition {
    public final String name;
    public final WeaponType type;
    public final DamageEvent damage;
    public WeaponDefinition(String name, WeaponType type, DamageEvent damage) {
        this.name = name;
        this.type = type;
        this.damage = damage;
    }
}
