# Local User Storage — DataStore + Room (Java)

**Goal:** Replace singleton tables with **DataStore (Proto)** and keep lists in **Room**. Store only what the device needs: profile, global score, and streaks in Proto; friends & badges in Room.

---

## DataStore (Proto, Java)

### 1) `user_profile.proto`

```proto
syntax = "proto3";
option java_package = "com.example.wellnest";
option java_multiple_files = true;

message UserProfile {
  string uid          = 1;  // optional mirror of Firebase UID
  string email        = 2;  // optional
  string name         = 3;  // optional
  int64  updated_at_ms= 4;  // last update time
}
```

**RxDataStore (Java)**

```java
RxDataStore<UserProfile> userProfileStore =
    RxDataStoreFactory.create(
        new UserProfileSerializer(),
        () -> new File(context.getFilesDir(), "user_profile.pb")
    );

public Single<UserProfile> saveUserProfile(String uid, String email, String name, long nowMs) {
  return userProfileStore.updateDataAsync(curr -> Single.just(
      curr.toBuilder()
          .setUid(uid == null ? "" : uid)
          .setEmail(email == null ? "" : email)
          .setName(name == null ? "" : name)
          .setUpdatedAtMs(nowMs)
          .build()
  ));
}

public Single<UserProfile> readUserProfile() {
  return userProfileStore.data().firstOrError();
}

public static final class UserProfileSerializer implements Serializer<UserProfile> {
  @NonNull @Override public UserProfile readFrom(@NonNull InputStream in) throws IOException {
    try { return UserProfile.parseFrom(in); }
    catch (InvalidProtocolBufferException e) { return UserProfile.getDefaultInstance(); }
  }
  @Override public void writeTo(@NonNull UserProfile v, @NonNull OutputStream out) throws IOException {
    v.writeTo(out);
  }
}
```

---

### 2) `global_score.proto` (dashboard total)

```proto
syntax = "proto3";
option java_package = "com.example.wellnest";
option java_multiple_files = true;

message GlobalScore {
  int32 total_points  = 1;  // cumulative app-wide score
  int64 updated_at_ms = 2;  // last time total was written
}
```

**RxDataStore (Java)**

```java
RxDataStore<GlobalScore> globalScoreStore =
    RxDataStoreFactory.create(
        new GlobalScoreSerializer(),
        () -> new File(context.getFilesDir(), "global_score.pb")
    );

// Prefer setting an explicit total to avoid drift.
public Single<GlobalScore> setGlobalPoints(int total, long nowMs) {
  return globalScoreStore.updateDataAsync(curr -> Single.just(
      curr.toBuilder().setTotalPoints(total).setUpdatedAtMs(nowMs).build()
  ));
}

public Single<Integer> readGlobalPoints() {
  return globalScoreStore.data().firstOrError().map(GlobalScore::getTotalPoints);
}

public static final class GlobalScoreSerializer implements Serializer<GlobalScore> {
  @NonNull @Override public GlobalScore readFrom(@NonNull InputStream in) throws IOException {
    try { return GlobalScore.parseFrom(in); }
    catch (InvalidProtocolBufferException e) { return GlobalScore.getDefaultInstance(); }
  }
  @Override public void writeTo(@NonNull GlobalScore v, @NonNull OutputStream out) throws IOException {
    v.writeTo(out);
  }
}
```

**Compute & write global total** (call whenever a micro‑app total changes):

```java
public Single<GlobalScore> updateGlobalFromModules(int snap, int jar, int roam, long nowMs) {
  int total = Math.max(0, snap) + Math.max(0, jar) + Math.max(0, roam);
  return setGlobalPoints(total, nowMs);
}
```

---

### 3) `streak.proto` (replaces singleton table)

```proto
syntax = "proto3";
option java_package = "com.example.wellnest";
option java_multiple_files = true;

message Streak {
  int32 current       = 1;
  int32 longest       = 2;
  string last_active_yyyy_mm_dd = 3; // ISO date, e.g. "2025-10-02"
}
```

**RxDataStore (Java)**

```java
RxDataStore<Streak> streakStore =
    RxDataStoreFactory.create(
        new StreakSerializer(),
        () -> new File(context.getFilesDir(), "streak.pb")
    );

public Single<Streak> markActiveToday(String todayIso) {
  return streakStore.updateDataAsync(curr -> Single.just(updateStreak(curr, todayIso)));
}

private Streak updateStreak(Streak curr, String today) {
  String last = curr.getLastActiveYyyyMmDd();
  int current = curr.getCurrent();
  int longest = curr.getLongest();
  if (!today.equals(last)) {
    // naive: if last is yesterday, increment; else reset
    boolean consecutive = isYesterdayOf(today, last);
    current = consecutive ? current + 1 : 1;
    if (current > longest) longest = current;
  }
  return curr.toBuilder()
      .setCurrent(current)
      .setLongest(longest)
      .setLastActiveYyyyMmDd(today)
      .build();
}

private boolean isYesterdayOf(String today, String last) {
  // implement with java.time.LocalDate
  try {
    java.time.LocalDate t = java.time.LocalDate.parse(today);
    java.time.LocalDate l = (last == null || last.isEmpty()) ? null : java.time.LocalDate.parse(last);
    return l != null && l.plusDays(1).equals(t);
  } catch (Exception e) { return false; }
}

public static final class StreakSerializer implements Serializer<Streak> {
  @NonNull @Override public Streak readFrom(@NonNull InputStream in) throws IOException {
    try { return Streak.parseFrom(in); }
    catch (InvalidProtocolBufferException e) { return Streak.getDefaultInstance(); }
  }
  @Override public void writeTo(@NonNull Streak v, @NonNull OutputStream out) throws IOException {
    v.writeTo(out);
  }
}
```

---

## Room (lists only)

### `FRIEND_LOCAL`

| Column       | Type | Notes                            |
| ------------ | ---- | -------------------------------- |
| `friendUid`  | TEXT | **PK**. Cloud UID of the friend. |
| `friendName` | TEXT | Display name.                    |

### `BADGE_LOCAL`

| Column     | Type    | Notes                                                        |
| ---------- | ------- | ------------------------------------------------------------ |
| `id`       | TEXT    | **PK**. Local/Cloud docId or stable code (e.g., `STREAK_3`). |
| `code`     | TEXT    | Badge code (enum‑like).                                      |
| `earnedAt` | INTEGER | Unix ms when earned.                                         |

**Entities (Java)**

```java
@Entity(tableName = "friend_local")
public class FriendLocal {
  @PrimaryKey @NonNull public String friendUid;
  public String friendName;
}

@Entity(tableName = "badge_local")
public class BadgeLocal {
  @PrimaryKey @NonNull public String id; // docId or stable code
  public String code;                    // e.g., STREAK_3
  public long earnedAt;                  // unix ms
}
```

**DAO (Java)**

```java
@Dao
public interface UserLocalDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void upsertFriends(List<FriendLocal> friends);

  @Query("SELECT * FROM friend_local ORDER BY friendName ASC")
  List<FriendLocal> listFriends();

  @Query("DELETE FROM friend_local WHERE friendUid = :uid")
  void removeFriend(String uid);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void upsertBadges(List<BadgeLocal> badges);

  @Query("SELECT * FROM badge_local ORDER BY earnedAt DESC")
  List<BadgeLocal> listBadges();
}
```

---

## Notes

* **No singleton Room tables**: profile, streak, and global score now live in Proto files (`user_profile.pb`, `streak.pb`, `global_score.pb`).
* **Global score**: write via `setGlobalPoints(total)`; typically compute `total = snap + jar + roam` from each micro‑app’s Proto stores.
* **Logout hygiene**: on sign‑out/switch user, call `database.clearAllTables()` and delete the three Proto files.
* **Multi‑account later**: either include `uid` inside each Proto (already in `UserProfile`) and keep one file per account name, or prefix filenames with the active UID.
