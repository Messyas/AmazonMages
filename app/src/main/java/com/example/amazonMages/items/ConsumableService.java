package com.example.amazonMages.items;

import com.example.amazonMages.effects.GameplayEffect;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.progression.PlayerProgression;
import com.example.amazonMages.progression.SkillTree;
import java.util.Random;

/** Gameplay rules for consuming inventory items, alchemy scaling, and combat quick-use. */
public final class ConsumableService {
    public static final String HEALING_HERB = "erva";
    private static final int HERB_HEAL = 2;

    public boolean use(Inventory inventory, ItemDefinition item, GameplayEffect.Target target) {
        if (item == null || item.effect == null || inventory.count(item.id) == 0) return false;
        // Preserve the last coconut as permanent weapon seed.
        if ("coco".equals(item.id) && inventory.count("coco") <= 1) return false;
        if (!item.effect.apply(target)) return false;
        return inventory.remove(item.id, 1);
    }

    public int useHealingHerb(Inventory inventory, int health, int maximumHealth) {
        if (inventory == null || maximumHealth < 1 || health < 0)
            throw new IllegalArgumentException("health/inventory");
        if (health >= maximumHealth || inventory.count(HEALING_HERB) == 0)
            return health;
        if (!inventory.remove(HEALING_HERB, 1)) return health;
        return (int) Math.min(maximumHealth, (long) health + HERB_HEAL);
    }

    /** Uses a restorative, buff, bomb, or skill rune with alchemy scaling and flora conservation. */
    public boolean useInCombat(Inventory inventory, ItemDefinition item, Player player,
                               PlayerProgression progression, SkillTree skills, Random random) {
        if (inventory == null || item == null || item.effect == null || player == null
                || player.getHealthPoint() <= 0) return false;
        int count = inventory.count(item.id);
        if (count == 0) return false;
        double mult = skills == null ? 1.0 : skills.alchemyMultiplier();
        boolean applied;
        boolean allowConserve = true;

        if ("runa_ancestral".equals(item.id)) {
            if (!useSkillRune(inventory, progression)) return false;
            player.getStatusEffects().grantBoost(180, 0.20, 0.20);
            return true;
        } else if ("bomba_coco".equals(item.id)) {
            player.getStatusEffects().grantBoost(120, 0.15, 0.15);
            applied = true;
        } else if ("tonico".equals(item.id)) {
            player.getStatusEffects().grantBoost(600, 0.30, 0.30);
            restoreMana(progression, 30.0 * mult);
            applied = true;
        } else if ("coco".equals(item.id)) {
            if (count <= 1) return false;
            boolean needHp = player.getPreciseHealth() < player.getMaximumHealth();
            boolean needMana = progression != null && progression.mana < progression.maxMana;
            if (!needHp && !needMana) return false;
            boolean healed = needHp && player.heal(1.0 * mult);
            boolean gainedMana = needMana && restoreMana(progression, 15.0 * mult);
            applied = healed || gainedMana;
        } else if (item.effect.kind == GameplayEffect.Kind.HEAL) {
            applied = player.heal(item.effect.amount * mult);
        } else if (item.effect.kind == GameplayEffect.Kind.MANA) {
            applied = restoreMana(progression, item.effect.amount * mult);
        } else {
            applied = item.effect.apply(new com.example.amazonMages.effects.PlayerEffectTarget(player, progression));
        }

        if (!applied) return false;
        if (skills != null && skills.rank("alquimia_viva") > 0) {
            player.getStatusEffects().grantBoost(180, 0.18, 0.18);
        }
        boolean conserve = allowConserve && skills != null && skills.hasFloraSymphony()
            && random != null && random.nextDouble() < 0.30;
        if (!conserve) inventory.remove(item.id, 1);
        return true;
    }

    /** Consumes one Ancestral Forest Rune to permanently award +1 Skill Tree point (never conserved for free). */
    public boolean useSkillRune(Inventory inventory, PlayerProgression progression) {
        if (inventory == null || progression == null || inventory.count("runa_ancestral") <= 0) return false;
        if (!inventory.remove("runa_ancestral", 1)) return false;
        progression.skillPoints++;
        return true;
    }

    /** Smartly consumes the most helpful available restorative or tonic for the quick-item HUD button. */
    public String quickUse(Inventory inventory, ItemRegistry registry, Player player,
                           PlayerProgression progression, SkillTree skills, Random random) {
        if (inventory == null || registry == null || player == null || player.getHealthPoint() <= 0) return null;
        if (player.getPreciseHealth() <= player.getMaximumHealth() - 3.0 && inventory.count("elixir") > 0
                && useInCombat(inventory, registry.get("elixir"), player, progression, skills, random)) {
            return "Elixir da Castanheira! ++Vida";
        }
        if (player.getPreciseHealth() < player.getMaximumHealth() && inventory.count("erva") > 0
                && useInCombat(inventory, registry.get("erva"), player, progression, skills, random)) {
            return "Erva usada! +Vida";
        }
        if (player.getPreciseHealth() < player.getMaximumHealth() && inventory.count("elixir") > 0
                && useInCombat(inventory, registry.get("elixir"), player, progression, skills, random)) {
            return "Elixir da Castanheira! +Vida";
        }
        if (progression != null && progression.mana < progression.maxMana * 0.70 && inventory.count("mana") > 0
                && useInCombat(inventory, registry.get("mana"), player, progression, skills, random)) {
            return "Essencia! +Mana";
        }
        if (inventory.count("tonico") > 0 && player.getStatusEffects().remainingTicks() < 90
                && useInCombat(inventory, registry.get("tonico"), player, progression, skills, random)) {
            return "Tonico de Tupa! +Vel +Mana";
        }
        if ((player.getPreciseHealth() < player.getMaximumHealth()
                || (progression != null && progression.mana < progression.maxMana * 0.65))
                && inventory.count("coco") > 2
                && useInCombat(inventory, registry.get("coco"), player, progression, skills, random)) {
            return "Agua de Coco! +Vida +Mana";
        }
        if (inventory.count("bencao") > 0 && player.getStatusEffects().remainingTicks() < 60
                && useInCombat(inventory, registry.get("bencao"), player, progression, skills, random)) {
            return "Bencao da Floresta ativa!";
        }
        if (inventory.count("bomba_coco") > 0
                && useInCombat(inventory, registry.get("bomba_coco"), player, progression, skills, random)) {
            return "BOMBA DE POROROCA!";
        }
        return null;
    }

    private static boolean restoreMana(PlayerProgression progression, double amount) {
        if (progression == null || amount <= 0 || progression.mana >= progression.maxMana) return false;
        progression.mana = Math.min(progression.maxMana, progression.mana + amount);
        return true;
    }
}
