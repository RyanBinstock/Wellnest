package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(
        tableName = "activity_tag_xref",
        primaryKeys = {"activityId","tagId"},
        indices = { @Index(value = {"tagId"}) }
)
public class ActivityTagXref {
    @NonNull public String activityId;  // FK -> ActivityJar.id
    public int tagId;                   // FK -> ActivityTag.id
}
