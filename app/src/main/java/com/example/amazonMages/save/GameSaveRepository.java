package com.example.amazonMages.save;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.room.Room;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Single-threaded persistence: loading and saves execute in submission order. */
public class GameSaveRepository {
    private static final String TAG = "GameSaveRepository";
    private final int slotId;
    private volatile boolean closed;
    private final GameDatabase database;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public int getSlotId() { return slotId; }

    public interface LoadCallback {
        void onLoaded(PlayerPositionEntity position);
    }

    public GameSaveRepository(Context context, int slotId) {
        this.slotId = slotId;
        database = Room.databaseBuilder(context.getApplicationContext(),
            GameDatabase.class, "amazon_mages_saves.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build();
    }

    public static final androidx.room.migration.Migration MIGRATION_1_2 =
        new androidx.room.migration.Migration(1, 2) {
            @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL("ALTER TABLE player_positions ADD COLUMN health INTEGER NOT NULL DEFAULT 5");
            }
        };

    public static final androidx.room.migration.Migration MIGRATION_2_3 =
        new androidx.room.migration.Migration(2, 3) {
            @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS quest_progress (slotId INTEGER NOT NULL, questId TEXT NOT NULL, state TEXT NOT NULL, progress INTEGER NOT NULL, PRIMARY KEY(slotId, questId))");
            }
        };

    public static final androidx.room.migration.Migration MIGRATION_3_4 =
        new androidx.room.migration.Migration(3, 4) {
            @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS player_stats (slotId INTEGER NOT NULL, level INTEGER NOT NULL, experience INTEGER NOT NULL, attributePoints INTEGER NOT NULL, skillPoints INTEGER NOT NULL, vigor INTEGER NOT NULL, mente INTEGER NOT NULL, resistencia INTEGER NOT NULL, inteligencia INTEGER NOT NULL, destreza INTEGER NOT NULL, arcano INTEGER NOT NULL, forca INTEGER NOT NULL, mana REAL NOT NULL, maxMana REAL NOT NULL, PRIMARY KEY(slotId))");
            }
        };

    public static final androidx.room.migration.Migration MIGRATION_4_5 =
        new androidx.room.migration.Migration(4, 5) {
            @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS inventory_items (slotId INTEGER NOT NULL, itemId TEXT NOT NULL, quantity INTEGER NOT NULL, PRIMARY KEY(slotId, itemId))");
            }
        };

    public interface InventoryLoadCallback {
        void onLoaded(List<InventoryItemEntity> items);
    }

    public void loadInventory(InventoryLoadCallback callback) {
        submit(() -> {
            List<InventoryItemEntity> rows;
            try { rows = database.inventoryItemDao().loadForSlot(slotId); }
            catch (RuntimeException e) {
                Log.e(TAG, "Unable to load inventory", e);
                rows = new ArrayList<>();
            }
            List<InventoryItemEntity> result = rows;
            postIfOpen(() -> callback.onLoaded(result));
        });
    }

    public void saveInventory(List<InventoryItemEntity> snapshot) {
        final List<InventoryItemEntity> rows = new ArrayList<>(snapshot);
        submit(() -> {
            try {
                database.runInTransaction(() -> {
                    database.inventoryItemDao().deleteForSlot(slotId);
                    database.inventoryItemDao().upsertAll(rows);
                });
            } catch (RuntimeException e) { Log.e(TAG, "Unable to save inventory", e); }
        });
    }

    public interface StatsLoadCallback { void onLoaded(PlayerStatsEntity stats); }

    public void loadStats(StatsLoadCallback callback) {
        submit(() -> {
            PlayerStatsEntity row = null;
            try { row = database.playerStatsDao().load(slotId); }
            catch (RuntimeException e) { Log.e(TAG, "Unable to load stats", e); }
            PlayerStatsEntity result = row;
            postIfOpen(() -> callback.onLoaded(result));
        });
    }

    public void saveStats(PlayerStatsEntity stats) {
        submit(() -> {
            try { database.playerStatsDao().save(stats); }
            catch (RuntimeException e) { Log.e(TAG, "Unable to save stats", e); }
        });
    }

    public interface QuestLoadCallback {
        void onLoaded(List<QuestProgressEntity> progress);
    }

    public void loadQuests(QuestLoadCallback callback) {
        submit(() -> {
            List<QuestProgressEntity> rows;
            try {
                rows = database.questProgressDao().loadForSlot(slotId);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to load quests", e);
                rows = new ArrayList<>();
            }
            List<QuestProgressEntity> result = rows;
            postIfOpen(() -> callback.onLoaded(result));
        });
    }

    public void saveQuests(List<QuestProgressEntity> snapshot) {
        final List<QuestProgressEntity> rows = new ArrayList<>(snapshot);
        submit(() -> {
            try {
                database.questProgressDao().upsertAll(rows);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to save quests", e);
            }
        });
    }

    public interface SlotsCallback {
        void onLoaded(java.util.List<PlayerPositionEntity> slots);
    }

    public void loadSlots(SlotsCallback callback) {
        submit(() -> {
            java.util.List<PlayerPositionEntity> slots;
            try {
                slots = database.playerPositionDao().loadAll();
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to load slots", e);
                slots = new java.util.ArrayList<>();
            }
            java.util.List<PlayerPositionEntity> result = slots;
            postIfOpen(() -> callback.onLoaded(result));
        });
    }

    public void deleteSlot(int id, Runnable done) {
        submit(() -> {
            try {
                deleteSlotData(database, id);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to delete slot", e);
            }
            postIfOpen(done);
        });
    }

    public void loadPosition(LoadCallback callback) {
        submit(() -> {
            PlayerPositionEntity position = null;
            try {
                position = database.playerPositionDao().load(slotId);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to load player position", e);
            }
            PlayerPositionEntity result = position;
            postIfOpen(() -> callback.onLoaded(result));
        });
    }

    public void savePosition(double x, double y, int health) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) return;
        PlayerPositionEntity snapshot =
            new PlayerPositionEntity(slotId, x, y, health, System.currentTimeMillis());
        submit(() -> {
            try {
                database.playerPositionDao().save(snapshot);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to save player position", e);
            }
        });
    }

    /** All mutable arguments are snapshots owned by this queued operation. */
    public void saveSnapshot(PlayerPositionEntity position, PlayerStatsEntity stats,
            List<QuestProgressEntity> quests, List<InventoryItemEntity> inventory) {
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y)) return;
        submit(() -> {
            try { writeSnapshot(database, position, stats, quests, inventory); }
            catch (RuntimeException e) { Log.e(TAG, "Unable to save snapshot", e); }
        });
    }

    static void writeSnapshot(GameDatabase db, PlayerPositionEntity position,
            PlayerStatsEntity stats, List<QuestProgressEntity> quests,
            List<InventoryItemEntity> inventory) {
        db.runInTransaction(() -> {
            db.playerPositionDao().save(position);
            db.playerStatsDao().save(stats);
            db.questProgressDao().deleteForSlot(position.slotId);
            db.questProgressDao().upsertAll(quests);
            db.inventoryItemDao().deleteForSlot(position.slotId);
            db.inventoryItemDao().upsertAll(inventory);
        });
    }

    static void deleteSlotData(GameDatabase db, int id) {
        db.runInTransaction(() -> {
            db.questProgressDao().deleteForSlot(id);
            db.playerStatsDao().deleteForSlot(id);
            db.inventoryItemDao().deleteForSlot(id);
            db.playerPositionDao().delete(id);
        });
    }

    /** Reject late work and callbacks once this session has been disposed. */
    private synchronized void submit(Runnable operation) {
        if (!closed) io.execute(operation);
    }

    private void postIfOpen(Runnable callback) {
        main.post(() -> { if (!closed) callback.run(); });
    }

    /** Flushes queued saves before closing the database; does not block the UI thread. */
    public synchronized void close() {
        if (closed) return;
        closed = true;
        io.execute(database::close);
        io.shutdown();
    }
}
