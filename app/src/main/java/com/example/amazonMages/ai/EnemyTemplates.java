package com.example.amazonMages.ai;

/** Authored Amazonian enemy roster for Vampire Survivors wave progression. */
public final class EnemyTemplates {
    private EnemyTemplates() {}

    public static final EnemyTemplate SWARM_BAT = new EnemyTemplate(
        "swarm_bat", "Morcego Carapana", EnemyRank.NORMAL,
        new EnemyConfig(1150, 0, 0, 0, 142, 0, 0, 1, 8, 120, 60),
        1, 0, 1, 22, 16, "enemy.swarm_bat.defeated", "bat", 0, true);

    public static final EnemyTemplate ACID_GHOST = new EnemyTemplate(
        "acid_ghost", "Fantasma Acido", EnemyRank.NORMAL,
        new EnemyConfig(1050, 470, 250, 130, 118, 350, Math.toRadians(12), 2, 18, 120, 75),
        2, 1, 1, 28, 22, "enemy.acid_ghost.defeated", "acid_ghost", 0, true);

    public static final EnemyTemplate CURUPIRA_IMP = new EnemyTemplate(
        "curupira_imp", "Diabrete Curupira", EnemyRank.NORMAL,
        new EnemyConfig(1050, 360, 135, 50, 136, 320, Math.toRadians(10), 4, 12, 125, 80),
        4, 1, 1, 26, 28, "enemy.curupira_imp.defeated", "imp", 1, false);

    public static final EnemyTemplate MOSS_GOLEM = new EnemyTemplate(
        "moss_golem", "Golem de Musgo", EnemyRank.NORMAL,
        new EnemyConfig(1100, 0, 0, 0, 84, 0, 0, 12, 10, 180, 90),
        12, 0, 2, 38, 55, "enemy.moss_golem.defeated", "golem", 1, false);

    public static final EnemyTemplate VINE_SHAMAN = new EnemyTemplate(
        "vine_shaman", "Xama do Pantano", EnemyRank.NORMAL,
        new EnemyConfig(1050, 480, 260, 130, 98, 360, Math.toRadians(8), 6, 16, 115, 70),
        6, 2, 1, 28, 38, "enemy.vine_shaman.defeated", "shaman", 1, false);

    public static final EnemyTemplate SWAMP_SENTINEL = new EnemyTemplate(
        "swamp_sentinel", "Sentinela do Pantano", EnemyRank.MINI_BOSS,
        new EnemyConfig(1200, 530, 220, 85, 118, 380, Math.toRadians(9), 45, 20, 90, 55),
        45, 2, 2, 43, 120, "enemy.swamp_sentinel.defeated", "sentinel", 2, false, .5);

    public static final EnemyTemplate ANCIENT_WRAITH = new EnemyTemplate(
        "ancient_wraith", "Espectro Ancestral", EnemyRank.BOSS,
        new EnemyConfig(1300, 650, 250, 100, 132, 420, Math.toRadians(6), 110, 20, 80, 48),
        110, 3, 3, 54, 350, "enemy.ancient_wraith.defeated", "wraith", 2, true, .7, .35);

    public static final EnemyTemplate BOITATA = new EnemyTemplate(
        "boitata", "Boitatá", EnemyRank.BOSS, EnemyConfig.acidGhost(),
        144, 2, 0, 62, 460, "enemy.boitata.defeated", "boitata", 2, false, .40);
}
