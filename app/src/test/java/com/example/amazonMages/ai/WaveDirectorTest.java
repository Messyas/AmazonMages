package com.example.amazonMages.ai;

import com.example.amazonMages.map.WorldDefinition;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaveDirectorTest {
    @Test public void scalesCapSurgeAndBossEncountersOverTime() {
        WaveDirector director = new WaveDirector();
        assertEquals(1, director.waveNumber());
        int initialCap = director.targetCap();
        assertFalse(director.isSurgeActive());

        // Step past the first 45s surge trigger (2700 ticks).
        int spawnsSeen = 0;
        for (int t = 0; t < 2705; t++) {
            spawnsSeen += director.step(0);
        }
        assertTrue(spawnsSeen > 50);
        assertTrue(director.isSurgeActive());
        assertEquals(2, director.waveNumber());
        assertTrue(director.targetCap() > initialCap);
        assertNotNull(director.pollAnnouncement());
        assertNull(director.pollAnnouncement());

        // Advance to 90s (5400 ticks) and 180s (10800 ticks) for timed mini-boss & boss.
        while (director.survivalTicks() < 5400) director.step(0);
        assertSame(EnemyTemplates.SWAMP_SENTINEL, director.checkTimedBoss());
        assertNull(director.checkTimedBoss());

        while (director.survivalTicks() < 10800) director.step(0);
        assertSame(EnemyTemplates.ANCIENT_WRAITH, director.checkTimedBoss());
        assertEquals(WaveDirector.MAX_CAP, director.targetCap());
    }

    @Test public void ambientPicksDiversifyAcrossProgressionAndBiomes() {
        WaveDirector director = new WaveDirector();
        Random rng = new Random(42);
        Set<String> early = new HashSet<>();
        for (int i = 0; i < 40; i++) early.add(director.pickAmbient(WorldDefinition.Region.CENTRAL, rng).id);
        assertTrue(early.contains("swarm_bat"));
        assertTrue(early.contains("acid_ghost"));

        // Advance past 60s to unlock Curupira Imps, Vine Shamans, and Moss Golems.
        for (int t = 0; t < 3700; t++) director.step(0);
        Set<String> late = new HashSet<>();
        for (int i = 0; i < 80; i++) {
            late.add(director.pickAmbient(WorldDefinition.Region.ANCIENT, rng).id);
            late.add(director.pickAmbient(WorldDefinition.Region.INDIGENOUS, rng).id);
        }
        assertTrue(late.contains("curupira_imp"));
        assertTrue(late.contains("vine_shaman"));
        assertTrue(late.contains("moss_golem"));
    }
}

