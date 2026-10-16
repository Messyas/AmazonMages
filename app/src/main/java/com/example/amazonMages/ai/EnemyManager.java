package com.example.amazonMages.ai;

import android.content.Context;
import android.graphics.Canvas;
import com.example.amazonMages.GameDisplay;
import com.example.amazonMages.gameobject.Circle;
import com.example.amazonMages.gameobject.Enemy;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gameobject.Spell;
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
    private int tick;
    private int spawnTicks;

    public EnemyManager(Context context, Player player) {
        this.context = context.getApplicationContext();
        this.player = player;
    }

    public void update() {
        if (tick >= Integer.MAX_VALUE - 1) tick = 0;
        tick++;
        attacks.beginTick(tick);
        // Keep spawn timing local to this play session, even when at capacity.
        if (++spawnTicks >= 180) {
            spawnTicks = 0;
            if (enemies.size() < MAX_ENEMIES) enemies.add(new Enemy(context, player));
        }

        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            double distance = Math.hypot(
                enemy.getPositionX() - player.getPositionX(),
                enemy.getPositionY() - player.getPositionY());
            if (distance > DESPAWN_DISTANCE) {
                iterator.remove();
                continue;
            }
            int period = distance < ACTIVE_DISTANCE ? 1 :
                distance < MID_DISTANCE ? MID_UPDATE_TICKS : FAR_UPDATE_TICKS;
            if ((tick + enemy.getUpdateOffset()) % period == 0) enemy.update(period);
        }

        // Rotating priority prevents the same ghost from monopolizing the firing budget.
        int size = enemies.size();
        int start = attacks.firstCandidate(size);
        if (attacks.canFire(tick, projectiles.size())) {
            for (int offset = 0; offset < size; offset++) {
                int index = (start + offset) % size;
                Enemy enemy = enemies.get(index);
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
                player.setHealthPoint(player.getHealthPoint() - 1);
                shots.remove();
            }
        }
    }

    public void collideWithPlayerSpells(List<Spell> spells) {
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            Iterator<Spell> shotIterator = spells.iterator();
            while (shotIterator.hasNext()) {
                if (Circle.isColliding(shotIterator.next(), enemy)) {
                    shotIterator.remove();
                    enemy.takeDamage(1);
                    if (enemy.isDead()) {
                        enemyIterator.remove();
                        break;
                    }
                }
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
