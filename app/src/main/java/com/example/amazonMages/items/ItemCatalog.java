package com.example.amazonMages.items;

import com.example.amazonMages.combat.*;
import com.example.amazonMages.effects.GameplayEffect;
import static com.example.amazonMages.effects.GameplayEffect.Kind.*;

/** Stable IDs are part of the save format. */
public final class ItemCatalog {
    public static final WeaponDefinition STAFF = new WeaponDefinition("Cajado do Pantano",
        WeaponType.INTRINSIC, new DamageEvent(1, 10, DamageType.MAGIC));
    private ItemCatalog() {}
    public static ItemRegistry create() {
        return new ItemRegistry()
            .register(new ItemDefinition("erva", "Erva do Pantano", "Recupera 2 de vida.", 99, null, new GameplayEffect(HEAL,2,0,DamageType.MAGIC)))
            .register(new ItemDefinition("mana", "Essencia de Mana", "Recupera 25 de mana.", 99, null, new GameplayEffect(MANA,25,0,DamageType.MAGIC)))
            .register(new ItemDefinition("bencao", "Bencao da Floresta", "Velocidade e defesa por 5s.", 20, null, new GameplayEffect(BOOST,.15,300,DamageType.MAGIC)))
            .register(new ItemDefinition("coco", "Dominio dos Cocos", "Arremesso e Tempestade de Cocos.", 1, WeaponLoadout.COCONUT))
            .register(new ItemDefinition("cajado", "Cajado do Pantano", "Dispara energia magica.", 1, STAFF));
    }
    public static String weaponId(WeaponDefinition weapon) {
        if (weapon == WeaponLoadout.COCONUT) return "coco";
        if (weapon == STAFF) return "cajado";
        return null;
    }
}
