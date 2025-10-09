package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "current_walk_local",
        indices = { @Index(value = {"lastUpdatedMs"}, name = "current_walk_updated_idx") }
)
public class CurrentWalkLocal {
    @PrimaryKey @NonNull public String id;     // current session id
    public String status;                      // ACTIVE | PAUSED
    public long startedAt;
    public int startStepCount;
    public long startElapsedRealtimeMs;
    public long lastUpdatedMs;
    public Integer lastKnownSteps;             // nullable
    public Double lastKnownDistanceMeters;     // nullable
}
