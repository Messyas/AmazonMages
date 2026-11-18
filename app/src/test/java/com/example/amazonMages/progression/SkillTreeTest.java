package com.example.amazonMages.progression;

import com.example.amazonMages.combat.WeaponLoadout;
import com.example.amazonMages.items.ItemCatalog;
import com.example.amazonMages.save.QuestProgressEntity;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkillTreeTest {
    @Test public void enforcesPrerequisitesMaxRanksAndPointSpending() {
        SkillTree tree = new SkillTree();
        PlayerProgression stats = new PlayerProgression();
        stats.skillPoints = 5;

        assertEquals(9, tree.nodes().size());
        assertFalse(tree.canUnlock("cocos_explosivos", stats));
        assertFalse(tree.canUnlock("furia_castanheira", stats));

        assertTrue(tree.unlock("golpe_triplo", stats));
        assertEquals(1, tree.rank("golpe_triplo"));
        assertEquals(4, stats.skillPoints);
        assertEquals(22, tree.chargeTicksRequired());

        assertTrue(tree.unlock("cocos_explosivos", stats));
        assertTrue(tree.unlock("cocos_explosivos", stats));
        assertFalse(tree.unlock("cocos_explosivos", stats)); // maxRank 2
        assertEquals(112.0, tree.explosiveCoconutRadius(), 0.01);

        assertTrue(tree.unlock("furia_castanheira", stats));
        assertEquals(2, tree.chargedBonusProjectiles());
        assertEquals(1.40, tree.specialDurationMultiplier(), 0.01);
        assertEquals(1, stats.skillPoints);
    }

    @Test public void respecRefundsAllSpentPointsAndResetsModifiers() {
        SkillTree tree = new SkillTree();
        PlayerProgression stats = new PlayerProgression();
        stats.skillPoints = 6;

        assertTrue(tree.unlock("guarda_viva", stats));
        assertTrue(tree.unlock("guarda_viva", stats));
        assertTrue(tree.unlock("passo_curupira", stats));
        assertTrue(tree.unlock("manto_castanheira", stats));
        assertEquals(2, stats.skillPoints);
        assertEquals(4, tree.totalSpentPoints());
        assertEquals(0.88, tree.guardReduction(), 0.01);
        assertEquals(50.0, tree.bonusPosture(), 0.01);
        assertEquals(76, tree.dashCooldownTicks());
        assertTrue(tree.hasLivingAegis());

        int refunded = tree.respec(stats);
        assertEquals(4, refunded);
        assertEquals(6, stats.skillPoints);
        assertEquals(0, tree.totalSpentPoints());
        assertEquals(0.70, tree.guardReduction(), 0.01);
        assertFalse(tree.hasLivingAegis());
    }

    @Test public void snapshotAndRestorePersistRanksWithClamping() {
        SkillTree source = new SkillTree();
        PlayerProgression stats = new PlayerProgression();
        stats.skillPoints = 4;
        source.unlock("raizes_fartas", stats);
        source.unlock("raizes_fartas", stats);
        source.unlock("alquimia_viva", stats);
        source.unlock("sinfonia_flora", stats);

        List<QuestProgressEntity> rows = new ArrayList<>();
        rows.add(new QuestProgressEntity(1, "other_quest", "ACTIVE", 2));
        source.appendSnapshot(1, rows);
        rows.add(new QuestProgressEntity(1, "skill.alquimia_viva", "UNLOCKED", 99)); // clamp test
        rows.add(new QuestProgressEntity(1, "skill.unknown_node", "UNLOCKED", 3));

        SkillTree restored = new SkillTree();
        restored.restore(rows);
        assertEquals(2, restored.rank("raizes_fartas"));
        assertEquals(2, restored.rank("alquimia_viva"));
        assertEquals(1, restored.rank("sinfonia_flora"));
        assertEquals(16, restored.coconutReserveCap());
        assertEquals(1.8, restored.alchemyMultiplier(), 0.01);
        assertTrue(restored.hasFloraSymphony());
    }

    @Test public void castEquippedSpecialWorksForBothCoconutAndStaff() {
        WeaponLoadout loadout = new WeaponLoadout();
        assertTrue(loadout.castEquippedSpecial());
        assertFalse(loadout.castEquippedSpecial());
        loadout.reduceSpecialCooldown(WeaponLoadout.SPECIAL_COOLDOWN_TICKS);
        loadout.equip(1, ItemCatalog.STAFF);
        loadout.select(1);
        assertFalse(loadout.special()); // strict zero-arg COCONUT contract preserved
        assertTrue(loadout.castEquippedSpecial());
    }
}

