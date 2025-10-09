package com.code.wlu.cp470.wellnest.data.local.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.code.wlu.cp470.wellnest.data.local.room.entities.PendingVerify;
import com.code.wlu.cp470.wellnest.data.local.room.entities.TaskLocal;

import java.util.List;

@Dao
public interface SnapTaskDao {

    // ---- Tasks ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertTasks(List<TaskLocal> tasks);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertTask(TaskLocal t);

    // Update task result after verification succeeds
    @Query("UPDATE task_local SET status = :status, pointsAwarded = :points, createdAt = :ts WHERE id = :taskId")
    void setResult(String taskId, String status, Integer points, long ts);

    @Query("SELECT * FROM task_local WHERE status = :status ORDER BY createdAt DESC")
    List<TaskLocal> listByStatus(String status);

    @Query("SELECT * FROM task_local ORDER BY createdAt DESC")
    List<TaskLocal> listAll();

    @Query("DELETE FROM task_local")
    void clearTasks();

    // ---- Pending Verify ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPending(PendingVerify p);

    @Query("DELETE FROM pending_verify WHERE taskId = :taskId")
    void clearPendingForTask(String taskId);

    @Query("SELECT * FROM pending_verify ORDER BY lastAttemptAt DESC")
    List<PendingVerify> listPending();
}
