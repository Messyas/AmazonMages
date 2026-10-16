package com.example.amazonMages.save;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "player_positions")
public class PlayerPositionEntity {
    @PrimaryKey
    public int slotId;
    public double x;
    public double y;
    public long updatedAt;

    public PlayerPositionEntity(int slotId, double x, double y, long updatedAt) {
        this.slotId = slotId;
        this.x = x;
        this.y = y;
        this.updatedAt = updatedAt;
    }
}
