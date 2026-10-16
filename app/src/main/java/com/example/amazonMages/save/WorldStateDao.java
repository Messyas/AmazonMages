package com.example.amazonMages.save;
import androidx.room.*;
import java.util.List;
@Dao
public interface WorldStateDao {
    @Query("SELECT * FROM equipment WHERE slotId=:slot ORDER BY weaponSlot") List<EquipmentEntity> equipment(int slot);
    @Query("SELECT * FROM world_pickups WHERE slotId=:slot ORDER BY pickupId LIMIT 64") List<WorldPickupEntity> pickups(int slot);
    @Insert(onConflict=OnConflictStrategy.REPLACE) void saveEquipment(List<EquipmentEntity> rows);
    @Insert(onConflict=OnConflictStrategy.REPLACE) void savePickups(List<WorldPickupEntity> rows);
    @Query("DELETE FROM equipment WHERE slotId=:slot") void deleteEquipment(int slot);
    @Query("DELETE FROM world_pickups WHERE slotId=:slot") void deletePickups(int slot);
}
