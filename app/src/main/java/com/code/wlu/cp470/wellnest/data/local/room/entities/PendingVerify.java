package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "pending_verify",
        indices = { @Index(value = {"lastAttemptAt"}, name = "pending_retry_idx") }
)
public class PendingVerify {
    @PrimaryKey @NonNull public String taskId; // FK (logical) → TaskLocal.id
    public String beforePath;
    public String afterPath;
    public int retries;
    public long lastAttemptAt;                 // unix ms
}
