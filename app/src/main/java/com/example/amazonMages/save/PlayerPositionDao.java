package com.example.amazonMages.save;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface PlayerPositionDao {
    @Query("SELECT * FROM player_positions WHERE slotId = :slotId LIMIT 1")
    PlayerPositionEntity load(int slotId);

    @Query("SELECT * FROM player_positions ORDER BY updatedAt DESC")
    java.util.List<PlayerPositionEntity> loadAll();

    @Query("DELETE FROM player_positions WHERE slotId = :slotId")
    void delete(int slotId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(PlayerPositionEntity position);
}
