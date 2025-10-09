package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activity_jar")
public class ActivityJar {
    @PrimaryKey @NonNull public String id;   // e.g., "aj_2025_0001"
    public String title;
    public String summary;                   // nullable
    public int categoryId;                   // FK -> ActivityCategory.id
    public String source;                    // STOCK | GENERATED | USER
    public long createdAt;
    public long updatedAt;
    public int isArchived;                   // 0/1
}
