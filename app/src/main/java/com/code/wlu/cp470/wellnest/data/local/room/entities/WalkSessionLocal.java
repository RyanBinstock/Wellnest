package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "walk_session_local",
        indices = { @Index(value = {"endedAt"}, name = "walk_hist_time_idx") }
)
public class WalkSessionLocal {
    @PrimaryKey @NonNull
    public String id;  // UUID or server id
    public long startedAt;
    public Long endedAt;           // nullable
    public int steps;
    public double distanceMeters;
    public Integer pointsAward;    // nullable
    public String status;          // COMPLETED | CANCELLED
}