package com.example.amazonMages.save;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One progression record per save slot. */
@Entity(tableName = "player_stats")
public class PlayerStatsEntity {
    @PrimaryKey public int slotId;
    public int level;
    public long experience;
    public int attributePoints, skillPoints;
    public int vigor, mente, resistencia, inteligencia, destreza, arcano, forca;
    public double mana, maxMana;

    public PlayerStatsEntity(int slotId, int level, long experience, int attributePoints,
        int skillPoints, int vigor, int mente, int resistencia, int inteligencia,
        int destreza, int arcano, int forca, double mana, double maxMana) {
        this.slotId = slotId;
        this.level = level;
        this.experience = experience;
        this.attributePoints = attributePoints;
        this.skillPoints = skillPoints;
        this.vigor = vigor;
        this.mente = mente;
        this.resistencia = resistencia;
        this.inteligencia = inteligencia;
        this.destreza = destreza;
        this.arcano = arcano;
        this.forca = forca;
        this.mana = mana;
        this.maxMana = maxMana;
    }
}
