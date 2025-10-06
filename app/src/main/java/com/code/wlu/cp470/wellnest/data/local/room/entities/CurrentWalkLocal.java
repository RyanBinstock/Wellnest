package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "current_walk_local")
public class CurrentWalkLocal {
    @PrimaryKey public int id = 1; // singleton row
    public long startedAt;
    public long lastUpdatedAt;
    public int steps;
    public int meters;
    public int isActive;           // 0/1
}
