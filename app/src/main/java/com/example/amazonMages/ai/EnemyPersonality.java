package com.example.amazonMages.ai;

/** Per-instance variation, chosen once when spawned. */
public enum EnemyPersonality {
    CAUTIOUS, AGGRESSIVE, SOCIAL;

    public static EnemyPersonality random(java.util.Random random) {
        int value = random.nextInt(100);
        return value < 30 ? CAUTIOUS : value < 70 ? AGGRESSIVE : SOCIAL;
    }
}
