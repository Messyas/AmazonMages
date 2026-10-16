package com.example.amazonMages.ai;

import android.content.Context;
import android.graphics.Canvas;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.combat.DamageEvent;
import com.example.amazonMages.combat.DamageType;
import com.example.amazonMages.gameobject.Circle;
import com.example.amazonMages.gameobject.Enemy;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gameobject.Spell;
import com.example.amazonMages.map.Tilemap;
import java.util.Random;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Owns enemy population, AI scheduling, attack coordination and projectile collisions. */
public final class EnemyManager {
    private static final int MAX_ENEMIES = 36;
    private static final double DESPAWN_DISTANCE = 1900;
    private static final double ACTIVE_DISTANCE = 800;
    private static final double MID_DISTANCE = 1250;
    private static final int MID_UPDATE_TICKS = 6;
    private static final int FAR_UPDATE_TICKS = 60;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<EnemyProjectile> projectiles = new ArrayList<>();
    private final AttackDirector attacks = new AttackDirector();
    private final Context context;
    private final Player player;
    private final Tilemap tilemap;
    private final Random random = new Random();
    public interface KillListener { void onEnemyDefeated(String eventId); }
    private KillListener killListener;
    public void setKillListener(KillListener listener) { killListener = listener; }
    private void reportDefeat() {
        if (killListener != null) killListener.onEnemyDefeated("enemy.acid_ghost.defeated");
    }
    private int tick;
    private int spawnTicks;

    public EnemyManager(Context context, Player player, Tilemap tilemap) {
        this.context = context.getApplicationContext();
        this.player = player;
        this.tilemap = tilemap;
    }

    public void update() {
        if (tick >= Integer.MAX_VALUE - 1) tick = 0;
        tick++;
        attacks.beginTick(tick);
        // Keep spawn timing local to this play session, even when at capacity.
        if (++spawnTicks >= 180) {
            spawnTicks = 0;
            if (enemies.size() < MAX_ENEMIES) {
                double[] spawn = tilemap.randomSwampSpawn(player.getPositionX(), player.getPositionY(), random);
                if (spawn != null) enemies.add(new Enemy(context, player, EnemyConfig.acidGhost(),
                    spawn[0], spawn[1], EnemyPersonality.random(random)));
            }
        }

        // Social ghosts select nearby unfused ghosts of the same species.
        for (Enemy enemy : enemies) {
            Enemy target = null;
            if (enemy.getPersonality() == EnemyPersonality.SOCIAL && !enemy.isFused() && !enemy.isDead()) {
                double best = 320;
                for (Enemy other : enemies) {
                    if (enemy == other || other.isFused() || other.isDead()) continue;
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
            if (enemy.finishedDeath() || distance > DESPAWN_DISTANCE) {
                iterator.remove();
                continue;
            }
            int period = distance < ACTIVE_DISTANCE ? 1 :
                distance < MID_DISTANCE ? MID_UPDATE_TICKS : FAR_UPDATE_TICKS;
            if ((tick + enemy.getUpdateOffset()) % period == 0) enemy.update(period);
        }

        // Fuse nearby unfused ghosts once; the survivor keeps both remaining HP.
        for (int i = 0; i < enemies.size(); i++) {
            Enemy first = enemies.get(i);
            if (first.isFused() || first.isDead() || first.getPersonality() != EnemyPersonality.SOCIAL) continue;
            for (int j = i + 1; j < enemies.size(); j++) {
                Enemy second = enemies.get(j);
                if (second.isFused() || second.isDead()) continue;
                if (Math.hypot(first.getPositionX() - second.getPositionX(),
                        first.getPositionY() - second.getPositionY()) < 52) {
                    first.fuseWith(second);
                    enemies.remove(j);
                    break;
                }
            }
        }

        // Rotating priority prevents the same ghost from monopolizing the firing budget.
        int size = enemies.size();
        int start = attacks.firstCandidate(size);
        if (attacks.canFire(tick, projectiles.size())) {
            for (int offset = 0; offset < size; offset++) {
                int index = (start + offset) % size;
                Enemy enemy = enemies.get(index);
                if (enemy.isDead()) continue;
                EnemyProjectile shot = enemy.fireProjectile();
                if (shot != null) {
                    projectiles.add(shot);
                    attacks.recordShot(tick, index, size);
                    break;
                }
            }
        }

        Iterator<EnemyProjectile> shots = projectiles.iterator();
        while (shots.hasNext()) {
            EnemyProjectile shot = shots.next();
            shot.update();
            if (shot.expired()) shots.remove();
            else if (Circle.isColliding(shot, player)) {
                player.receiveHit(new DamageEvent(shot.getDamage(), shot.getDamage() * 12.0, DamageType.ACID));
                shots.remove();
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
                    if (enemy.isDead()) { reportDefeat(); break; }
                }
            }
        }
    }

    /** Area impacts used by the intrinsic coconut storm. */
    public void coconutImpact(double x, double y, double radius) {
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) continue;
            double distance = Math.hypot(enemy.getPositionX() - x, enemy.getPositionY() - y);
            if (distance < radius + enemy.getRadius()) {
                enemy.receiveHit(new DamageEvent(1, 18, DamageType.PHYSICAL));
                if (enemy.isDead()) reportDefeat();
            }
        }
    }

    public void draw(Canvas canvas, GameDisplay display) {
        for (Enemy enemy : enemies) enemy.draw(canvas, display);
        for (EnemyProjectile shot : projectiles) shot.draw(canvas, display);
    }

    public int getEnemyCount() { return enemies.size(); }
    public int getProjectileCount() { return projectiles.size(); }
}
