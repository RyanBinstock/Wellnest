# ActivityJar — Local Storage Schema (Room + DataStore Proto)

This schema supports:

* **Stock activities** fetched from Firebase and synced into the local DB (so we can add content without app releases).
* **Locally-defined categories & tags** (no Firebase source of truth).
* **On-device generated activities** (Tavily/GPT) stored for later reuse.
* **Completions history** with the ability to query each completion’s **category, tags, and timestamp**.
* A **lightweight key–value score** for ActivityJar using **DataStore (Proto)** in **Java**.

---

## Tables (Room/SQLite)

### `ACTIVITY_JAR`

Local catalog of all activities (both **stock** from cloud and **generated** on-device).

| Column       | Type    | Constraints / Notes                                   |
| ------------ | ------- | ----------------------------------------------------- |
| `id`         | TEXT    | **PK**. Mirror of Firestore doc ID (cloud canonical). |
| `title`      | TEXT    | Activity title.                                       |
| `summary`    | TEXT    | Optional blurb/description.                           |
| `categoryId` | INTEGER | FK → `ACTIVITY_CATEGORY.id`.                          |
| `source`     | TEXT    | Enum: `STOCK`, `GENERATED`, `USER`.                   |
| `createdAt`  | INTEGER | Unix ms.                                              |
| `updatedAt`  | INTEGER | Unix ms.                                              |
| `isArchived` | INTEGER | 0/1. Use instead of deletes to preserve references.   |

**Indices**

* `INDEX activity_category_idx(categoryId)`
* `INDEX activity_source_idx(source)`

---

### `ACTIVITY_CATEGORY` (local-only dictionary)

| Column | Type    | Constraints / Notes   |
| ------ | ------- | --------------------- |
| `id`   | INTEGER | **PK AUTOINCREMENT**  |
| `name` | TEXT    | Unique category name. |
|        |         |                       |

**Indices**

* `UNIQUE(name)`

---

### `ACTIVITY_TAG` (local-only dictionary)

| Column | Type    | Constraints / Notes  |
| ------ | ------- | -------------------- |
| `id`   | INTEGER | **PK AUTOINCREMENT** |
| `name` | TEXT    | Unique tag key       |

**Indices**

* `UNIQUE(name)`

---

### `ACTIVITY_TAG_XREF` (many-to-many between activities and tags)

| Column       | Type    | Constraints / Notes             |
| ------------ | ------- | ------------------------------- |
| `activityId` | TEXT    | FK → `ACTIVITY_JAR.id`          |
| `tagId`      | INTEGER | FK → `ACTIVITY_TAG.id`          |
| **PK**       |         | Composite `(activityId, tagId)` |

**Indices**

* `INDEX activity_tag_xref_activity_idx(activityId)`
* `INDEX activity_tag_xref_tag_idx(tagId)`

---

### `COMPLETION`

Records each time the user completes an activity. We **snapshot** the category at completion time and link tags via a completion junction so category/tag queries are stable even if the activity’s tags change later.

| Column                 | Type    | Constraints / Notes                    |
| ---------------------- | ------- | -------------------------------------- |
| `id`                   | TEXT    | **PK**. UUID.                          |
| `activityId`           | TEXT    | FK → `ACTIVITY_JAR.id`.                |
| `completedAt`          | INTEGER | Unix ms timestamp.                     |
| `categoryIdAtComplete` | INTEGER | Snapshot of category FK at completion. |
| `notes`                | TEXT    | Optional user notes (nullable).        |
|                        |         |                                        |

**Indices**

* `INDEX completion_time_idx(completedAt DESC)`
* `INDEX completion_activity_idx(activityId)`
* `INDEX completion_category_snapshot_idx(categoryIdAtComplete)`

---

### `COMPLETION_TAG` (many-to-many between a completion and tags)

| Column         | Type    | Constraints / Notes               |
| -------------- | ------- | --------------------------------- |
| `completionId` | TEXT    | FK → `COMPLETION.id`              |
| `tagId`        | INTEGER | FK → `ACTIVITY_TAG.id`            |
| **PK**         |         | Composite `(completionId, tagId)` |

**Indices**

* `INDEX completion_tag_completion_idx(completionId)`
* `INDEX completion_tag_tag_idx(tagId)`

---

## Room Entities (Java, minimal imports implied)

```java
@Entity(
  tableName = "activity_jar",
  indices = {
    @Index(value = {"categoryId"}, name = "activity_category_idx"),
    @Index(value = {"source"}, name = "activity_source_idx")
  }
)
public class ActivityJar {
  @PrimaryKey @NonNull public String id;
  public String title;
  public String summary;       // nullable
  public int categoryId;       // FK to ActivityCategory.id
  public String source;        // STOCK | GENERATED | USER
  public long createdAt;
  public long updatedAt;
  public int isArchived;       // 0/1
}

@Entity(tableName = "activity_category", indices = {@Index(value = {"name"}, unique = true)})
public class ActivityCategory {
  @PrimaryKey(autoGenerate = true) public int id;
  public String name;
  public String colorHex;  // nullable
  public Integer sortKey;  // nullable
}

@Entity(tableName = "activity_tag", indices = {@Index(value = {"name"}, unique = true)})
public class ActivityTag {
  @PrimaryKey(autoGenerate = true) public int id;
  public String name;
}

@Entity(
  tableName = "activity_tag_xref",
  primaryKeys = {"activityId", "tagId"},
  foreignKeys = {
    @ForeignKey(entity = ActivityJar.class, parentColumns = "id", childColumns = "activityId", onDelete = ForeignKey.CASCADE),
    @ForeignKey(entity = ActivityTag.class, parentColumns = "id", childColumns = "tagId", onDelete = ForeignKey.CASCADE)
  },
  indices = {
    @Index(value = {"activityId"}, name = "activity_tag_xref_activity_idx"),
    @Index(value = {"tagId"}, name = "activity_tag_xref_tag_idx")
  }
)
public class ActivityTagXref {
  @NonNull public String activityId;
  public int tagId;
}

@Entity(
  tableName = "completion",
  indices = {
    @Index(value = {"completedAt"}, name = "completion_time_idx"),
    @Index(value = {"activityId"}, name = "completion_activity_idx"),
    @Index(value = {"categoryIdAtComplete"}, name = "completion_category_snapshot_idx")
  }
)
public class Completion {
  @PrimaryKey @NonNull public String id;
  public String activityId;    // FK to ActivityJar.id
  public long completedAt;     // unix ms
  public int categoryIdAtComplete;
  public String notes;         // nullable
}

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
  @NonNull public String completionId;
  public int tagId;
}
```

---

## DAO Sketch (Java)

```java
@Dao
public interface ActivityJarDao {
  // --- Catalog upsert ---
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void upsertActivities(List<ActivityJar> activities);

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insertCategory(ActivityCategory c);

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insertTag(ActivityTag t);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void upsertTagsXref(List<ActivityTagXref> xrefs);

  // --- Queries ---
  @Query("SELECT * FROM activity_jar WHERE isArchived = 0 ORDER BY updatedAt DESC")
  List<ActivityJar> listAll();

  @Query("SELECT * FROM activity_jar WHERE categoryId = :categoryId AND isArchived = 0 ORDER BY updatedAt DESC")
  List<ActivityJar> listByCategory(int categoryId);

  @Query("SELECT a.* FROM activity_jar a\n         JOIN activity_tag_xref x ON x.activityId = a.id\n         JOIN activity_tag t ON t.id = x.tagId\n         WHERE t.name IN (:tagNames) AND a.isArchived = 0\n         GROUP BY a.id\n         ORDER BY a.updatedAt DESC")
  List<ActivityJar> listByAnyTag(List<String> tagNames);

  // --- Completions ---
  @Insert(onConflict = OnConflictStrategy.ABORT)
  void insertCompletion(Completion c);

  @Insert(onConflict = OnConflictStrategy.ABORT)
  void insertCompletionTags(List<CompletionTag> tags);

  // Completion + category + tags for history UI
  @Query("SELECT c.id AS completionId, c.completedAt, a.title, cat.name AS category\n         FROM completion c\n         JOIN activity_jar a ON a.id = c.activityId\n         JOIN activity_category cat ON cat.id = c.categoryIdAtComplete\n         ORDER BY c.completedAt DESC\n         LIMIT :limit OFFSET :offset")
  List<CompletionSummaryRow> listCompletionSummaries(int limit, int offset);

  // Tags for a given completion
  @Query("SELECT t.name FROM completion_tag ct JOIN activity_tag t ON t.id = ct.tagId WHERE ct.completionId = :completionId")
  List<String> listCompletionTags(String completionId);
}
```

*(Define ********************`CompletionSummaryRow`******************** as a Room @DatabaseView or a small POJO with matching fields.)*

---

## ActivityJar Score — DataStore (Proto, Java)

Use a lightweight key–value store for scoring. No daily reset. Points awarded by your **custom logic** (not directly tied to a completion).

**Proto (********`activity_jar_score.proto`****************************************):**

```proto
syntax = "proto3";
option java_package = "com.example.wellnest";
option java_multiple_files = true;

message ActivityJarScore {
  int32 total_points  = 1;
  int64 updated_at_ms = 2;
}
```

**Java (RxDataStore Proto):**

```java
RxDataStore<ActivityJarScore> jarScoreStore =
    RxDataStoreFactory.create(
        new ActivityJarScoreSerializer(),
        () -> new File(context.getFilesDir(), "activity_jar_score.pb")
    );

public Single<ActivityJarScore> addJarPoints(int delta, long nowMs) {
  return jarScoreStore.updateDataAsync(current ->
      Single.just(current.toBuilder()
        .setTotalPoints(current.getTotalPoints() + delta)
        .setUpdatedAtMs(nowMs)
        .build())
  );
}

public Single<Integer> readJarPoints() {
  return jarScoreStore.data().firstOrError().map(ActivityJarScore::getTotalPoints);
}

public static final class ActivityJarScoreSerializer implements Serializer<ActivityJarScore> {
  @NonNull @Override
  public ActivityJarScore readFrom(@NonNull InputStream input) throws IOException {
    try { return ActivityJarScore.parseFrom(input); }
    catch (InvalidProtocolBufferException e) { return ActivityJarScore.getDefaultInstance(); }
  }
  @Override
  public void writeTo(@NonNull ActivityJarScore t, @NonNull OutputStream output) throws IOException {
    t.writeTo(output);
  }
}
```

---

## Sync & Generation Flows

**1) Sync stock activities from Firebase → Room**

* Listen to `catalog/activities` (or whatever path you choose) for **added/modified** docs.
* Map each remote doc to `ActivityJar` row:

  * `id` set from cloud; `source = STOCK`.
  * **Do not** overwrite local `categoryId`/tags automatically unless you intentionally push a mapping update.
* Upsert into `ACTIVITY_JAR`; manage `isArchived` instead of hard deletes to preserve history.

**2) Generate activities (Tavily/GPT) → Room**

* For each generated idea, create an `ActivityJar` row with `source = GENERATED` and local category/tags.
* These are **local only**; no Firebase write needed.

**3) Mark completion**

* Insert into `COMPLETION` with `activityId`, `completedAt`, and **snapshot** `categoryIdAtComplete`.
* Insert a set of `COMPLETION_TAG` rows from the activity’s current tags to freeze the tag state at completion time.
* Separately (if desired), call `addJarPoints(delta)` to update the ActivityJar score via DataStore.

**4) Queries you asked for**

* *“What category and tags did a completed activity have and when?”*

  * Join `COMPLETION` → `activity_jar` → `activity_category`, and `COMPLETION_TAG` → `activity_tag` to return `{completedAt, title, category, [tags...]}`.

---

## Notes & Options

* Keep categories/tags **local-only** so you can iterate freely without cloud migrations.
* If you later want to push curated tags from cloud, add a `REMOTE_CATEGORY_MAP` or a special flag on `ACTIVITY_JAR` rows you allow the cloud to override.
* If you want faster tag queries, consider a **materialized tags text** column on `COMPLETION` (comma-separated) purely for display, while still maintaining normalized `COMPLETION_TAG` for correctness.
