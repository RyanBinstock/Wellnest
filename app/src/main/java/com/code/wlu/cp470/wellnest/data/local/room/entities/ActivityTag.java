package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "activity_tag",
        indices = { @Index(value = {"name"}, unique = true) }
)
public class ActivityTag {
    @PrimaryKey(autoGenerate = true) public int id;
    public String name;
}
