package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "pending_verify",
        indices = { @Index(value = {"taskId"}) }
)
public class PendingVerify {
    @PrimaryKey(autoGenerate = true) public int id;

    public int taskId;          // FK -> TaskLocal.id
    public String photoPath;    // app-private file path
    public long submittedAt;    // when user submitted
}
