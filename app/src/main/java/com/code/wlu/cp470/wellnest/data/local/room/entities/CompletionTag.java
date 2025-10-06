package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "completion_tag",
        indices = { @Index(value = {"completionId"}), @Index(value = {"tagId"}) }
)
public class CompletionTag {
    @PrimaryKey(autoGenerate = true) public int id;
    public int completionId;   // FK -> Completion.id
    public int tagId;          // FK -> ActivityTag.id
}
