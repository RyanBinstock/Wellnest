# ActivityJar Micro App - Sequence Diagrams

## Overview

ActivityJar is an AI-powered activity suggestion micro app within the Wellnest Android application. It generates personalized activity recommendations across five categories (Explore, Nightlife, Play, Cozy, Culture) using AI and local context like weather and location. Users browse activities in a carousel, view details, and "accept" activities to earn points that contribute to their overall score.

### Architecture Components

| Layer | Component | Description |
|-------|-----------|-------------|
| **UI** | `ActivityJarFragment` | Launcher fragment that starts ActivityJarActivity |
| **UI** | `ActivityJarActivity` | Main screen with category buttons and score display |
| **UI** | `activityJarSelection` | Fragment showing activities carousel for selected category |
| **UI** | `ActivityCarouselAdapter` | RecyclerView adapter for activity cards |
| **UI** | `ActivityInfoDialog` | Dialog showing activity details with Accept button |
| **UI** | `FiltersBottomSheetDialog` | Bottom sheet for Friends/Solo/Family filters |
| **ViewModel** | `ActivityJarViewModel` | Manages activities map, loading state, errors, and score |
| **Repository** | `ActivityJarRepository` | Orchestrates local SQLite and remote Firebase operations |
| **Local** | `ActivityJarManager` | SQLite score operations |
| **Local** | `ActivityJarCacheManager` | SQLite cache for AI-generated activities |
| **External** | `WellnestAiClient` | AI service for generating activity suggestions |
| **Utility** | `ActivityJarPrefetcher` | Background prefetching with time-window logic |

---

## 1. App Startup - Opening ActivityJar

This diagram shows the flow when a user opens the ActivityJar micro app from the home screen.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant HomeFragment
    participant ActivityJarFragment
    participant ActivityJarActivity
    participant ActivityJarViewModel
    participant ActivityJarRepository
    participant ActivityJarManager
    participant SQLiteDB as SQLite Database

    User->>HomeFragment: Taps ActivityJar card
    HomeFragment->>ActivityJarFragment: Navigate via NavController
    ActivityJarFragment->>ActivityJarActivity: startActivity with Intent
    ActivityJarFragment->>HomeFragment: navigateUp - remove from back stack
    
    ActivityJarActivity->>ActivityJarActivity: onCreate
    ActivityJarActivity->>ActivityJarActivity: setContentView activity_activity_jar
    ActivityJarActivity->>ActivityJarActivity: Setup edge-to-edge and immersive UI
    ActivityJarActivity->>ActivityJarViewModel: ViewModelProvider.get
    
    Note over ActivityJarViewModel: Constructor initializes dependencies
    ActivityJarViewModel->>ActivityJarManager: new ActivityJarManager with db
    ActivityJarManager->>SQLiteDB: ensureSingletonRows for score table
    ActivityJarViewModel->>ActivityJarRepository: new ActivityJarRepository
    
    ActivityJarViewModel->>ActivityJarViewModel: loadScore
    ActivityJarViewModel->>ActivityJarRepository: getScore with callback
    ActivityJarRepository->>ActivityJarManager: getActivityJarScore
    ActivityJarManager->>SQLiteDB: SELECT score FROM activity_jar_score WHERE id=1
    SQLiteDB-->>ActivityJarManager: Score value
    ActivityJarManager-->>ActivityJarRepository: Score integer
    ActivityJarRepository-->>ActivityJarViewModel: onScoreUpdated callback
    ActivityJarViewModel-->>ActivityJarActivity: LiveData score update
    
    ActivityJarActivity->>ActivityJarActivity: updateScoreWithAnimation
    ActivityJarActivity->>ActivityJarActivity: Setup category button listeners
    ActivityJarActivity-->>User: Display main screen with categories
```

---

## 2. Category Selection and Activity Browsing

This diagram shows the flow when a user selects a category and browses activities.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant ActivityJarActivity
    participant activityJarSelection
    participant ActivityJarViewModel
    participant ActivityJarCacheManager
    participant WellnestAiClient
    participant SQLiteDB as SQLite Database
    participant ActivityCarouselAdapter
    participant WellnestCarouselView

    User->>ActivityJarActivity: Taps category button - e.g. Explore
    ActivityJarActivity->>activityJarSelection: FragmentTransaction replace with startIndex=0
    
    activityJarSelection->>activityJarSelection: onCreateView
    activityJarSelection->>ActivityJarViewModel: ViewModelProvider.get from requireActivity
    activityJarSelection->>ActivityCarouselAdapter: new adapter with click listener
    activityJarSelection->>WellnestCarouselView: setAdapter
    
    activityJarSelection->>ActivityJarViewModel: Observe getActivities LiveData
    activityJarSelection->>ActivityJarViewModel: Observe getIsLoading LiveData
    activityJarSelection->>ActivityJarViewModel: Observe getError LiveData
    
    activityJarSelection->>ActivityJarViewModel: loadActivities
    
    alt Activities already loaded in ViewModel
        ActivityJarViewModel-->>activityJarSelection: Return cached activities map
    else Need to load activities
        ActivityJarViewModel->>ActivityJarViewModel: isLoading.setValue true
        ActivityJarViewModel->>ActivityJarCacheManager: hasValidCache
        ActivityJarCacheManager->>SQLiteDB: SELECT COUNT from activity_jar_cache
        SQLiteDB-->>ActivityJarCacheManager: count
        
        alt Valid cache exists
            ActivityJarCacheManager-->>ActivityJarViewModel: true
            ActivityJarViewModel->>ActivityJarCacheManager: getCachedData
            ActivityJarCacheManager->>SQLiteDB: SELECT json_data, weather_summary, timestamp
            SQLiteDB-->>ActivityJarCacheManager: CacheEntry
            ActivityJarCacheManager-->>ActivityJarViewModel: CacheEntry with JSON
            ActivityJarViewModel->>ActivityJarViewModel: parseActivitiesFromJson
            ActivityJarViewModel-->>activityJarSelection: activities.postValue with map
        else No cache or invalid
            ActivityJarViewModel->>WellnestAiClient: planThingsToDo with Context
            Note over WellnestAiClient: Fetches weather and location, calls AI API
            WellnestAiClient-->>ActivityJarViewModel: Map of Category to Activity list
            ActivityJarViewModel->>ActivityJarViewModel: serializeActivitiesToJson
            ActivityJarViewModel->>ActivityJarCacheManager: saveCache with JSON and summary
            ActivityJarCacheManager->>SQLiteDB: DELETE FROM activity_jar_cache
            ActivityJarCacheManager->>SQLiteDB: INSERT INTO activity_jar_cache
            ActivityJarViewModel-->>activityJarSelection: activities.postValue with map
        end
        ActivityJarViewModel->>ActivityJarViewModel: isLoading.postValue false
    end
    
    activityJarSelection->>activityJarSelection: getCategoryByIndex - Explore
    activityJarSelection->>activityJarSelection: Filter activities for Explore category
    activityJarSelection->>ActivityCarouselAdapter: setActivities with filtered list
    ActivityCarouselAdapter->>WellnestCarouselView: notifyDataSetChanged
    activityJarSelection-->>User: Display activity carousel
```

---

## 3. Viewing Activity Details and Accepting

This diagram shows the complete flow when a user views an activity's details and accepts it.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant WellnestCarouselView
    participant ActivityCarouselAdapter
    participant activityJarSelection
    participant ActivityInfoDialog
    participant ActivityJarViewModel
    participant ActivityJarRepository
    participant ActivityJarManager
    participant ActivityJarCacheManager
    participant SQLiteDB as SQLite Database

    User->>WellnestCarouselView: Swipes through carousel
    User->>ActivityCarouselAdapter: Taps activity card
    ActivityCarouselAdapter->>activityJarSelection: onActivityClick callback
    
    activityJarSelection->>ActivityInfoDialog: newInstance with Activity data
    activityJarSelection->>ActivityInfoDialog: setOnActivityAcceptListener
    activityJarSelection->>ActivityInfoDialog: show via ChildFragmentManager
    
    ActivityInfoDialog->>ActivityInfoDialog: onCreateView
    ActivityInfoDialog->>ActivityInfoDialog: Display emoji, title, description, address, tags
    ActivityInfoDialog-->>User: Show activity details dialog
    
    User->>ActivityInfoDialog: Taps Accept button
    ActivityInfoDialog->>activityJarSelection: onActivityAccepted callback
    
    activityJarSelection->>ActivityJarViewModel: acceptActivity with Activity
    
    Note over ActivityJarViewModel: Two operations: add score and remove activity
    
    par Add Score
        ActivityJarViewModel->>ActivityJarRepository: addScore 50 points with callback
        ActivityJarRepository->>ActivityJarManager: addToActivityJarScore 50
        ActivityJarManager->>SQLiteDB: BEGIN TRANSACTION
        ActivityJarManager->>SQLiteDB: UPDATE activity_jar_score SET score=score+50
        ActivityJarManager->>SQLiteDB: SELECT score WHERE id=1
        SQLiteDB-->>ActivityJarManager: new score value
        ActivityJarManager->>SQLiteDB: COMMIT
        ActivityJarManager-->>ActivityJarRepository: new score
        ActivityJarRepository-->>ActivityJarViewModel: onScoreUpdated callback
        ActivityJarViewModel-->>ActivityJarActivity: score LiveData update
    and Remove Activity from List
        ActivityJarViewModel->>ActivityJarViewModel: removeActivity
        ActivityJarViewModel->>ActivityJarViewModel: Remove from activities map
        ActivityJarViewModel->>ActivityJarViewModel: activities.postValue updated map
        ActivityJarViewModel->>ActivityJarCacheManager: saveCache with updated JSON
        ActivityJarCacheManager->>SQLiteDB: DELETE and INSERT updated cache
    end
    
    ActivityInfoDialog->>ActivityInfoDialog: dismiss
    activityJarSelection->>User: Toast - Accepted! +50 points
    
    ActivityCarouselAdapter->>ActivityCarouselAdapter: Data updated via observer
    WellnestCarouselView-->>User: Activity removed from carousel
```

---

## 4. Random Activity Selection - Dice Feature

This diagram shows the flow when a user taps the dice button to get a random activity.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant activityJarSelection
    participant ActivityCarouselAdapter
    participant WellnestCarouselView
    participant ActivityInfoDialog
    participant ActivityJarViewModel

    User->>activityJarSelection: Taps dice button btnRandomDice
    activityJarSelection->>ActivityCarouselAdapter: getItemCount
    ActivityCarouselAdapter-->>activityJarSelection: count of activities
    
    alt Count is zero
        activityJarSelection-->>User: No action - empty list
    else Has activities
        activityJarSelection->>activityJarSelection: Generate random index 0 to count-1
        activityJarSelection->>WellnestCarouselView: getViewPager.setCurrentItem randomIndex with smooth scroll
        WellnestCarouselView-->>User: Carousel scrolls to random activity
        
        activityJarSelection->>ActivityCarouselAdapter: getItem randomIndex
        ActivityCarouselAdapter-->>activityJarSelection: Activity object
        
        activityJarSelection->>ActivityInfoDialog: newInstance with random Activity
        activityJarSelection->>ActivityInfoDialog: setOnActivityAcceptListener
        activityJarSelection->>ActivityInfoDialog: show via ChildFragmentManager
        ActivityInfoDialog-->>User: Show random activity details
        
        Note over User,ActivityInfoDialog: User can accept or dismiss
    end
```

---

## 5. Filters Bottom Sheet

This diagram shows the filter selection flow.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant activityJarSelection
    participant FiltersBottomSheetDialog
    
    User->>activityJarSelection: Taps filter button btnFilters
    activityJarSelection->>FiltersBottomSheetDialog: new instance
    activityJarSelection->>FiltersBottomSheetDialog: show via ChildFragmentManager
    
    FiltersBottomSheetDialog->>FiltersBottomSheetDialog: onCreateView
    FiltersBottomSheetDialog->>FiltersBottomSheetDialog: Setup filter options - Friends, Solo, Family
    FiltersBottomSheetDialog->>FiltersBottomSheetDialog: Default selection is Solo
    FiltersBottomSheetDialog-->>User: Display bottom sheet with filter options
    
    User->>FiltersBottomSheetDialog: Taps filter option - e.g. Friends
    FiltersBottomSheetDialog->>FiltersBottomSheetDialog: current = FRIENDS
    FiltersBottomSheetDialog->>FiltersBottomSheetDialog: updateUi - highlight Friends icon
    FiltersBottomSheetDialog->>FiltersBottomSheetDialog: Update description text
    FiltersBottomSheetDialog-->>User: Visual feedback on selection
    
    User->>FiltersBottomSheetDialog: Taps Apply Filters button
    Note over FiltersBottomSheetDialog: TODO - send selection to ViewModel
    FiltersBottomSheetDialog->>FiltersBottomSheetDialog: dismiss
```

---

## 6. Background Prefetching

This diagram shows the background prefetching mechanism that runs to ensure activities are cached ahead of time.

```mermaid
sequenceDiagram
    autonumber
    participant BackgroundThread
    participant ActivityJarPrefetcher
    participant WellnestDatabaseHelper
    participant ActivityJarCacheManager
    participant WellnestAiClient
    participant SQLiteDB as SQLite Database

    BackgroundThread->>ActivityJarPrefetcher: prefetchActivities with Context
    ActivityJarPrefetcher->>WellnestDatabaseHelper: getWritableDatabase
    WellnestDatabaseHelper-->>ActivityJarPrefetcher: SQLiteDatabase
    ActivityJarPrefetcher->>ActivityJarCacheManager: new CacheManager with db
    
    ActivityJarPrefetcher->>ActivityJarPrefetcher: shouldPrefetch
    ActivityJarPrefetcher->>ActivityJarCacheManager: hasValidCache
    ActivityJarCacheManager->>SQLiteDB: SELECT COUNT from activity_jar_cache
    SQLiteDB-->>ActivityJarCacheManager: count
    
    alt No valid cache
        ActivityJarCacheManager-->>ActivityJarPrefetcher: false - should prefetch
    else Has cache - check time window
        ActivityJarPrefetcher->>ActivityJarCacheManager: getCachedData
        ActivityJarCacheManager->>SQLiteDB: SELECT timestamp
        SQLiteDB-->>ActivityJarCacheManager: CacheEntry with timestamp
        ActivityJarPrefetcher->>ActivityJarPrefetcher: isSameTimeWindow cache vs now
        
        Note over ActivityJarPrefetcher: Time Windows: Morning 8-13, Afternoon 13-18, Evening 18-21
        
        alt Same time window
            ActivityJarPrefetcher-->>BackgroundThread: Cache valid - skip prefetch
        else Different time window or different day
            ActivityJarPrefetcher->>ActivityJarPrefetcher: Should prefetch
        end
    end
    
    opt Should prefetch
        loop Retry up to 3 times with backoff
            ActivityJarPrefetcher->>WellnestAiClient: planThingsToDo with Context
            alt Success
                WellnestAiClient-->>ActivityJarPrefetcher: Map of activities by category
            else Failure
                ActivityJarPrefetcher->>ActivityJarPrefetcher: Wait 2s * retryCount
            end
        end
        
        alt Got activities
            ActivityJarPrefetcher->>ActivityJarPrefetcher: serializeActivitiesToJson
            ActivityJarPrefetcher->>ActivityJarCacheManager: saveCache with JSON
            ActivityJarCacheManager->>SQLiteDB: DELETE FROM activity_jar_cache
            ActivityJarCacheManager->>SQLiteDB: INSERT new cache entry
            ActivityJarPrefetcher-->>BackgroundThread: Prefetch complete
        else All retries failed
            ActivityJarPrefetcher-->>BackgroundThread: Prefetch failed - log error
        end
    end
```

---

## 7. Score Synchronization - Local to Firebase

This diagram shows the once-daily score synchronization between local SQLite and Firebase Firestore.

```mermaid
sequenceDiagram
    autonumber
    participant ActivityJarRepository
    participant SharedPreferences
    participant ActivityJarManager
    participant SQLiteDB as SQLite Database
    participant FirebaseUserManager
    participant Firestore

    ActivityJarRepository->>ActivityJarRepository: syncActivityJarScoreOnceDaily called
    
    alt Legacy constructor - no context
        ActivityJarRepository-->>ActivityJarRepository: Log warning and return
    end
    
    ActivityJarRepository->>SharedPreferences: Get uid from user_repo_prefs
    
    alt uid is null or empty
        ActivityJarRepository-->>ActivityJarRepository: Log warning and return
    end
    
    ActivityJarRepository->>ActivityJarRepository: Calculate todayEpochDay
    ActivityJarRepository->>SharedPreferences: Get last_sync_activity_jar_score_epoch_day_uid
    
    alt lastEpochDay == todayEpochDay
        ActivityJarRepository-->>ActivityJarRepository: Already synced today - return
    end
    
    ActivityJarRepository->>ActivityJarManager: getActivityJarScore
    ActivityJarManager->>SQLiteDB: SELECT score FROM activity_jar_score WHERE id=1
    SQLiteDB-->>ActivityJarManager: localScore
    ActivityJarManager-->>ActivityJarRepository: localScore
    
    ActivityJarRepository->>FirebaseUserManager: getActivityJarScore with uid
    FirebaseUserManager->>Firestore: users/uid/microapp_scores/activity_jar.get
    Firestore-->>FirebaseUserManager: DocumentSnapshot
    FirebaseUserManager-->>ActivityJarRepository: ActivityJarScore with remoteScore
    
    ActivityJarRepository->>ActivityJarRepository: finalScore = max of localScore and remoteScore
    
    alt finalScore != localScore
        ActivityJarRepository->>ActivityJarManager: upsertRoamioScore with finalScore
        ActivityJarManager->>SQLiteDB: UPDATE activity_jar_score SET score=finalScore
    end
    
    alt remoteScore is null OR finalScore != remoteScore
        ActivityJarRepository->>FirebaseUserManager: upsertActivityJarScore with ActivityJarScore
        FirebaseUserManager->>Firestore: users/uid/microapp_scores/activity_jar.set
    end
    
    ActivityJarRepository->>SharedPreferences: Save todayEpochDay as last sync date
```

---

## Key Interactions Notes

### Data Flow Pattern
The ActivityJar micro app follows a clean MVVM architecture:
1. **UI Layer** → Fragments and Dialogs handle user interactions and display
2. **ViewModel Layer** → Exposes LiveData for activities, loading state, errors, and score
3. **Repository Layer** → Orchestrates between local SQLite and remote Firebase
4. **Data Layer** → Separate managers for score (ActivityJarManager) and cache (ActivityJarCacheManager)

### AI-Powered Activity Generation
- Activities are generated by `WellnestAiClient.planThingsToDo()` using AI
- AI considers local context like weather and location
- Results are cached in SQLite to avoid repeated API calls
- Activities are organized by 5 categories: Explore, Nightlife, Play, Cozy, Culture

### Caching Strategy
- Activities are cached as JSON in `activity_jar_cache` table
- Cache includes timestamp and weather summary
- Time-window based invalidation: Morning (8-13), Afternoon (13-18), Evening (18-21)
- Prefetcher runs in background to ensure fresh activities

### Score System
- Users earn +50 points per accepted activity
- Score stored locally in singleton row (id=1) in `activity_jar_score` table
- Score synced to Firebase once daily using epoch day tracking
- "Never decrease" strategy: max(local, remote) wins during sync

### Category System
Activities are organized into 5 categories mapped by index:
- **0**: Explore - Outdoor adventures and discovery
- **1**: Nightlife - Evening entertainment
- **2**: Play - Games and recreational activities
- **3**: Cozy - Relaxing indoor activities
- **4**: Culture - Arts, museums, cultural experiences

### Filter System (Partial Implementation)
- FiltersBottomSheetDialog provides Friends/Solo/Family filter options
- Currently UI-only; filter application to ViewModel/Repository is TODO

### Threading Model
- UI operations: Main thread
- Activity loading: Background ExecutorService in ViewModel
- Cache operations: Same background thread
- AI API calls: Background thread (WellnestAiClient handles internally)
- Score operations: Background ExecutorService in Repository
- Prefetching: Explicit background thread required

### Key UI Components
- **WellnestCarouselView**: Custom ViewPager2 wrapper for smooth carousel experience
- **ActivityCarouselAdapter**: Binds activity data to carousel cards
- **ActivityInfoDialog**: Full-featured dialog with emoji, title, description, address, tags