# SnapTask Local DB Schema (Room/SQLite)

This doc captures the **minimal local schema** for SnapTask with a separate **PENDING_VERIFY** queue used only while the GPT verification call is pending. Photos **never leave the device**; only file paths are stored locally and purged after success.

---

## Tables

### `TASK_LOCAL`

A daily list of TODO tasks (refreshed from the cloud each day). Tracks completion status and awarded points; does **not** store photos.

| Column          | Type    | Constraints / Notes                        |                |
| --------------- | ------- | ------------------------------------------ | -------------- |
| `id`            | TEXT    | **PK**. UUID or cloud doc ID.              |                |
| `title`         | TEXT    | Task title shown to the user.              |                |
| `status`        | TEXT    | `"FINISHED"                                | "UNFINISHED"`. |
| `pointsAwarded` | INTEGER | Nullable until verification completes.     |                |
| `createdAt`     | INTEGER | Unix ms. Used for daily refresh / sorting. |                |

**Suggested indices**

* `INDEX task_createdAt_idx(createdAt DESC)`
* `INDEX task_status_idx(status)`

**Notes**

* This table is your **authoritative local list** for today’s tasks. You can wipe/reseed daily from Firestore and keep only local state you need (finished/unfinished + awarded points).

---

### `PENDING_VERIFY`

Temporary queue holding file paths for tasks **currently awaiting API verification**. Removed when verification succeeds or the task is canceled.

| Column          | Type    | Constraints / Notes                                   |
| --------------- | ------- | ----------------------------------------------------- |
| `taskId`        | TEXT    | **PK**. FK to `TASK_LOCAL.id` (logical; FK optional). |
| `beforePath`    | TEXT    | App-private path to the **before** photo.             |
| `afterPath`     | TEXT    | App-private path to the **after** photo.              |
| `retries`       | INTEGER | Backoff counter (0 default).                          |
| `lastAttemptAt` | INTEGER | Unix ms of last attempt; used for backoff windows.    |

**Suggested indices**

* `INDEX pending_retry_idx(lastAttemptAt DESC)`

**Notes**

* Store **relative** paths under your app-private directory (e.g., `files/snaptask/<id>_before.jpg`).
* On successful verification: delete files, delete `PENDING_VERIFY` row, set `TASK_LOCAL.pointsAwarded`, set `status = "FINISHED"`.
* On failure: increment `retries`, set `lastAttemptAt`, keep row & files for retry.

---

## Room Entities (Java, minimal imports implied)

> Note: Keep imports implicit; use `@Entity`, `@PrimaryKey`, and `@Index`. All timestamps are Unix ms (`long`).

```java
@Entity(
  tableName = "task_local",
  indices = {
    @Index(value = {"createdAt"}, name = "task_createdAt_idx"),
    @Index(value = {"status"}, name = "task_status_idx")
  }
)
public class TaskLocal {
  @PrimaryKey @NonNull public String id;
  public String title;
  // "FINISHED" | "UNFINISHED"
  public String status;
  public Integer pointsAwarded;   // nullable
  public long createdAt;          // unix ms
}
```

```java
@Entity(
  tableName = "pending_verify",
  indices = { @Index(value = {"lastAttemptAt"}, name = "pending_retry_idx") }
)
public class PendingVerify {
  @PrimaryKey @NonNull public String taskId; // mirrors TaskLocal.id
  public String beforePath;                  // app-private relative path
  public String afterPath;                   // app-private relative path
  public int retries;                        // backoff counter
  public long lastAttemptAt;                 // unix ms
}
```

---

## DAO Sketch

```java
@Dao
public interface TaskDao {
  // TASK_LOCAL
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void upsertTasks(List<TaskLocal> tasks);

  @Query("SELECT * FROM task_local WHERE status = :status ORDER BY createdAt DESC")
  List<TaskLocal> listByStatus(String status);

  @Query("UPDATE task_local SET status = :status, pointsAwarded = :points WHERE id = :id")
  void setResult(String id, String status, Integer points);

  // PENDING_VERIFY
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void upsertPending(PendingVerify row);

  @Query("SELECT * FROM pending_verify ORDER BY lastAttemptAt DESC")
  List<PendingVerify> listPending();

  @Query("DELETE FROM pending_verify WHERE taskId = :taskId")
  void clearPending(String taskId);
}
```

---

## Lifecycle & Flow

1. **Daily refresh:** Clear and seed `TASK_LOCAL` from cloud. All tasks start `UNFINISHED`.
2. **Capture:** Save photos to app-private storage; write/replace one `PENDING_VERIFY` row.
3. **Verify:** Call the API. On success → update `TASK_LOCAL` (status to `FINISHED`, set `pointsAwarded`), delete files & `PENDING_VERIFY`.
4. **Retry policy:** If API fails, update `retries` + `lastAttemptAt`. A foreground service / WorkManager can retry based on backoff, **or** keep the UI blocking until a retry succeeds.

---

## Migrations (forward-compatible)

* If later you add reasons or audit: add `lastError TEXT` to `TASK_LOCAL`.
* If you add hard deletes: add `deletedAt INTEGER` soft-delete columns instead of physical deletes.
* If multi-account arrives: add a `uid TEXT` column to both tables and use composite PKs `(uid, id)` / `(uid, taskId)`.
