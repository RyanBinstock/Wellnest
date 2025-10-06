package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "task_local",
        indices = { @Index(value = {"status"}), @Index(value = {"categoryId"}) }
)
public class TaskLocal {
    @PrimaryKey(autoGenerate = true) public int id;

    public String title;
    public String description;     // optional
    public int points;             // e.g., 10–50
    public String status;          // UNFINISHED | PENDING_VERIFY | FINISHED
    public int categoryId;         // for local categorization
    public int recurMask;          // bitmask for days (Sun=1, Mon=2, ...)
    public long createdAt;
    public long updatedAt;
}
