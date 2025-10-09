package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "activity_category",
        indices = { @Index(value = {"name"}, unique = true) }
)
public class ActivityCategory {
    @PrimaryKey(autoGenerate = true) public int id;
    public String name;
    public String colorHex; // optional (e.g. "#7BC86C")
}
