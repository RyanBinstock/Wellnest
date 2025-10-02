# Roamio — Local Storage Schema (Room + DataStore Proto)

This schema keeps Roamio simple and resilient:

* **KV score** stored via **DataStore (Proto)** (Java) — no daily reset.
* **History of walk sessions** in Room for basic stats/history.
* **Temporary current-walk table** in Room so the micro‑game continues offline (process kill‑safe) as long as the day was unlocked online once.

---

## Tables (Room/SQLite)

### `WALK_SESSION_LOCAL` — History

Summary rows for completed/cancelled walks.

| Column           | Type    | Constraints / Notes                          |
| ---------------- | ------- | -------------------------------------------- |
| `id`             | TEXT    | **PK**. UUID or server ID mirror.            |
| `startedAt`      | INTEGER | Unix ms.                                     |
| `endedAt`        | INTEGER | Unix ms; nullable until finalized.           |
| `steps`          | INTEGER | Total steps for the session.                 |
| `distanceMeters` | REAL    | Derived from steps/stride or GPS.            |
| `pointsAward`    | INTEGER | Nullable; if points are awarded per session. |
| `status`         | TEXT    | `COMPLETED` or `CANCELLED`.                  |

**Indices**

* `INDEX walk_hist_time_idx(endedAt DESC)`

---

### `CURRENT_WALK_LOCAL` — Active session (kill‑safe & offline)

Stores the *minimum* needed to recover after process death and operate offline.

| Column                    | Type    | Constraints / Notes                                        |
| ------------------------- | ------- | ---------------------------------------------------------- |
| `id`                      | TEXT    | **PK**. Current session ID (e.g., UUID).                   |
| `status`                  | TEXT    | `ACTIVE` or `PAUSED`.                                      |
| `startedAt`               | INTEGER | Unix ms when the session began.                            |
| `startStepCount`          | INTEGER | Cumulative `TYPE_STEP_COUNTER` value at start.             |
| `startElapsedRealtimeMs`  | INTEGER | `SystemClock.elapsedRealtime()` at start (detect reboots). |
| `lastUpdatedMs`           | INTEGER | Unix ms of the last in‑DB checkpoint.                      |
| `lastKnownSteps`          | INTEGER | Cached steps so far (optional convenience for UI).         |
| `lastKnownDistanceMeters` | REAL    | Cached distance so far (optional).                         |

**Why these fields?** With the cumulative step counter, you can always recompute `steps = nowCounter - startStepCount` even after process death. The `startElapsedRealtimeMs` helps detect device reboots mid‑walk.

**Indices**

* `INDEX current_walk_updated_idx(lastUpdatedMs DESC)`

---

## Room Entities (Java)

```java
@Entity(
  tableName = "walk_session_local",
  indices = { @Index(value = {"endedAt"}, name = "walk_hist_time_idx") }
)
public class WalkSessionLocal {
  @PrimaryKey @NonNull public String id;
  public long startedAt;
  public Long endedAt;           // nullable
  public int steps;
  public double distanceMeters;
  public Integer pointsAward;    // nullable
  public String status;          // COMPLETED | CANCELLED
}

@Entity(
  tableName = "current_walk_local",
  indices = { @Index(value = {"lastUpdatedMs"}, name = "current_walk_updated_idx") }
)
public class CurrentWalkLocal {
  @PrimaryKey @NonNull public String id;     // current session id
  public String status;                      // ACTIVE | PAUSED
  public long startedAt;
  public int startStepCount;
  public long startElapsedRealtimeMs;
  public long lastUpdatedMs;
  public Integer lastKnownSteps;             // nullable
  public Double lastKnownDistanceMeters;     // nullable
}
```

---

## DAO Sketch (Java)

```java
@Dao
public interface RoamioDao {
  // --- History ---
  @Insert(onConflict = OnConflictStrategy.ABORT)
  void insertSession(WalkSessionLocal s);

  @Query("SELECT * FROM walk_session_local ORDER BY endedAt DESC LIMIT :limit OFFSET :offset")
  List<WalkSessionLocal> listHistory(int limit, int offset);

  // --- Current walk ---
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void upsertCurrent(CurrentWalkLocal c);

  @Query("SELECT * FROM current_walk_local LIMIT 1")
  CurrentWalkLocal getCurrent();

  @Query("DELETE FROM current_walk_local WHERE id = :id")
  void clearCurrent(String id);
}
```

---

## Roamio Score — DataStore (Proto, Java)

A simple key–value total for Roamio. **No daily reset.**

**Proto (********`roam_score.proto`********):**

```proto
syntax = "proto3";
option java_package = "com.example.wellnest";
option java_multiple_files = true;

message RoamScore {
  int32 total_points  = 1;
  int64 updated_at_ms = 2;
}
```

**Java (RxDataStore Proto):**

```java
RxDataStore<RoamScore> roamScoreStore =
    RxDataStoreFactory.create(
        new RoamScoreSerializer(),
        () -> new File(context.getFilesDir(), "roam_score.pb")
    );

public Single<RoamScore> addRoamPoints(int delta, long nowMs) {
  return roamScoreStore.updateDataAsync(current ->
      Single.just(current.toBuilder()
        .setTotalPoints(current.getTotalPoints() + delta)
        .setUpdatedAtMs(nowMs)
        .build())
  );
}

public Single<Integer> readRoamPoints() {
  return roamScoreStore.data().firstOrError().map(RoamScore::getTotalPoints);
}

public static final class RoamScoreSerializer implements Serializer<RoamScore> {
  @NonNull @Override
  public RoamScore readFrom(@NonNull InputStream input) throws IOException {
    try { return RoamScore.parseFrom(input); }
    catch (InvalidProtocolBufferException e) { return RoamScore.getDefaultInstance(); }
  }
  @Override
  public void writeTo(@NonNull RoamScore t, @NonNull OutputStream output) throws IOException {
    t.writeTo(output);
  }
}
```

---

## Recommended Flow (tying schema to behavior)

**Start Walk**

1. Create `CurrentWalkLocal` with new `id`, set `status=ACTIVE`, store `startStepCount`, `startElapsedRealtimeMs`, `startedAt`.
2. Begin foreground tracking (service) and optionally checkpoint `lastKnownSteps/distance` + `lastUpdatedMs` every few minutes.

**Resume After Kill/Offline**

* If a row exists in `current_walk_local`, recompute live `steps` from the sensor (`nowCounter - startStepCount`), refresh `lastKnownSteps/distance`, continue.

**End Walk**

1. Compute `steps`/`distance`; insert a `WalkSessionLocal` history row with `status=COMPLETED` and `endedAt`.
2. Award points (custom logic) and call `addRoamPoints(delta)` in DataStore.
3. `clearCurrent(id)` to remove the temp row.

**Cancel Walk**

* Insert a `WalkSessionLocal` with `status=CANCELLED` and `endedAt`, then `clearCurrent(id)`.
