# SnapTask Micro App - Sequence Diagrams

## Overview

SnapTask is a gamified task completion micro app within the Wellnest Android application. Users complete tasks by taking "before" and "after" photos, which are then evaluated by an AI to verify task completion. Upon successful completion, users earn points that contribute to their overall score, which evolves their character through different life stages.

### Architecture Components

| Layer | Component | Description |
|-------|-----------|-------------|
| **UI** | `SnapTaskActivity` | Main task list screen with RecyclerView |
| **UI** | `SnapTaskFragment` | Launcher fragment that starts SnapTaskActivity |
| **UI** | `SnapTaskAdapter` | RecyclerView adapter for task cards |
| **UI** | `SnapTaskDetailActivity` | Task detail with photo capture and AI evaluation |
| **ViewModel** | `SnapTaskViewModel` | Manages UI state and repository access |
| **Repository** | `SnapTaskRepository` | Orchestrates local and remote data sources |
| **Local** | `SnapTaskManager` | SQLite database operations |
| **Remote** | `FirebaseSnapTaskManager` | Firestore operations |
| **External** | `WellnestAiClient` | AI task evaluation service |

---

## 1. App Startup - Loading Tasks

This diagram shows the flow when a user opens the SnapTask micro app from the home screen.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant HomeFragment
    participant SnapTaskFragment
    participant SnapTaskActivity
    participant SnapTaskViewModel
    participant SnapTaskRepository
    participant SnapTaskManager
    participant SQLiteDB as SQLite Database

    User->>HomeFragment: Taps SnapTask card
    HomeFragment->>SnapTaskFragment: Navigate via NavController
    SnapTaskFragment->>SnapTaskActivity: startActivity with Intent
    SnapTaskFragment->>HomeFragment: navigateUp - remove from back stack
    
    SnapTaskActivity->>SnapTaskActivity: onCreate
    SnapTaskActivity->>SnapTaskViewModel: ViewModelProvider.get
    
    Note over SnapTaskViewModel: Constructor initializes dependencies
    SnapTaskViewModel->>SnapTaskManager: new SnapTaskManager with db
    SnapTaskManager->>SQLiteDB: ensureSingletonRows for score table
    SnapTaskViewModel->>SnapTaskRepository: new SnapTaskRepository
    
    SnapTaskActivity->>SnapTaskActivity: bindViews and setupRecycler
    SnapTaskActivity->>SnapTaskViewModel: getTasks
    SnapTaskViewModel->>SnapTaskRepository: getTasks
    SnapTaskRepository->>SnapTaskManager: getTasks
    SnapTaskManager->>SQLiteDB: SELECT * FROM tasks
    SQLiteDB-->>SnapTaskManager: Cursor with task rows
    SnapTaskManager-->>SnapTaskRepository: List of Task objects
    SnapTaskRepository-->>SnapTaskViewModel: List of Task objects
    SnapTaskViewModel-->>SnapTaskActivity: List of Task objects
    
    SnapTaskActivity->>SnapTaskAdapter: new SnapTaskAdapter with tasks
    SnapTaskActivity->>SnapTaskActivity: Calculate progress percentage
    SnapTaskActivity->>SnapTaskActivity: setProgressAnimated on WellnestProgressBar
    
    SnapTaskActivity->>SnapTaskViewModel: getScore
    SnapTaskViewModel->>SnapTaskRepository: getSnapTaskScore
    SnapTaskRepository->>SnapTaskManager: getSnapTaskScore
    SnapTaskManager->>SQLiteDB: SELECT score FROM snapTask_score WHERE id=1
    SQLiteDB-->>SnapTaskManager: Score value
    SnapTaskManager-->>SnapTaskActivity: Score integer
    
    SnapTaskActivity->>SnapTaskActivity: Update score TextView
    SnapTaskActivity->>SnapTaskActivity: Set character image based on score
    SnapTaskActivity-->>User: Display task list with progress
```

---

## 2. Task Sync from Firebase

This diagram shows how tasks are synchronized from Firestore to the local SQLite database. This typically happens during app initialization or when refreshing data.

```mermaid
sequenceDiagram
    autonumber
    participant SnapTaskRepository
    participant FirebaseSnapTaskManager
    participant Firestore
    participant SnapTaskManager
    participant SQLiteDB as SQLite Database

    SnapTaskRepository->>SnapTaskRepository: syncSnapTasks called
    SnapTaskRepository->>FirebaseSnapTaskManager: getTasks
    
    Note over FirebaseSnapTaskManager: Blocking call with Tasks.await
    FirebaseSnapTaskManager->>Firestore: collection micro_app_data/snap_task/tasks.get
    Firestore-->>FirebaseSnapTaskManager: QuerySnapshot
    
    loop For each DocumentSnapshot
        FirebaseSnapTaskManager->>FirebaseSnapTaskManager: Create Task from document
        Note over FirebaseSnapTaskManager: Fields: Name, Points, Description
    end
    
    FirebaseSnapTaskManager-->>SnapTaskRepository: List of Task objects
    
    loop For each Task
        SnapTaskRepository->>SnapTaskManager: upsertTask with uid, name, points, description, completed
        SnapTaskManager->>SQLiteDB: UPDATE tasks SET... WHERE uid=?
        alt Row does not exist
            SnapTaskManager->>SQLiteDB: INSERT INTO tasks VALUES...
        end
    end
    
    Note over SnapTaskRepository: Local DB now mirrors Firebase tasks
```

---

## 3. Completing a Task - Full Flow

This is the most complex flow, showing how a user completes a task with before/after photos and AI evaluation.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant SnapTaskActivity
    participant SnapTaskAdapter
    participant SnapTaskDetailActivity
    participant Camera as Camera Intent
    participant WellnestAiClient
    participant SnapTaskViewModel
    participant SnapTaskRepository
    participant SnapTaskManager
    participant SQLiteDB as SQLite Database

    User->>SnapTaskActivity: Views task list
    User->>SnapTaskAdapter: Taps incomplete task card
    SnapTaskAdapter->>SnapTaskDetailActivity: createIntent with task data
    SnapTaskAdapter->>SnapTaskActivity: startActivityForResult REQUEST_TASK_DETAIL
    
    SnapTaskDetailActivity->>SnapTaskDetailActivity: onCreate - mode = before
    SnapTaskDetailActivity->>SnapTaskDetailActivity: bindViews and wireUi
    SnapTaskDetailActivity->>SnapTaskDetailActivity: updateUIForMode - highlight Before card
    SnapTaskDetailActivity-->>User: Show task detail with Start Task button
    
    User->>SnapTaskDetailActivity: Taps Start Task button
    SnapTaskDetailActivity->>Camera: startActivityForResult ACTION_IMAGE_CAPTURE code 1001
    Camera-->>User: Open camera viewfinder
    User->>Camera: Takes before photo
    Camera-->>SnapTaskDetailActivity: onActivityResult with Bitmap
    
    SnapTaskDetailActivity->>SnapTaskDetailActivity: downscale and compress to JPEG
    SnapTaskDetailActivity->>SnapTaskDetailActivity: Store beforeImage bytes
    SnapTaskDetailActivity->>SnapTaskDetailActivity: mode = after
    SnapTaskDetailActivity->>SnapTaskDetailActivity: updateUIForMode - highlight After card
    SnapTaskDetailActivity-->>User: Show End Task button
    
    User->>SnapTaskDetailActivity: Taps End Task button
    SnapTaskDetailActivity->>Camera: startActivityForResult ACTION_IMAGE_CAPTURE code 1002
    Camera-->>User: Open camera viewfinder
    User->>Camera: Takes after photo
    Camera-->>SnapTaskDetailActivity: onActivityResult with Bitmap
    
    SnapTaskDetailActivity->>SnapTaskDetailActivity: downscale and compress to JPEG
    SnapTaskDetailActivity->>SnapTaskDetailActivity: Store afterImage bytes
    SnapTaskDetailActivity->>SnapTaskDetailActivity: evaluateTask
    SnapTaskDetailActivity->>SnapTaskDetailActivity: showLoadingOverlay with pulsing progress
    
    Note over SnapTaskDetailActivity,WellnestAiClient: Background thread execution
    SnapTaskDetailActivity->>WellnestAiClient: evaluateSnapTask with description, beforeImage, afterImage
    WellnestAiClient-->>SnapTaskDetailActivity: verdict - pass or fail
    
    alt verdict == pass
        SnapTaskDetailActivity->>SnapTaskDetailActivity: hideLoadingOverlay
        SnapTaskDetailActivity->>SnapTaskDetailActivity: handleTaskCompletionSuccess
        SnapTaskDetailActivity->>SnapTaskViewModel: completeTaskAndApplyScore with uid and points
        SnapTaskViewModel->>SnapTaskRepository: setTaskCompleted with uid
        SnapTaskRepository->>SnapTaskManager: setTaskCompleted with uid
        SnapTaskManager->>SQLiteDB: UPDATE tasks SET completed=1 WHERE uid=?
        SQLiteDB-->>SnapTaskManager: rows affected
        SnapTaskViewModel->>SnapTaskRepository: addToSnapTaskScore with points
        SnapTaskRepository->>SnapTaskManager: addToSnapTaskScore with delta
        SnapTaskManager->>SQLiteDB: BEGIN TRANSACTION
        SnapTaskManager->>SQLiteDB: UPDATE snapTask_score SET score=score+? WHERE id=1
        SnapTaskManager->>SQLiteDB: SELECT score FROM snapTask_score WHERE id=1
        SQLiteDB-->>SnapTaskManager: new score value
        SnapTaskManager->>SQLiteDB: COMMIT
        SnapTaskManager-->>SnapTaskRepository: new score
        SnapTaskDetailActivity->>SnapTaskDetailActivity: showSuccessDialog with points earned
        SnapTaskDetailActivity-->>User: Display success with points
        User->>SnapTaskDetailActivity: Taps Continue
        SnapTaskDetailActivity->>SnapTaskActivity: setResult RESULT_OK with task data
        SnapTaskDetailActivity->>SnapTaskDetailActivity: finishWithExitAnim
    else verdict == fail
        SnapTaskDetailActivity->>SnapTaskDetailActivity: hideLoadingOverlay
        SnapTaskDetailActivity->>SnapTaskDetailActivity: showFailureDialog
        SnapTaskDetailActivity-->>User: Display failure message
        User->>SnapTaskDetailActivity: Taps Try Again
        SnapTaskDetailActivity->>SnapTaskActivity: setResult RESULT_CANCELED
        SnapTaskDetailActivity->>SnapTaskDetailActivity: finishWithExitAnim
    end
    
    SnapTaskActivity->>SnapTaskActivity: onActivityResult REQUEST_TASK_DETAIL
    SnapTaskActivity->>SnapTaskActivity: refreshScoreAndCharacter
    SnapTaskActivity->>SnapTaskActivity: refreshTasks
    SnapTaskActivity-->>User: Updated task list and score
```

---

## 4. Score Synchronization - Local to Firebase

This diagram shows the once-daily score synchronization between local SQLite and Firebase Firestore.

```mermaid
sequenceDiagram
    autonumber
    participant SnapTaskRepository
    participant SharedPreferences
    participant SnapTaskManager
    participant SQLiteDB as SQLite Database
    participant FirebaseSnapTaskManager
    participant Firestore

    SnapTaskRepository->>SnapTaskRepository: syncSnapTaskScoreOnceDaily called
    SnapTaskRepository->>SharedPreferences: Get uid from user_repo_prefs
    
    alt uid is null or empty
        SnapTaskRepository->>SnapTaskRepository: Log warning and return
    end
    
    SnapTaskRepository->>SnapTaskRepository: Calculate todayEpochDay
    SnapTaskRepository->>SharedPreferences: Get last_sync_snap_task_score_epoch_day_uid
    
    alt lastEpochDay == todayEpochDay
        SnapTaskRepository->>SnapTaskRepository: Already synced today - return
    end
    
    SnapTaskRepository->>SnapTaskManager: getSnapTaskScore
    SnapTaskManager->>SQLiteDB: SELECT score FROM snapTask_score WHERE id=1
    SQLiteDB-->>SnapTaskManager: localScore
    SnapTaskManager-->>SnapTaskRepository: localScore
    
    SnapTaskRepository->>FirebaseSnapTaskManager: getScore with uid
    FirebaseSnapTaskManager->>Firestore: users/uid/microapp_scores/snap_task.get
    Firestore-->>FirebaseSnapTaskManager: DocumentSnapshot
    FirebaseSnapTaskManager-->>SnapTaskRepository: SnapTaskScore with remoteScore
    
    SnapTaskRepository->>SnapTaskRepository: finalScore = max of localScore and remoteScore
    
    alt finalScore != localScore
        SnapTaskRepository->>SnapTaskManager: upsertSnapTaskScore with finalScore
        SnapTaskManager->>SQLiteDB: UPDATE snapTask_score SET score=? WHERE id=1
    end
    
    alt remoteScore is null OR finalScore != remoteScore
        SnapTaskRepository->>FirebaseSnapTaskManager: upsertScore with SnapTaskScore
        FirebaseSnapTaskManager->>Firestore: users/uid/microapp_scores/snap_task.set
    end
    
    SnapTaskRepository->>SharedPreferences: Save todayEpochDay as last sync date
```

---

## 5. Task List Refresh After Completion

This diagram shows the simplified refresh flow when returning from task detail.

```mermaid
sequenceDiagram
    autonumber
    participant SnapTaskActivity
    participant SnapTaskViewModel
    participant SnapTaskRepository
    participant SnapTaskManager
    participant SQLiteDB as SQLite Database
    participant WellnestProgressBar
    participant RecyclerView

    SnapTaskActivity->>SnapTaskActivity: onActivityResult - REQUEST_TASK_DETAIL
    
    SnapTaskActivity->>SnapTaskViewModel: getScore
    SnapTaskViewModel->>SnapTaskRepository: getSnapTaskScore
    SnapTaskRepository->>SnapTaskManager: getSnapTaskScore
    SnapTaskManager->>SQLiteDB: SELECT score
    SQLiteDB-->>SnapTaskActivity: score value
    
    SnapTaskActivity->>SnapTaskActivity: Update score TextView
    
    alt score < 500
        SnapTaskActivity->>SnapTaskActivity: Set character to puffin_baby
    else score < 1000
        SnapTaskActivity->>SnapTaskActivity: Set character to puffin_teen
    else score < 1500
        SnapTaskActivity->>SnapTaskActivity: Set character to puffin_adult
    else score >= 1500
        SnapTaskActivity->>SnapTaskActivity: Set character to puffin_senior
    end
    
    SnapTaskActivity->>SnapTaskViewModel: getTasks
    SnapTaskViewModel->>SnapTaskRepository: getTasks
    SnapTaskRepository->>SnapTaskManager: getTasks
    SnapTaskManager->>SQLiteDB: SELECT * FROM tasks
    SQLiteDB-->>SnapTaskActivity: List of tasks
    
    SnapTaskActivity->>SnapTaskActivity: Count completed tasks
    SnapTaskActivity->>SnapTaskActivity: Calculate progress percentage
    SnapTaskActivity->>WellnestProgressBar: setProgressAnimated with percentage
    
    SnapTaskActivity->>SnapTaskAdapter: new SnapTaskAdapter with updated tasks
    SnapTaskActivity->>RecyclerView: setAdapter
```

---

## Key Interactions Notes

### Data Flow Pattern
The SnapTask micro app follows a clean MVVM architecture:
1. **UI Layer** → Activities and Adapters handle user interactions and display
2. **ViewModel Layer** → Exposes data to UI and delegates to Repository
3. **Repository Layer** → Orchestrates between local and remote data sources
4. **Data Layer** → Separate managers for SQLite and Firestore operations

### Local-First Design
- All task data is read from local SQLite database for fast UI rendering
- Firebase sync happens in background, pulling task definitions from Firestore
- Score is stored locally and synchronized with Firebase once daily using epoch day tracking

### AI Evaluation
- Photos are downscaled to max 480px dimension and compressed to 70% JPEG quality
- Evaluation happens on a background thread to avoid blocking UI
- WellnestAiClient receives task description and both images to determine pass/fail

### Character Evolution
The puffin character evolves based on total score:
- **Baby Puffin**: 0-499 points
- **Teen Puffin**: 500-999 points  
- **Adult Puffin**: 1000-1499 points
- **Senior Puffin**: 1500+ points

### Threading Model
- UI operations: Main thread
- Database operations: Called synchronously from ViewModel - consider moving to background
- Firebase operations: Use `Tasks.await` for blocking calls - must be on background thread
- AI evaluation: Explicit background thread with `runOnUiThread` callback