package com.example.amazonMages.effects;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.progression.PlayerProgression;
import com.example.amazonMages.combat.*;

/** Keeps Player health authoritative and progression mana authoritative. */
public final class PlayerEffectTarget implements GameplayEffect.Target {
    private final Player player;
    private final PlayerProgression progression;
    public PlayerEffectTarget(Player player, PlayerProgression progression) { this.player=player; this.progression=progression; }
    public boolean damage(DamageEvent hit) {
        double before=player.getPreciseHealth(); player.receiveHit(hit); return player.getPreciseHealth()<before;
    }
    public boolean heal(double amount) { return player.heal(amount); }
    public boolean mana(double amount) {
        if (player.getHealthPoint()<=0 || progression.mana>=progression.maxMana) return false;
        progression.mana=Math.min(progression.maxMana,progression.mana+amount); return true;
    }
    public StatusEffects statuses() { return player.getStatusEffects(); }
}
