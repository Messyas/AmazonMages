package com.example.amazonMages.ai;

/** Examples can be replaced with authored content without changing runtime logic. */
public final class EnemyTemplates {
    private EnemyTemplates() {}

    public static final EnemyTemplate ACID_GHOST = new EnemyTemplate(
        "acid_ghost", "Fantasma Acido", EnemyRank.NORMAL, EnemyConfig.acidGhost(),
        4, 1, 30, 10, "enemy.acid_ghost.defeated");

    public static final EnemyTemplate SWAMP_SENTINEL = new EnemyTemplate(
        "swamp_sentinel", "Sentinela do Pantano", EnemyRank.MINI_BOSS,
        new EnemyConfig(750, 530, 250, 110, 115, 370, Math.toRadians(9),
            28, 40, 95, 60),
        28, 2, 43, 80, "enemy.swamp_sentinel.defeated", .5);

    public static final EnemyTemplate ANCIENT_WRAITH = new EnemyTemplate(
        "ancient_wraith", "Espectro Ancestral", EnemyRank.BOSS,
        new EnemyConfig(950, 650, 300, 125, 105, 400, Math.toRadians(6),
            90, 60, 90, 55),
        90, 3, 58, 300, "enemy.ancient_wraith.defeated", .7, .35);
}
