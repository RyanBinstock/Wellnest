package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "task_local",
        indices = {
                @Index(value = {"createdAt"}, name = "task_createdAt_idx"),
                @Index(value = {"status"}, name = "task_status_idx")
        }
)
public class TaskLocal {
    @PrimaryKey @NonNull public String id;  // UUID or cloud doc id
    public String title;
    // "FINISHED" | "UNFINISHED"
    public String status;
    public Integer pointsAwarded;  // nullable until verified
    public long createdAt;         // unix ms
}