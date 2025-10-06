package com.code.wlu.cp470.wellnest.data.local.room.dao;

import androidx.room.*;
import com.code.wlu.cp470.wellnest.data.local.room.entities.*;
import java.util.List;

@Dao
public interface RoamioDao {

    // ---- Current walk (singleton row id=1) ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertCurrent(CurrentWalkLocal current);

    @Query("SELECT * FROM current_walk_local WHERE id = 1")
    CurrentWalkLocal getCurrent();

    @Query("DELETE FROM current_walk_local")
    void clearCurrent();

    // ---- Sessions ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSession(WalkSessionLocal s);

    @Query("SELECT * FROM walk_session_local ORDER BY startedAt DESC")
    List<WalkSessionLocal> listSessions();

    @Query("DELETE FROM walk_session_local")
    void clearSessions();

    // Convenience transaction: finish a walk
    @Transaction
    default long finishWalk(CurrentWalkLocal c, int points, String routeGeoJson) {
        if (c == null || c.isActive == 0) return -1;
        WalkSessionLocal s = new WalkSessionLocal();
        s.startedAt = c.startedAt;
        s.endedAt = System.currentTimeMillis();
        s.steps = c.steps;
        s.meters = c.meters;
        s.points = points;
        s.routeGeoJson = routeGeoJson;
        long id = insertSession(s);
        clearCurrent();
        return id;
    }
}
