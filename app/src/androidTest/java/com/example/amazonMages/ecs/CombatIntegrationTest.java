package com.example.amazonMages.ecs;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.amazonMages.ai.*;
import com.example.amazonMages.gameobject.Player;
import com.example.amazonMages.gamepanel.Joystick;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CombatIntegrationTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private Player player() { return new Player(context, new Joystick(0, 0, 70, 40), 100, 100, 32, null); }
    @Test public void hostileShotDamagesOnceAndExpires() {
        Player player = player();
        HostileProjectileWorld shots = new HostileProjectileWorld();
        shots.spawn(new EnemyProjectile(100, 100, 0, 0, 0, 1));
        shots.update(player);
        shots.update(player);
        assertEquals(4, player.getHealthPoint());
        assertEquals(0, shots.count());
        shots.spawn(new EnemyProjectile(2000, 2000, 0, 0, 0, 1));
        for (int i = 0; i < 180; i++) shots.update(player);
        assertEquals(0, shots.count());
        assertEquals(4, player.getHealthPoint());
    }
    @Test public void deathReportsOnceAndDebugCleanupDoesNotReward() {
        EnemyManager enemies = new EnemyManager(context, player(), null);
        int[] deaths = {0};
        enemies.setKillListener(event -> deaths[0]++);
        assertTrue(enemies.spawnNormalForDebug(100, 100));
        enemies.coconutImpact(100, 100, 100, 100);
        enemies.coconutImpact(100, 100, 100, 100);
        assertEquals(1, deaths[0]);
        assertTrue(enemies.spawnNormalForDebug(100, 100));
        enemies.clearForDebug();
        assertEquals(1, deaths[0]);
    }
}
