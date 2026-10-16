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
    private static final int DEFAULT_SLOT = 1;
    private final GameDatabase database;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface LoadCallback {
        void onLoaded(PlayerPositionEntity position);
    }

    public GameSaveRepository(Context context) {
        database = Room.databaseBuilder(context.getApplicationContext(),
            GameDatabase.class, "amazon_mages_saves.db").build();
    }

    public void loadPosition(LoadCallback callback) {
        io.execute(() -> {
            PlayerPositionEntity position = null;
            try {
                position = database.playerPositionDao().load(DEFAULT_SLOT);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to load player position", e);
            }
            PlayerPositionEntity result = position;
            main.post(() -> callback.onLoaded(result));
        });
    }

    public void savePosition(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) return;
        PlayerPositionEntity snapshot =
            new PlayerPositionEntity(DEFAULT_SLOT, x, y, System.currentTimeMillis());
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
