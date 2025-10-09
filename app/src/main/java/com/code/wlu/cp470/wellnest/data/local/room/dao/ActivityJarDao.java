package com.code.wlu.cp470.wellnest.data.local.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.code.wlu.cp470.wellnest.data.local.room.entities.ActivityCategory;
import com.code.wlu.cp470.wellnest.data.local.room.entities.ActivityJar;
import com.code.wlu.cp470.wellnest.data.local.room.entities.ActivityTag;
import com.code.wlu.cp470.wellnest.data.local.room.entities.ActivityTagXref;
import com.code.wlu.cp470.wellnest.data.local.room.entities.Completion;
import com.code.wlu.cp470.wellnest.data.local.room.entities.CompletionTag;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface ActivityJarDao {

    // ---- Activities ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertActivities(List<ActivityJar> list);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertActivity(ActivityJar a);

    @Query("UPDATE activity_jar SET isArchived = :isArchived WHERE id = :id")
    void setArchived(String id, int isArchived);

    @Query("SELECT * FROM activity_jar WHERE isArchived = 0 ORDER BY updatedAt DESC")
    List<ActivityJar> listActive();

    @Query("SELECT * FROM activity_jar WHERE categoryId = :categoryId AND isArchived = 0 ORDER BY updatedAt DESC")
    List<ActivityJar> listByCategory(int categoryId);

    @Query("SELECT * FROM activity_jar WHERE title LIKE '%' || :q || '%' AND isArchived = 0 ORDER BY updatedAt DESC")
    List<ActivityJar> searchByTitle(String q);

    // ---- Categories ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsertCategory(ActivityCategory c);

    @Query("SELECT * FROM activity_category ORDER BY name")
    List<ActivityCategory> listCategories();

    // ---- Tags ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsertTag(ActivityTag t);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertTags(List<ActivityTag> tags);

    @Query("SELECT * FROM activity_tag ORDER BY name")
    List<ActivityTag> listTags();

    // ---- Tag Xref ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void attachTagsToActivity(List<ActivityTagXref> xrefs);

    @Query("DELETE FROM activity_tag_xref WHERE activityId = :activityId")
    void clearActivityTags(String activityId);

    @Query("SELECT t.* FROM activity_tag t " +
            "INNER JOIN activity_tag_xref x ON x.tagId = t.id " +
            "WHERE x.activityId = :activityId " +
            "ORDER BY t.name")
    List<ActivityTag> tagsForActivity(String activityId);

    // ---- Completions (String UUID PKs) ----
    // Completion.id is a String (UUID). Do NOT rely on returned rowIds.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertCompletion(Completion c);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertCompletionTags(List<CompletionTag> cts);

    @Query("SELECT * FROM completion ORDER BY completedAt DESC")
    List<Completion> listCompletions();

    @Transaction
    default void insertCompletionWithTags(Completion c, List<Integer> tagIds) {
        // Expect c.id to already be a UUID string
        insertCompletion(c);
        if (tagIds == null || tagIds.isEmpty()) return;

        ArrayList<CompletionTag> list = new ArrayList<>(tagIds.size());
        for (int tagId : tagIds) {
            CompletionTag ct = new CompletionTag();
            ct.completionId = c.id;   // String FK to Completion.id
            ct.tagId = tagId;
            list.add(ct);
        }
        insertCompletionTags(list);
    }
}
