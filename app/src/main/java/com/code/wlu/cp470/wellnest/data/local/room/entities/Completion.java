package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "completion",
        indices = { @Index(value = {"activityId"}) }
)
public class Completion {
    @PrimaryKey(autoGenerate = true) public int id;
    public String activityId;   // FK -> ActivityJar.id
    public long completedAt;    // epoch millis
    public String notes;        // optional
    public int points;          // optional
}
