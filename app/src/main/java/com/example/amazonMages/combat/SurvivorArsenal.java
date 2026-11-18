package com.example.amazonMages.combat;

import com.example.amazonMages.progression.PlayerProgression;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure-Java Vampire Survivors progression & auto-cast weapon coordinator.
 * Integrates original Coconut/Leaf/Parry mechanics with orbital Pororoca Orbs and Vine Lightning.
 */
public final class SurvivorArsenal {
    public static final int MAX_WEAPON_LEVEL = 5;

    public enum UpgradeKind {
        COCONUT, LEAF, ORBS, LIGHTNING, PARRY, HASTE, MAGNET, VIGOR
    }

    public static final class UpgradeOption {
        public final UpgradeKind kind;
        public final String title, description, badge;
        public final int pickupIconIndex;

        public UpgradeOption(UpgradeKind kind, String title, String description, String badge, int pickupIconIndex) {
            this.kind = kind;
            this.title = title;
            this.description = description;
            this.badge = badge;
            this.pickupIconIndex = pickupIconIndex;
        }
    }

    public static final class LightningStrikeFx {
        public final double x, y;
        public int ticksRemaining;
        public LightningStrikeFx(double x, double y, int ticksRemaining) {
            this.x = x;
            this.y = y;
            this.ticksRemaining = ticksRemaining;
        }
    }

    public interface AutoCastSink {
        void fireCoconutFan(int count, double damageMultiplier);
        void fireLeafBurst(int count, int pierce, double damageMultiplier);
        void pulseOrbDamage(double orbX, double orbY, double radius, double damageMultiplier);
        void strikeLightningTarget(double damageMultiplier, double areaRadius);
    }

    private int coconutLevel = 1;
    private int leafLevel = 0;
    private int orbLevel = 0;
    private int lightningLevel = 0;
    private int parryLevel = 1;

    private int coconutCooldown = 18;
    private int leafCooldown = 10;
    private int orbPulseCooldown = 0;
    private int lightningCooldown = 30;
    private double orbAngle = 0;
    private int autoCoconutVolley = 0;
    private boolean lastVolleyExplosive = false;
    private com.example.amazonMages.progression.SkillTree skillTree;
    private final List<LightningStrikeFx> strikes = new ArrayList<>();

    public void setSkillTree(com.example.amazonMages.progression.SkillTree tree) {
        skillTree = tree;
    }

    public double activeExplosiveRadius() {
        return skillTree == null ? 0.0 : skillTree.explosiveCoconutRadius();
    }

    public boolean pollExplosiveVolley() {
        boolean result = lastVolleyExplosive;
        lastVolleyExplosive = false;
        return result;
    }

    public void reduceAllCooldowns(int ticks) {
        int t = Math.max(0, ticks);
        coconutCooldown = Math.max(0, coconutCooldown - t);
        leafCooldown = Math.max(0, leafCooldown - t);
        lightningCooldown = Math.max(0, lightningCooldown - t);
    }

    public int coconutLevel() { return coconutLevel; }
    public int leafLevel() { return leafLevel; }
    public int orbLevel() { return orbLevel; }
    public int lightningLevel() { return lightningLevel; }
    public int parryLevel() { return parryLevel; }

    public void unlockLeafFromStaff() {
        if (leafLevel == 0) leafLevel = 1;
    }

    public int orbCount() { return orbLevel <= 0 ? 0 : 1 + orbLevel; }
    public double orbOrbitRadius() { return 98.0 + orbLevel * 10.0; }
    public double orbAngle() { return orbAngle; }

    public double parryRadius() { return 185.0 + (parryLevel - 1) * 35.0; }
    public double parryDamage(PlayerProgression progression) {
        double mult = progression == null ? 1.0 : Math.max(progression.magicMultiplier(), progression.physicalMultiplier());
        return (3.5 + parryLevel * 1.5) * mult;
    }
    public int parryStunTicks() { return 42 + (parryLevel - 1) * 12; }
    public double parryManaRestore() { return 20.0 + (parryLevel - 1) * 4.0; }

    public double magnetRadius(PlayerProgression progression) {
        int arcano = progression == null ? 1 : progression.arcano;
        return 145.0 + Math.max(0, arcano - 1) * 24.0;
    }

    public void addStrikeFx(double x, double y) {
        strikes.add(new LightningStrikeFx(x, y, 20));
    }

    public List<LightningStrikeFx> strikes() { return strikes; }

    public void update(double playerX, double playerY, boolean hasTarget,
                       PlayerProgression progression, AutoCastSink sink) {
        for (int i = strikes.size() - 1; i >= 0; i--) {
            if (--strikes.get(i).ticksRemaining <= 0) strikes.remove(i);
        }
        double castSpeed = progression == null ? 1.0 : progression.castSpeed();
        double physMult = progression == null ? 1.0 : progression.physicalMultiplier();
        double magicMult = progression == null ? 1.0 : progression.magicMultiplier();

        if (coconutLevel > 0) {
            if (coconutCooldown > 0) coconutCooldown--;
            if (coconutCooldown <= 0 && hasTarget) {
                int count = coconutLevel >= 5 ? 4 : coconutLevel >= 3 ? 3 : coconutLevel >= 2 ? 2 : 1;
                double dmg = (1.2 + (coconutLevel - 1) * 0.3) * physMult;
                autoCoconutVolley++;
                lastVolleyExplosive = activeExplosiveRadius() > 0 && autoCoconutVolley % 3 == 0;
                sink.fireCoconutFan(count, dmg);
                int baseCd = Math.max(15, 30 - (coconutLevel - 1) * 4);
                coconutCooldown = Math.max(10, (int) Math.round(baseCd / castSpeed));
            }
        }

        if (leafLevel > 0) {
            if (leafCooldown > 0) leafCooldown--;
            if (leafCooldown <= 0 && hasTarget) {
                int count = leafLevel >= 4 ? 3 : leafLevel >= 2 ? 2 : 1;
                int pierce = 1 + leafLevel;
                double dmg = (0.9 + (leafLevel - 1) * 0.28) * magicMult;
                sink.fireLeafBurst(count, pierce, dmg);
                int baseCd = Math.max(14, 34 - (leafLevel - 1) * 4);
                leafCooldown = Math.max(10, (int) Math.round(baseCd / castSpeed));
            }
        }

        if (orbLevel > 0) {
            orbAngle = (orbAngle + (0.042 + orbLevel * 0.005)) % (Math.PI * 2);
            if (orbPulseCooldown > 0) orbPulseCooldown--;
            if (orbPulseCooldown <= 0) {
                int count = orbCount();
                double orbitR = orbOrbitRadius();
                double dmg = (0.85 + (orbLevel - 1) * 0.35) * magicMult;
                for (int i = 0; i < count; i++) {
                    double angle = orbAngle + i * (Math.PI * 2 / count);
                    double ox = playerX + Math.cos(angle) * orbitR;
                    double oy = playerY + Math.sin(angle) * orbitR;
                    sink.pulseOrbDamage(ox, oy, 34.0, dmg);
                }
                orbPulseCooldown = 16;
            }
        }

        if (lightningLevel > 0) {
            if (lightningCooldown > 0) lightningCooldown--;
            if (lightningCooldown <= 0 && hasTarget) {
                double dmg = (2.4 + (lightningLevel - 1) * 0.9) * magicMult;
                double area = 78.0 + (lightningLevel - 1) * 10.0;
                sink.strikeLightningTarget(dmg, area);
                int baseCd = Math.max(48, 110 - (lightningLevel - 1) * 14);
                lightningCooldown = Math.max(32, (int) Math.round(baseCd / castSpeed));
            }
        }
    }

    public List<UpgradeOption> generateChoices(Random random) {
        List<UpgradeOption> pool = new ArrayList<>();
        if (coconutLevel < MAX_WEAPON_LEVEL) {
            pool.add(new UpgradeOption(UpgradeKind.COCONUT, "Dominio dos Cocos",
                coconutLevel == 1 ? "Dispara 2 cocos em leque automaticamente."
                    : "Mais cocos, cadencia e dano fisico.",
                "Nv. " + coconutLevel + " -> " + (coconutLevel + 1), 7));
        }
        if (leafLevel < MAX_WEAPON_LEVEL) {
            pool.add(new UpgradeOption(UpgradeKind.LEAF, "Lamina da Folha",
                leafLevel == 0 ? "Lanca folhas arcanas que perfuram a horda."
                    : "Mais laminas perfurantes e dano magico.",
                leafLevel == 0 ? "NOVO!" : "Nv. " + leafLevel + " -> " + (leafLevel + 1), 6));
        }
        if (orbLevel < MAX_WEAPON_LEVEL) {
            pool.add(new UpgradeOption(UpgradeKind.ORBS, "Orbes da Pororoca",
                orbLevel == 0 ? "Esferas d'agua orbitam e repelem inimigos."
                    : "+1 orbe orbital e maior alcance.",
                orbLevel == 0 ? "NOVO!" : "Nv. " + orbLevel + " -> " + (orbLevel + 1), 4));
        }
        if (lightningLevel < MAX_WEAPON_LEVEL) {
            pool.add(new UpgradeOption(UpgradeKind.LIGHTNING, "Raio de Cipo",
                lightningLevel == 0 ? "Raios periodicos enraizam e ferem chefes."
                    : "Raios mais frequentes e maior area.",
                lightningLevel == 0 ? "NOVO!" : "Nv. " + lightningLevel + " -> " + (lightningLevel + 1), 2));
        }
        if (parryLevel < MAX_WEAPON_LEVEL) {
            pool.add(new UpgradeOption(UpgradeKind.PARRY, "Contra-Parry Arcano",
                "Amplia onda de choque do Parry, stun e mana.",
                "Nv. " + parryLevel + " -> " + (parryLevel + 1), 1));
        }
        pool.add(new UpgradeOption(UpgradeKind.HASTE, "Foco & Celeridade",
            "+1 Destreza e +1 Inteligencia (cadencia e magia).", "PASSIVO", 5));
        pool.add(new UpgradeOption(UpgradeKind.MAGNET, "Ima de Mana",
            "+2 Arcano e +1 Mente (atrai cristais mais longe).", "PASSIVO", 0));
        pool.add(new UpgradeOption(UpgradeKind.VIGOR, "Vigor da Floresta",
            "+1 Vida maxima e cura +2 coracoes agora.", "CURA", 3));

        // Prioritize unlocking new weapons on early level-ups before shuffling remaining options.
        Collections.shuffle(pool, random);
        if (leafLevel == 0 || orbLevel == 0 || lightningLevel == 0) {
            pool.sort((a, b) -> Boolean.compare("NOVO!".equals(b.badge), "NOVO!".equals(a.badge)));
        }
        return new ArrayList<>(pool.subList(0, Math.min(3, pool.size())));
    }

    /** Applies the chosen upgrade and returns HP to heal immediately. */
    public int apply(UpgradeKind kind, PlayerProgression progression) {
        switch (kind) {
            case COCONUT:
                if (coconutLevel < MAX_WEAPON_LEVEL) coconutLevel++;
                if (progression != null) progression.forca++;
                return 0;
            case LEAF:
                if (leafLevel < MAX_WEAPON_LEVEL) leafLevel++;
                if (progression != null) progression.inteligencia++;
                return 0;
            case ORBS:
                if (orbLevel < MAX_WEAPON_LEVEL) orbLevel++;
                if (progression != null) progression.arcano++;
                return 0;
            case LIGHTNING:
                if (lightningLevel < MAX_WEAPON_LEVEL) lightningLevel++;
                if (progression != null) progression.inteligencia++;
                return 0;
            case PARRY:
                if (parryLevel < MAX_WEAPON_LEVEL) parryLevel++;
                if (progression != null) progression.resistencia++;
                return 0;
            case HASTE:
                if (progression != null) {
                    progression.destreza++;
                    progression.inteligencia++;
                }
                return 0;
            case MAGNET:
                if (progression != null) {
                    progression.arcano += 2;
                    progression.mente++;
                }
                return 0;
            case VIGOR:
                if (progression != null) progression.vigor++;
                return 2;
            default:
                return 0;
        }
    }
}

