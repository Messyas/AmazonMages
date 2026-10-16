package com.example.amazonMages.ecs;

import com.example.amazonMages.gameobject.Spell;

/** Temporary bridge to the existing Canvas projectile visuals and collision API. */
public final class ProjectileComponent implements Component {
    public final Spell spell;
    public ProjectileComponent(Spell spell) {
        if (spell == null) throw new IllegalArgumentException("spell");
        this.spell = spell;
    }
}
