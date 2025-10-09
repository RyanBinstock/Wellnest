package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "badge_local",
        indices = { @Index(value = {"code"}, unique = true) }
)
public class BadgeLocal {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String code;          // unique slug, e.g. "first_login"
    public String name;          // display name
    public String description;   // optional
    public String iconUrl;       // optional (or drawable name)
    public long earnedAt;        // epoch millis
}
