package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "friend_local",
        indices = { @Index(value = {"friendUid"}, unique = true) }
)
public class FriendLocal {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String friendUid;

    public String displayName;     // nullable ok
    public String avatarUrl;       // nullable ok
    public long sinceEpochMs;      // when added
}
