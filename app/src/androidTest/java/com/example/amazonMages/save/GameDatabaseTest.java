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
        GameSaveRepository.MIGRATION_3_4, GameSaveRepository.MIGRATION_4_5,
        GameSaveRepository.MIGRATION_5_6
    };

    @Test public void everyHistoricalVersionMigratesAndPreservesSlots() {
        for (int version = 1; version <= 5; version++) {
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
            GameDatabase db = Room.databaseBuilder(context, GameDatabase.class, name)
                    .addMigrations(MIGRATIONS).build();
            try {
                PlayerPositionEntity position = db.playerPositionDao().load(1);
                assertEquals(100, position.x, 0);
                assertEquals(5, position.health);
                assertTrue(db.inventoryItemDao().loadForSlot(1).isEmpty());
                if (version >= 3) assertEquals(2, db.questProgressDao().loadForSlot(1).get(0).progress);
                if (version >= 4) assertEquals(2, db.playerStatsDao().load(1).level);
                assertRoundTripAndDeletion(db);
            } finally { db.close(); context.deleteDatabase(name); }
        }
    }

    @Test public void freshV5WithoutHealthDefaultAlsoMigrates() {
        String name = "fresh-v5-test";
        context.deleteDatabase(name);
        // Fresh v2-v5 schemas had no DEFAULT, unlike saves migrated from v1.
        try (android.database.sqlite.SQLiteDatabase legacy = context.openOrCreateDatabase(name, 0, null)) {
            legacy.execSQL("CREATE TABLE player_positions (slotId INTEGER NOT NULL PRIMARY KEY, x REAL NOT NULL, y REAL NOT NULL, updatedAt INTEGER NOT NULL, health INTEGER NOT NULL)");
            legacy.execSQL("INSERT INTO player_positions VALUES (1, 12, 34, 99, 3)");
            legacy.execSQL("CREATE TABLE quest_progress (slotId INTEGER NOT NULL, questId TEXT NOT NULL, state TEXT NOT NULL, progress INTEGER NOT NULL, PRIMARY KEY(slotId, questId))");
            legacy.execSQL("CREATE TABLE player_stats (slotId INTEGER NOT NULL, level INTEGER NOT NULL, experience INTEGER NOT NULL, attributePoints INTEGER NOT NULL, skillPoints INTEGER NOT NULL, vigor INTEGER NOT NULL, mente INTEGER NOT NULL, resistencia INTEGER NOT NULL, inteligencia INTEGER NOT NULL, destreza INTEGER NOT NULL, arcano INTEGER NOT NULL, forca INTEGER NOT NULL, mana REAL NOT NULL, maxMana REAL NOT NULL, PRIMARY KEY(slotId))");
            legacy.execSQL("CREATE TABLE inventory_items (slotId INTEGER NOT NULL, itemId TEXT NOT NULL, quantity INTEGER NOT NULL, PRIMARY KEY(slotId, itemId))");
            legacy.execSQL("INSERT INTO inventory_items VALUES (1, 'erva', 7)");
            legacy.setVersion(5);
        }
        GameDatabase db = Room.databaseBuilder(context, GameDatabase.class, name)
                .addMigrations(MIGRATIONS).build();
        try {
            assertEquals(3, db.playerPositionDao().load(1).health);
            assertEquals(7, db.inventoryItemDao().loadForSlot(1).get(0).quantity);
        } finally { db.close(); context.deleteDatabase(name); }
    }

    @Test public void freshDatabaseMatchesMigratedSchemaAndTransactions() {
        GameDatabase db = Room.inMemoryDatabaseBuilder(context, GameDatabase.class).build();
        try {
            assertRoundTripAndDeletion(db);
        } finally { db.close(); }
    }

    @Test public void failedInventoryWriteRollsBackEntireSnapshot() {
        GameDatabase db = Room.inMemoryDatabaseBuilder(context, GameDatabase.class).build();
        try {
            db.playerPositionDao().save(new PlayerPositionEntity(1, 10, 20, 5, 1));
            try {
                GameSaveRepository.writeSnapshot(db,
                    new PlayerPositionEntity(1, 99, 99, 1, 2),
                    new PlayerStatsEntity(1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 10, 100),
                    Collections.singletonList(new QuestProgressEntity(1, "quest", "ACTIVE", 1)),
                    Collections.singletonList(new InventoryItemEntity(1, null, 1)));
                fail("Expected invalid primary key to abort save");
            } catch (android.database.sqlite.SQLiteConstraintException expected) {
                assertEquals(5, db.playerPositionDao().load(1).health);
                assertEquals(10, db.playerPositionDao().load(1).x, 0);
                assertNull(db.playerStatsDao().load(1));
                assertTrue(db.questProgressDao().loadForSlot(1).isEmpty());
            }
        } finally { db.close(); }
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
            // Refill both slots so deletion proves removal and isolation of every table.
            GameSaveRepository.writeSnapshot(db, position, stats,
                Collections.singletonList(new QuestProgressEntity(slot, "first_ghosts", "ACTIVE", 2)),
                Collections.singletonList(new InventoryItemEntity(slot, "erva", 4)));
        }
        GameSaveRepository.deleteSlotData(db, 1);
        assertNull(db.playerPositionDao().load(1));
        assertNull(db.playerStatsDao().load(1));
        assertTrue(db.questProgressDao().loadForSlot(1).isEmpty());
        assertTrue(db.inventoryItemDao().loadForSlot(1).isEmpty());
        assertNotNull(db.playerPositionDao().load(2));
        assertNotNull(db.playerStatsDao().load(2));
        assertEquals(1, db.questProgressDao().loadForSlot(2).size());
        assertEquals(4, db.inventoryItemDao().loadForSlot(2).get(0).quantity);
    }
}
