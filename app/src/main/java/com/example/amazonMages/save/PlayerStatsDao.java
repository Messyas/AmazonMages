package com.example.amazonMages.save;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface PlayerStatsDao {
    @Query("SELECT * FROM player_stats WHERE slotId = :slotId LIMIT 1")
    PlayerStatsEntity load(int slotId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(PlayerStatsEntity stats);

    @Query("DELETE FROM player_stats WHERE slotId = :slotId")
    void deleteForSlot(int slotId);
}
