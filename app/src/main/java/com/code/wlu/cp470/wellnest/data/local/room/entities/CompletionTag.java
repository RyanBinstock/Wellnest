package com.code.wlu.cp470.wellnest.data.local.room.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "completion_tag",
        primaryKeys = {"completionId", "tagId"},
        foreignKeys = {
                @ForeignKey(entity = Completion.class, parentColumns = "id", childColumns = "completionId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = ActivityTag.class, parentColumns = "id", childColumns = "tagId", onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index(value = {"completionId"}, name = "completion_tag_completion_idx"),
                @Index(value = {"tagId"}, name = "completion_tag_tag_idx")
        }
)
public class CompletionTag {
    public @NonNull String completionId;  // TEXT FK → Completion.id
    public int tagId;
}
