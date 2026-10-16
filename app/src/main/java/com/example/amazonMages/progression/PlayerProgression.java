package com.example.amazonMages.progression;

import com.example.amazonMages.save.PlayerStatsEntity;

/** Seven attributes and level progression; no combat scaling is applied until balancing is defined. */
public final class PlayerProgression {
    public int level = 1;
    public long experience = 0;
    public int attributePoints = 0;
    public int skillPoints = 0;
    public int vigor = 1, mente = 1, resistencia = 1, inteligencia = 1, destreza = 1, arcano = 1, forca = 1;
    public double mana = 100;
    public double maxMana = 100;

    public int maximumHealth() { return 5 + Math.max(0, vigor - 1); }
    public double manaCapacity() { return 100 + Math.max(0, mente - 1) * 10.0; }
    public double castSpeed() { return Math.min(2.0, 1 + Math.max(0, destreza - 1) * .035); }
    public double magicMultiplier() { return 1 + Math.max(0, inteligencia - 1) * .06; }
    public double physicalMultiplier() { return 1 + Math.max(0, forca - 1) * .06; }
    public void regenerateMana() {
        mana = Math.min(maxMana, mana + (1.5 + Math.max(0, mente - 1) * .15) / 60.0);
    }
    public boolean spendMana(double amount) {
        if (!Double.isFinite(amount) || amount < 0 || mana < amount) return false;
        mana -= amount;
        return true;
    }

    public static long experienceToNextLevel(int level) {
        return 100L * Math.max(1, level);
    }

    /** XP can grant multiple levels, but never awards XP from events automatically. */
    public void gainExperience(long amount) {
        if (amount <= 0) return;
        experience = amount > Long.MAX_VALUE - experience ? Long.MAX_VALUE : experience + amount;
        while (level < 100 && experience >= experienceToNextLevel(level)) {
            experience -= experienceToNextLevel(level);
            level++;
            attributePoints++;
            skillPoints++;
        }
    }

    public boolean allocate(String attribute) {
        if (attributePoints <= 0 || attribute == null) return false;
        switch (attribute) {
            case "vigor": vigor++; break;
            case "mente": mente++; break;
            case "resistencia": resistencia++; break;
            case "inteligencia": inteligencia++; break;
            case "destreza": destreza++; break;
            case "arcano": arcano++; break;
            case "forca": forca++; break;
            default: return false;
        }
        attributePoints--;
        return true;
    }

    public PlayerStatsEntity snapshot(int slotId) {
        return new PlayerStatsEntity(slotId, level, experience, attributePoints, skillPoints,
            vigor, mente, resistencia, inteligencia, destreza, arcano, forca, mana, maxMana);
    }

    public void restore(PlayerStatsEntity row) {
        if (row == null) return;
        level = Math.max(1, Math.min(100, row.level));
        experience = Math.max(0, row.experience);
        attributePoints = Math.max(0, row.attributePoints);
        skillPoints = Math.max(0, row.skillPoints);
        vigor = Math.max(1, row.vigor);
        mente = Math.max(1, row.mente);
        resistencia = Math.max(1, row.resistencia);
        inteligencia = Math.max(1, row.inteligencia);
        destreza = Math.max(1, row.destreza);
        arcano = Math.max(1, row.arcano);
        forca = Math.max(1, row.forca);
        maxMana = Double.isFinite(row.maxMana) ? Math.max(1, row.maxMana) : 100;
        mana = Double.isFinite(row.mana) ? Math.max(0, Math.min(maxMana, row.mana)) : maxMana;
    }
}
