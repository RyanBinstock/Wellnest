package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "completion",
        indices = {
                @Index(value = {"completedAt"}, name = "completion_time_idx"),
                @Index(value = {"activityId"}, name = "completion_activity_idx"),
                @Index(value = {"categoryIdAtComplete"}, name = "completion_category_snapshot_idx")
        }
)
public class Completion {
    @PrimaryKey @NonNull public String id;     // UUID string
    @NonNull
    public String activityId;         // FK → ActivityJar.id (TEXT)
    public long completedAt;                   // unix ms
    public int categoryIdAtComplete;           // snapshot of category at completion
    public String notes;                       // nullable
}
