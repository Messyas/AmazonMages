package com.example.amazonMages.ai;

import android.content.Context;
import android.graphics.Canvas;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.ecs.Entity;
import com.example.amazonMages.ecs.ProjectileComponent;
import com.example.amazonMages.ecs.ProjectileWorld;
import com.example.amazonMages.combat.DamageEvent;
import com.example.amazonMages.combat.DamageType;
import com.example.amazonMages.gameobject.Circle;
import com.example.amazonMages.gameobject.Enemy;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gameobject.Spell;
import com.example.amazonMages.map.Tilemap;
import com.example.amazonMages.map.WorldDefinition;
import java.util.Random;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Owns enemy population, Vampire Survivors wave director, soft separation, parry/melee, and collisions. */
public final class EnemyManager {
    private static final int MAX_ENEMIES = WaveDirector.MAX_CAP;
    private static final double DESPAWN_DISTANCE = 1550;
    private final com.example.amazonMages.ecs.EnemyRegistry enemies = new com.example.amazonMages.ecs.EnemyRegistry();
    private final WaveDirector waves = new WaveDirector();
    private final com.example.amazonMages.ecs.HostileProjectileWorld projectiles = new com.example.amazonMages.ecs.HostileProjectileWorld();
    private final List<Entity> impacts = new ArrayList<>();
    private final AttackDirector attacks = new AttackDirector();
    private final Context context;
    private final Player player;
    private final Tilemap tilemap;
    private final Random random = new Random();

    public interface KillListener { void onEnemyDefeated(String eventId); }
    public interface DropListener { void onDefeat(EnemyTemplate template, double x, double y); }
    public interface ParryRiposteListener { void onParrySuccess(double sourceX, double sourceY); }

    private DropListener dropListener;
    public void setDropListener(DropListener listener) { dropListener = listener; }
    public interface DetailedKillListener { void onEnemyDefeated(String eventId, long entityId); }
    private DetailedKillListener detailedKillListener;
    public void setDetailedKillListener(DetailedKillListener listener) { detailedKillListener = listener; }
    private KillListener killListener;
    public void setKillListener(KillListener listener) { killListener = listener; }
    private ParryRiposteListener parryListener;
    public void setParryListener(ParryRiposteListener listener) { parryListener = listener; }

    private int killCount;
    private int tick;

    public EnemyManager(Context context, Player player, Tilemap tilemap) {
        this.context = context.getApplicationContext();
        this.player = player;
        this.tilemap = tilemap;
    }

    public WaveDirector waveDirector() { return waves; }
    public int getKillCount() { return killCount; }

    private void reportDefeat(Enemy enemy) {
        long entityId = enemies.claimDefeat(enemy);
        if (entityId == 0) return;
        killCount++;
        EnemyTemplate template = enemy.getTemplate();
        String event = template == null ? "enemy.acid_ghost.defeated" : template.defeatEvent;
        if (dropListener != null) dropListener.onDefeat(template, enemy.getPositionX(), enemy.getPositionY());
        if (killListener != null) killListener.onEnemyDefeated(event);
        if (detailedKillListener != null) detailedKillListener.onEnemyDefeated(event, entityId);
    }

    /** Bosses are explicit or timed wave encounters, never added as normal ambient mobs. */
    public Enemy spawnEncounter(EnemyTemplate template, double x, double y) {
        if (enemies.size() >= MAX_ENEMIES + 2 || template == null || !template.isBossEncounter() ||
            !Double.isFinite(x) || !Double.isFinite(y)) return null;
        for (Enemy enemy : enemies) {
            if (!enemy.isDead() && enemy.getTemplate() != null &&
                enemy.getTemplate().id.equals(template.id)) return null;
        }
        if (tilemap != null) {
            WorldDefinition.Position safe = tilemap.world().safePosition(x, y, 32);
            x = safe.x; y = safe.y;
        }
        Enemy enemy = new Enemy(context, player, template, x, y);
        if (tilemap != null) enemy.setWorldGeometry(tilemap.world());
        enemies.add(enemy);
        return enemy;
    }

    public Enemy activeBoss() {
        for (Enemy enemy : enemies) {
            if (!enemy.isDead() && enemy.getTemplate() != null &&
                enemy.getTemplate().isBossEncounter()) return enemy;
        }
        return null;
    }

    public void update() {
        if (tick >= Integer.MAX_VALUE - 1) tick = 0;
        tick++;
        attacks.beginTick(tick);

        if (tilemap != null) {
            int spawnCount = waves.step(enemies.size());
            if (spawnCount > 0) {
                WorldDefinition.Region region = tilemap.world().regionAt(player.getPositionX(), player.getPositionY());
                for (int i = 0; i < spawnCount && enemies.size() < waves.targetCap(); i++) {
                    EnemyTemplate next = waves.pickAmbient(region, random);
                    double[] pos = tilemap.randomWalkableSpawn(
                        player.getPositionX(), player.getPositionY(), random, 500, 780, next.flying);
                    if (pos != null) {
                        EnemyPersonality personality = next == EnemyTemplates.ACID_GHOST
                            ? EnemyPersonality.random(random)
                            : EnemyPersonality.AGGRESSIVE;
                        Enemy enemy = new Enemy(context, player, next, pos[0], pos[1], personality);
                        enemy.setWorldGeometry(tilemap.world());
                        enemies.add(enemy);
                    }
                }
            }
            EnemyTemplate timedBoss = waves.checkTimedBoss();
            if (timedBoss != null) {
                double[] pos = tilemap.randomWalkableSpawn(
                    player.getPositionX(), player.getPositionY(), random, 460, 620, timedBoss.flying);
                if (pos != null) spawnEncounter(timedBoss, pos[0], pos[1]);
            }
        }

        // Social acid ghosts select nearby unfused ghosts of the same species.
        for (Enemy enemy : enemies) {
            Enemy target = null;
            boolean isAcidGhost = enemy.getTemplate() == null || "acid_ghost".equals(enemy.getTemplate().id);
            if (isAcidGhost && enemy.getPersonality() == EnemyPersonality.SOCIAL && !enemy.isFused()
                    && !enemy.isDead() && enemy.getRank() == EnemyRank.NORMAL) {
                double best = 320;
                for (Enemy other : enemies) {
                    boolean otherGhost = other.getTemplate() == null || "acid_ghost".equals(other.getTemplate().id);
                    if (!otherGhost || enemy == other || other.isFused() || other.isDead()
                            || other.getRank() != EnemyRank.NORMAL) continue;
                    double distance = Math.hypot(enemy.getPositionX() - other.getPositionX(),
                        enemy.getPositionY() - other.getPositionY());
                    if (distance < best) { best = distance; target = other; }
                }
            }
            enemy.setGroupTarget(target);
        }

        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            double distance = Math.hypot(
                enemy.getPositionX() - player.getPositionX(),
                enemy.getPositionY() - player.getPositionY());
            boolean isBoss = enemy.getTemplate() != null && enemy.getTemplate().isBossEncounter();
            if (enemy.finishedDeath() || (!isBoss && distance > DESPAWN_DISTANCE)) {
                iterator.remove();
                continue;
            }
            int period = distance < 850 ? 1 : distance < 1200 ? 4 : 16;
            if ((tick + enemy.getUpdateOffset()) % period == 0) {
                enemy.update(period);
                if (tilemap != null && !tilemap.world().inside(enemy.getPositionX() / 64, enemy.getPositionY() / 64)) {
                    WorldDefinition.Position safe = tilemap.world().safePosition(
                        enemy.getPositionX(), enemy.getPositionY(), enemy.getRadius());
                    enemy.setPosition(safe.x, safe.y);
                }
            }
        }

        // Soft separation keeps Vampire Survivors hordes readable without stacking on 1 pixel.
        int n = enemies.size();
        for (int i = 0; i < n; i++) {
            Enemy a = enemies.get(i);
            if (a.isDead()) continue;
            for (int j = i + 1; j < n; j++) {
                Enemy b = enemies.get(j);
                if (b.isDead()) continue;
                double dx = b.getPositionX() - a.getPositionX();
                double dy = b.getPositionY() - a.getPositionY();
                double dist2 = dx * dx + dy * dy;
                double minSep = (a.getRadius() + b.getRadius()) * 0.72;
                if (dist2 > 0.01 && dist2 < minSep * minSep) {
                    double dist = Math.sqrt(dist2);
                    double push = Math.min(4.0, (minSep - dist) * 0.25);
                    double nx = dx / dist * push;
                    double ny = dy / dist * push;
                    a.nudge(-nx, -ny);
                    b.nudge(nx, ny);
                }
            }
        }

        // Fuse nearby unfused social acid ghosts once.
        for (int i = 0; i < enemies.size(); i++) {
            Enemy first = enemies.get(i);
            boolean firstGhost = first.getTemplate() == null || "acid_ghost".equals(first.getTemplate().id);
            if (!firstGhost || first.isFused() || first.isDead() || first.getRank() != EnemyRank.NORMAL
                    || first.getPersonality() != EnemyPersonality.SOCIAL) continue;
            for (int j = i + 1; j < enemies.size(); j++) {
                Enemy second = enemies.get(j);
                boolean secondGhost = second.getTemplate() == null || "acid_ghost".equals(second.getTemplate().id);
                if (!secondGhost || second.isFused() || second.isDead() || second.getRank() != EnemyRank.NORMAL) continue;
                if (Math.hypot(first.getPositionX() - second.getPositionX(),
                        first.getPositionY() - second.getPositionY()) < 52) {
                    first.fuseWith(second);
                    enemies.remove(j);
                    break;
                }
            }
        }

        // Melee contact check with Parry Riposte support.
        double px = player.getPositionX(), py = player.getPositionY(), pr = player.getRadius();
        for (Enemy enemy : enemies) {
            if (enemy.isDead() || enemy.isStunned()) continue;
            double dx = enemy.getPositionX() - px, dy = enemy.getPositionY() - py;
            double dist = Math.hypot(dx, dy);
            if (dist < pr + enemy.getRadius() - 5) {
                if (player.tryParry(enemy.getPositionX(), enemy.getPositionY())) {
                    if (parryListener != null) parryListener.onParrySuccess(enemy.getPositionX(), enemy.getPositionY());
                    break;
                } else if (player.canTakeContactDamage() && enemy.getMeleeDamage() > 0) {
                    player.receiveHit(new DamageEvent(enemy.getMeleeDamage(),
                        enemy.getMeleeDamage() * 14.0, DamageType.PHYSICAL));
                    player.grantContactInvulnerability(54);
                    if (dist > 0.01) enemy.applyImpulse(dx / dist * 7.5, dy / dist * 7.5);
                    break;
                }
            }
        }

        // Rotating priority prevents the same ranged enemy from monopolizing the firing budget.
        int size = enemies.size();
        int start = attacks.firstCandidate(size);
        if (attacks.canFire(tick, projectiles.count())) {
            for (int offset = 0; offset < size; offset++) {
                int index = (start + offset) % size;
                Enemy enemy = enemies.get(index);
                if (enemy.isDead() || enemy.isStunned()) continue;
                EnemyProjectile shot = enemy.fireProjectile();
                if (shot != null) {
                    projectiles.spawn(shot);
                    attacks.recordShot(tick, index, size);
                    break;
                }
            }
        }

        if (projectiles.update(player) && parryListener != null) {
            parryListener.onParrySuccess(player.getPositionX() + player.getAttackDirectionX() * 48,
                player.getPositionY() + player.getAttackDirectionY() * 48);
        }
    }

    /** ECS projectile collision with optional piercing support. */
    public void collideWithPlayerProjectiles(ProjectileWorld world) {
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) continue;
            world.overlappingInto(enemy.getPositionX(), enemy.getPositionY(), enemy.getRadius(), impacts);
            for (Entity projectile : impacts) {
                if (enemy.isDead()) break;
                Spell spell = projectile.get(ProjectileComponent.class).spell;
                if (spell.alreadyHit(enemy.uid)) continue;
                spell.markHit(enemy.uid);
                if (spell.consumePierce()) {
                    if (!world.consume(projectile.id())) continue;
                }
                enemy.receiveHit(spell.getHit());
                if (spell.getExplosiveRadius() > 0) {
                    coconutImpact(enemy.getPositionX(), enemy.getPositionY(),
                        spell.getExplosiveRadius(), spell.getHit().amount * 0.65);
                }
                double speed = Math.hypot(spell.getVelocityX(), spell.getVelocityY());
                if (speed > 0.01) {
                    enemy.applyImpulse(spell.getVelocityX() / speed * 7.0, spell.getVelocityY() / speed * 7.0);
                }
                if (enemy.isDead()) {
                    reportDefeat(enemy);
                    break;
                }
            }
        }
    }

    public void collideWithPlayerSpells(List<Spell> spells) {
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (enemy.isDead()) continue;
            Iterator<Spell> shotIterator = spells.iterator();
            while (shotIterator.hasNext()) {
                Spell spell = shotIterator.next();
                if (Circle.isColliding(spell, enemy)) {
                    shotIterator.remove();
                    enemy.receiveHit(spell.getHit());
                    if (enemy.isDead()) { reportDefeat(enemy); break; }
                }
            }
        }
    }

    /** Area impacts used by the intrinsic coconut storm. */
    public void coconutImpact(double x, double y, double radius) {
        coconutImpact(x, y, radius, 1.0);
    }

    public void coconutImpact(double x, double y, double radius, double damageMultiplier) {
        applyAreaDamage(x, y, radius, new DamageEvent(Math.max(0, damageMultiplier), 18, DamageType.PHYSICAL), 5.0, 0);
    }

    /** Shared radial damage helper for orbital orbs, lightning strikes, and parry shockwaves. */
    public int applyAreaDamage(double x, double y, double radius, DamageEvent hit, double knockbackSpeed, int stunTicks) {
        int hits = 0;
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) continue;
            double dx = enemy.getPositionX() - x;
            double dy = enemy.getPositionY() - y;
            double distance = Math.hypot(dx, dy);
            if (distance < radius + enemy.getRadius()) {
                hits++;
                enemy.receiveHit(hit);
                if (knockbackSpeed > 0 && distance > 0.01) {
                    enemy.applyImpulse(dx / distance * knockbackSpeed, dy / distance * knockbackSpeed);
                }
                if (stunTicks > 0) enemy.stun(stunTicks);
                if (enemy.isDead()) reportDefeat(enemy);
            }
        }
        return hits;
    }

    /** Radial stun & arcane knockback triggered on successful parry or Living Bark Aegis burst. */
    public int parryShockwave(double x, double y, double radius, double damageAmount, int stunTicks) {
        return applyAreaDamage(x, y, radius,
            new DamageEvent(Math.max(1, damageAmount), 45, DamageType.MAGIC), 12.0, stunTicks);
    }

    /** Pulls enemies toward the Water Vortex center and optionally applies periodic arcane/root pulses. */
    public int pullAndDamageVortex(double vx, double vy, double radius, double pullSpeed,
                                   DamageEvent pulseHit, int rootTicks) {
        int affected = 0;
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) continue;
            double dx = vx - enemy.getPositionX();
            double dy = vy - enemy.getPositionY();
            double dist = Math.hypot(dx, dy);
            if (dist < radius + enemy.getRadius()) {
                affected++;
                if (dist > 14.0 && pullSpeed > 0) {
                    double step = Math.min(pullSpeed, dist * 0.35);
                    enemy.nudge(dx / dist * step, dy / dist * step);
                }
                if (pulseHit != null) {
                    enemy.receiveHit(pulseHit);
                    if (rootTicks > 0) enemy.stun(rootTicks);
                    if (enemy.isDead()) reportDefeat(enemy);
                }
            }
        }
        return affected;
    }

    public Enemy findNearestEnemy(double x, double y, double maxRange) {
        Enemy best = null;
        double bestDist = maxRange;
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) continue;
            double d = Math.hypot(enemy.getPositionX() - x, enemy.getPositionY() - y);
            if (d < bestDist) { bestDist = d; best = enemy; }
        }
        return best;
    }

    public Enemy findHighestHealthEnemy(double x, double y, double maxRange) {
        Enemy best = null;
        int bestHp = -1;
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) continue;
            double d = Math.hypot(enemy.getPositionX() - x, enemy.getPositionY() - y);
            if (d <= maxRange && enemy.getHealth() > bestHp) {
                bestHp = enemy.getHealth();
                best = enemy;
            }
        }
        return best;
    }

    public void draw(Canvas canvas, GameDisplay display) {
        for (Enemy enemy : enemies) enemy.draw(canvas, display);
        projectiles.draw(canvas, display);
    }

    public void addActorsTo(List<com.example.amazonMages.gameobject.GameObject> target) {
        target.addAll(enemies);
    }
    public void drawProjectiles(Canvas canvas, GameDisplay display) { projectiles.draw(canvas, display); }

    /** Clears hostile projectiles and creates breathing room when player revives at their spot. */
    public void onPlayerRespawn() {
        projectiles.clear();
        double px = player.getPositionX();
        double py = player.getPositionY();
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) continue;
            double dx = enemy.getPositionX() - px;
            double dy = enemy.getPositionY() - py;
            double dist = Math.hypot(dx, dy);
            if (dist < 260) {
                double angle = dist < 0.001 ? Math.random() * Math.PI * 2 : Math.atan2(dy, dx);
                enemy.setPosition(px + Math.cos(angle) * 300, py + Math.sin(angle) * 300);
            }
        }
    }

    /** Debug only: removes active enemies and projectiles without kill rewards. */
    public void clearForDebug() {
        enemies.clear();
        projectiles.clear();
    }

    /** Debug-only normal enemy spawn; bypasses the ambient spawn timer. */
    public boolean spawnNormalForDebug(double x, double y) {
        return spawnDebugSpecies(EnemyTemplates.ACID_GHOST, x, y);
    }

    public boolean spawnDebugSpecies(EnemyTemplate template, double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || enemies.size() >= MAX_ENEMIES || template == null) return false;
        if (tilemap != null) {
            WorldDefinition.Position safe = tilemap.world().safePosition(x, y, 32);
            x = safe.x; y = safe.y;
        }
        Enemy enemy = new Enemy(context, player, template, x, y, EnemyPersonality.AGGRESSIVE);
        if (tilemap != null) enemy.setWorldGeometry(tilemap.world());
        enemies.add(enemy);
        return true;
    }

    public int getEnemyCount() { return enemies.size(); }
    public int getProjectileCount() { return projectiles.count(); }
}
