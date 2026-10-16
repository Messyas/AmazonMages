package com.example.amazonMages.ai;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
public class SpawnDirectorTest {
    @Test public void capacityTimerDistanceAndBossRulesAreIndependentOfEntities() {
        SpawnDirector spawns=new SpawnDirector(new SpawnDirector.Region(5,2,2,300,1400,EnemyTemplates.ACID_GHOST));
        Random random=new Random(1);
        assertNull(spawns.update(0,random)); assertSame(EnemyTemplates.ACID_GHOST,spawns.update(0,random));
        assertNull(spawns.update(2,random));assertNull(spawns.update(2,random));
        assertEquals(1,spawns.updatePeriod(799));assertEquals(6,spawns.updatePeriod(800));assertEquals(60,spawns.updatePeriod(1250));
        assertTrue(spawns.shouldDespawn(1901,false));assertFalse(spawns.shouldDespawn(5000,true));
    }
    @Test(expected=IllegalArgumentException.class) public void bossesCannotSpawnAsAmbientPopulation() {
        new SpawnDirector.Region(5,2,2,300,1400,EnemyTemplates.ANCIENT_WRAITH);
    }
}
