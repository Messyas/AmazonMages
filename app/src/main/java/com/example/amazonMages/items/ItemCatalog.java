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
            .register(new ItemDefinition("coco", "Dominio dos Cocos", "Municao viva (x25): fortalece ataques/especiais ou use (+1 Vida e +15 Mana).", 25, WeaponLoadout.COCONUT, new GameplayEffect(HEAL, 1, 0, DamageType.PHYSICAL)))
            .register(new ItemDefinition("cajado", "Cajado do Pantano", "Dispara energia magica.", 1, STAFF))
            .register(new ItemDefinition("elixir", "Elixir da Castanheira", "Cura 4 de vida e fortalece a vitalidade.", 10, null, new GameplayEffect(HEAL, 4, 0, DamageType.MAGIC)))
            .register(new ItemDefinition("bomba_coco", "Bomba de Pororoca", "Explode onda arcana de 240px e recarrega o Especial.", 15, null, new GameplayEffect(BOOST, .15, 120, DamageType.MAGIC)))
            .register(new ItemDefinition("tonico", "Tonico de Tupa", "+30% velocidade/defesa por 10s e +30 Mana.", 10, null, new GameplayEffect(BOOST, .30, 600, DamageType.MAGIC)))
            .register(new ItemDefinition("runa_ancestral", "Runa da Floresta", "Use para ganhar +1 Ponto de Habilidade permanente!", 10, null, new GameplayEffect(BOOST, .20, 180, DamageType.MAGIC)));
    }
    public static String weaponId(WeaponDefinition weapon) {
        if (weapon == WeaponLoadout.COCONUT) return "coco";
        if (weapon == STAFF) return "cajado";
        return null;
    }
}
