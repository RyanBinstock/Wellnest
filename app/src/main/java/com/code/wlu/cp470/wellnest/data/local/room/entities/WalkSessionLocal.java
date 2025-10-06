package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "walk_session_local",
        indices = { @Index(value = {"startedAt"}) }
)
public class WalkSessionLocal {
    @PrimaryKey(autoGenerate = true) public int id;

    public long startedAt;
    public long endedAt;
    public int steps;
    public int meters;
    public int points;             // score contribution
    public String routeGeoJson;    // optional serialized route
}
