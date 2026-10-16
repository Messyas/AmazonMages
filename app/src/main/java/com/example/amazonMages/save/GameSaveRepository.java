package com.example.amazonMages.save;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.room.Room;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Single-threaded persistence: loading and saves execute in submission order. */
public class GameSaveRepository {
    private static final String TAG = "GameSaveRepository";
    private final int slotId;
    private final GameDatabase database;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface LoadCallback {
        void onLoaded(PlayerPositionEntity position);
    }

    public GameSaveRepository(Context context, int slotId) {
        this.slotId = slotId;
        database = Room.databaseBuilder(context.getApplicationContext(),
            GameDatabase.class, "amazon_mages_saves.db")
            .addMigrations(MIGRATION_1_2).build();
    }

    public static final androidx.room.migration.Migration MIGRATION_1_2 =
        new androidx.room.migration.Migration(1, 2) {
            @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL("ALTER TABLE player_positions ADD COLUMN health INTEGER NOT NULL DEFAULT 5");
            }
        };

    public interface SlotsCallback {
        void onLoaded(java.util.List<PlayerPositionEntity> slots);
    }

    public void loadSlots(SlotsCallback callback) {
        io.execute(() -> {
            java.util.List<PlayerPositionEntity> slots;
            try {
                slots = database.playerPositionDao().loadAll();
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to load slots", e);
                slots = new java.util.ArrayList<>();
            }
            java.util.List<PlayerPositionEntity> result = slots;
            main.post(() -> callback.onLoaded(result));
        });
    }

    public void deleteSlot(int id, Runnable done) {
        io.execute(() -> {
            try {
                database.playerPositionDao().delete(id);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to delete slot", e);
            }
            main.post(done);
        });
    }

    public void loadPosition(LoadCallback callback) {
        io.execute(() -> {
            PlayerPositionEntity position = null;
            try {
                position = database.playerPositionDao().load(slotId);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to load player position", e);
            }
            PlayerPositionEntity result = position;
            main.post(() -> callback.onLoaded(result));
        });
    }

    public void savePosition(double x, double y, int health) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) return;
        PlayerPositionEntity snapshot =
            new PlayerPositionEntity(slotId, x, y, health, System.currentTimeMillis());
        io.execute(() -> {
            try {
                database.playerPositionDao().save(snapshot);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to save player position", e);
            }
        });
    }

    /** Flushes queued saves before closing the database; does not block the UI thread. */
    public void close() {
        io.execute(database::close);
        io.shutdown();
    }
}
