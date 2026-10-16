package com.example.amazonMages.save;

import android.content.Context;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Collections;
import static org.junit.Assert.*;

/** Opens actual historical schemas through Room, including its schema validation. */
@RunWith(AndroidJUnit4.class)
public class GameDatabaseTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final Migration[] MIGRATIONS = {
        GameSaveRepository.MIGRATION_1_2, GameSaveRepository.MIGRATION_2_3,
        GameSaveRepository.MIGRATION_3_4, GameSaveRepository.MIGRATION_4_5
    };

    @Test public void everyHistoricalVersionMigratesAndPreservesSlots() {
        for (int version = 1; version <= 4; version++) {
            String name = "migration-test-" + version;
            context.deleteDatabase(name);
            final int sourceVersion = version;
            SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                    .callback(new SupportSQLiteOpenHelper.Callback(version) {
                        @Override public void onCreate(SupportSQLiteDatabase db) {
                            db.execSQL("CREATE TABLE player_positions (slotId INTEGER NOT NULL PRIMARY KEY, x REAL NOT NULL, y REAL NOT NULL, updatedAt INTEGER NOT NULL)");
                            db.execSQL("INSERT INTO player_positions VALUES (1, 100, 200, 1234)");
                            for (int i = 0; i < sourceVersion - 1; i++) MIGRATIONS[i].migrate(db);
                            if (sourceVersion >= 3)
                                db.execSQL("INSERT INTO quest_progress VALUES (1, 'first_ghosts', 'ACTIVE', 2)");
                            if (sourceVersion >= 4)
                                db.execSQL("INSERT INTO player_stats VALUES (1, 2, 15, 1, 1, 1, 1, 1, 1, 1, 1, 1, 20, 100)");
                        }
                        @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                            throw new AssertionError("Unexpected upgrade in fixture");
                        }
                    }).build());
            helper.getWritableDatabase();
            helper.close();
            try (GameDatabase db = Room.databaseBuilder(context, GameDatabase.class, name)
                    .addMigrations(MIGRATIONS).build()) {
                PlayerPositionEntity position = db.playerPositionDao().load(1);
                assertEquals(100, position.x, 0);
                assertEquals(5, position.health);
                assertTrue(db.inventoryItemDao().loadForSlot(1).isEmpty());
                if (version >= 3) assertEquals(2, db.questProgressDao().loadForSlot(1).get(0).progress);
                if (version >= 4) assertEquals(2, db.playerStatsDao().load(1).level);
                assertRoundTripAndDeletion(db);
            } finally { context.deleteDatabase(name); }
        }
    }

    @Test public void freshDatabaseMatchesMigratedSchemaAndTransactions() {
        try (GameDatabase db = Room.inMemoryDatabaseBuilder(context, GameDatabase.class).build()) {
            assertRoundTripAndDeletion(db);
        }
    }

    private void assertRoundTripAndDeletion(GameDatabase db) {
        for (int slot = 1; slot <= 2; slot++) {
            PlayerPositionEntity position = new PlayerPositionEntity(slot, 101, 202, 3, 99);
            PlayerStatsEntity stats = new PlayerStatsEntity(slot, 2, 10, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 30, 100);
            GameSaveRepository.writeSnapshot(db, position, stats,
                Collections.singletonList(new QuestProgressEntity(slot, "first_ghosts", "ACTIVE", 2)),
                Collections.singletonList(new InventoryItemEntity(slot, "erva", 4)));
            assertEquals(3, db.playerPositionDao().load(slot).health);
            assertEquals(30, db.playerStatsDao().load(slot).mana, 0);
            assertEquals(2, db.questProgressDao().loadForSlot(slot).get(0).progress);
            assertEquals(4, db.inventoryItemDao().loadForSlot(slot).get(0).quantity);
            GameSaveRepository.writeSnapshot(db, position, stats, Collections.emptyList(), Collections.emptyList());
            assertTrue(db.inventoryItemDao().loadForSlot(slot).isEmpty());
            assertTrue(db.questProgressDao().loadForSlot(slot).isEmpty());
        }
        GameSaveRepository.deleteSlotData(db, 1);
        assertNull(db.playerPositionDao().load(1));
        assertNull(db.playerStatsDao().load(1));
        assertTrue(db.questProgressDao().loadForSlot(1).isEmpty());
        assertTrue(db.inventoryItemDao().loadForSlot(1).isEmpty());
        assertNotNull(db.playerPositionDao().load(2));
        assertNotNull(db.playerStatsDao().load(2));
    }
}
