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
    public int health;

    public PlayerPositionEntity(int slotId, double x, double y, int health, long updatedAt) {
        this.slotId = slotId;
        this.x = x;
        this.y = y;
        this.updatedAt = updatedAt;
        this.health = health;
    }
}
