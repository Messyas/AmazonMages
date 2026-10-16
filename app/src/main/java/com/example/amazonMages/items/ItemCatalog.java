package com.example.amazonMages.items;

import com.example.amazonMages.combat.*;

/** Stable IDs are part of the save format. */
public final class ItemCatalog {
    public static final WeaponDefinition STAFF = new WeaponDefinition("Cajado do Pantano",
        WeaponType.INTRINSIC, new DamageEvent(1, 10, DamageType.MAGIC));
    private ItemCatalog() {}
    public static ItemRegistry create() {
        return new ItemRegistry()
            .register(new ItemDefinition("erva", "Erva do Pantano", "Recupera 2 de vida.", 99, null))
            .register(new ItemDefinition("mana", "Essencia de Mana", "Recupera 25 de mana.", 99, null))
            .register(new ItemDefinition("coco", "Dominio dos Cocos", "Arremesso e Tempestade de Cocos.", 1, WeaponLoadout.COCONUT))
            .register(new ItemDefinition("cajado", "Cajado do Pantano", "Dispara energia magica.", 1, STAFF));
    }
    public static String weaponId(WeaponDefinition weapon) {
        if (weapon == WeaponLoadout.COCONUT) return "coco";
        if (weapon == STAFF) return "cajado";
        return null;
    }
}
