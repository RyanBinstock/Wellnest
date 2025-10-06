package com.code.wlu.cp470.wellnest.data.local.room.dao;

import androidx.room.*;
import com.code.wlu.cp470.wellnest.data.local.room.entities.*;
import java.util.List;

@Dao
public interface SnapTaskDao {

    // ---- Tasks ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertTasks(List<TaskLocal> tasks);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsertTask(TaskLocal t);

    @Query("UPDATE task_local SET status = :status, points = :points, updatedAt = :ts WHERE id = :taskId")
    void setResult(int taskId, String status, int points, long ts);

    @Query("SELECT * FROM task_local WHERE status = :status ORDER BY updatedAt DESC")
    List<TaskLocal> listByStatus(String status);

    @Query("SELECT * FROM task_local ORDER BY updatedAt DESC")
    List<TaskLocal> listAll();

    @Query("DELETE FROM task_local")
    void clearTasks();

    // ---- Pending Verify ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPending(PendingVerify p);

    @Query("DELETE FROM pending_verify WHERE taskId = :taskId")
    void clearPendingForTask(int taskId);

    @Query("SELECT * FROM pending_verify ORDER BY submittedAt DESC")
    List<PendingVerify> listPending();
}
