package com.code.wlu.cp470.wellnest.data.local.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.code.wlu.cp470.wellnest.data.local.room.entities.CurrentWalkLocal;
import com.code.wlu.cp470.wellnest.data.local.room.entities.WalkSessionLocal;

import java.util.List;

@Dao
public interface RoamioDao {

    // ---- Current walk ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertCurrent(CurrentWalkLocal current);

    @Query("SELECT * FROM current_walk_local LIMIT 1")
    CurrentWalkLocal getCurrent();

    @Query("DELETE FROM current_walk_local WHERE id = :id")
    void clearCurrent(String id);

    @Query("DELETE FROM current_walk_local")
    void clearAllCurrent();

    // ---- Sessions ----
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertSession(WalkSessionLocal s);

    @Query("SELECT * FROM walk_session_local ORDER BY endedAt DESC LIMIT :limit OFFSET :offset")
    List<WalkSessionLocal> listHistory(int limit, int offset);

    @Query("DELETE FROM walk_session_local")
    void clearSessions();

    // ---- Convenience transaction: finish a walk ----
    @Transaction
    default void finishWalk(CurrentWalkLocal current, int points, double distanceMeters, int steps) {
        if (current == null || !"ACTIVE".equals(current.status)) return;

        WalkSessionLocal session = new WalkSessionLocal();
        session.id = java.util.UUID.randomUUID().toString();
        session.startedAt = current.startedAt;
        session.endedAt = System.currentTimeMillis();
        session.steps = steps;
        session.distanceMeters = distanceMeters;
        session.pointsAward = points;
        session.status = "COMPLETED";

        insertSession(session);
        clearCurrent(current.id);
    }
}
