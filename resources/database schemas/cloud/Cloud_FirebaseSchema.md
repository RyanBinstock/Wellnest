# Wellnest — Firebase Cloud DB Schema (Firestore)

Design principles (from the local schemas):

* **No photos in the cloud** (SnapTask images are local-only).
* **Micro‑app scores are local** (DataStore Proto). Firestore keeps only a **globalPoints** counter for leaderboards and cross‑device.
* **SnapTask tasks live in a global collection** with a **recurrence field**; clients seed today’s local TODOs by querying for tasks whose recurrence includes **today’s day‑of‑week**. No user task history is stored in Firestore.
* **ActivityJar stock activities** are hosted in a top‑level `activities` collection. Categories/tags remain **local‑only**.
* **Roamio** stays local‑only; no cloud uploads of sessions or step samples.

---

## Collections & Documents

### `users/{uid}` (profile summary)

Holds minimal user profile and **globalPoints** source of truth.

```jsonc
users/{uid}: {
  displayName: string,
  globalPoints: number,     // updated by backend only
  createdAt: timestamp,
  updatedAt: timestamp
}
```

#### Subcollections under `users/{uid}`

1) `friends/{edgeId}`

```jsonc
friends/{edgeId}: {
  friendUid: string,
  status: "pending" | "accepted" | "blocked",
  updatedAt: timestamp
}
```

---

### `snap_tasks/{taskId}` — Global SnapTask catalog with recurrence

Primary source for the app’s **TASK_LOCAL** seeding. Clients query by day‑of‑week to build today’s list.

```jsonc
snap_tasks/{taskId}: {
  title: string,
  recurDow: number[],  // ISO day-of-week integers: 1=Mon .. 7=Sun
  basePoints: number,  // optional hint; app/backend decides final award
  active: boolean,     // if false, client should ignore
  updatedAt: timestamp
}
```

Query pattern on client (example): `where("active", "==", true).where("recurDow", "array-contains", todayIsoDow)`.

---

### `activities/{id}` — ActivityJar stock content

Public (or read‑only) catalog of activities you can add without app updates. Categories/tags remain local.

```jsonc
activities/{id}: {
  title: string,
  summary: string,
  categoryId: string,  // maps to local category dictionary
  updatedAt: timestamp,
  active: boolean
}
```

---

### `leaderboards/global/{uid}` — Denormalized ranks (optional)

Write‑only by backend; readable by all.

```jsonc
leaderboards/global/{uid}: {
  displayName: string,
  points: number,       // mirror of users/{uid}.globalPoints
  updatedAt: timestamp
}
```

---

## Security Rules (starter)

Key ideas:

* Users may read/write only within `users/{uid}` where `uid == auth.uid`.
* Clients **cannot mutate** `globalPoints` directly.
* Catalog and SnapTask global lists are read‑only to clients.

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isOwner(uid) {
      return request.auth != null && request.auth.uid == uid;
    }

    match /users/{uid} {
      allow read: if isOwner(uid);
      allow create: if isOwner(uid);

      // Prevent client from changing globalPoints directly
      allow update: if isOwner(uid) && (
        request.resource.data.globalPoints == resource.data.globalPoints
      );

      match /activities/{savedId} {
        allow read, write: if isOwner(uid);
      }

      match /friends/{edgeId} {
        allow read, write: if isOwner(uid);
      }
    }

    // Global SnapTask source — read‑only
    match /snap_tasks/{taskId} {
      allow read: if request.auth != null; // or true if fully public
      allow write: if false; // backend/admin only
    }

    // Public activities — read‑only
    match /activities/{id} {
      allow read: if request.auth != null; // or true if fully public
      allow write: if false; // backend/admin only
    }

    // Leaderboard — public read, backend write
    match /leaderboards/global/{uid} {
      allow read: if true;
      allow write: if false;
    }
  }
}
```

---

## Indexes (suggested)

* `snap_tasks` — no composite needed; use `array-contains` on `recurDow` and equality on `active`.
* `users/{uid}/activities` — single field on `savedAt DESC` (if used).
* `leaderboards/global` — single field on `points DESC`.

---

## Event Flows (how Firestore ties to the local DB)

### SnapTask

1. **Morning fetch:** client queries `snap_tasks` with `active == true` and `array-contains recurDow == todayIsoDow` → seeds **TASK_LOCAL** for the day (no per‑user task docs created in Firestore).
2. **User completes task:** verification and photos are **local‑only**; update local status and points (and optionally call a backend function `awardPoints(delta)` to bump `users/{uid}.globalPoints`).

### ActivityJar

* Client fetches `activities/*` → upserts into **ACTIVITY_JAR**. Local categories/tags stay local.
* When awarding ActivityJar points (custom logic), optionally call a backend function `awardPoints(delta)` → backend increments `users/{uid}.globalPoints`.

### Notes

* Roamio is local‑only; no Firestore writes.
* If you later need per‑user SnapTask history, add `users/{uid}/task_runs/{id}` without affecting the global `snap_tasks` catalog.
