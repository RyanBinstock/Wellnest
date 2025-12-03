# Roamio Micro App - Sequence Diagrams

## Overview

Roamio is an AI-powered walking adventure micro app within the Wellnest Android application. It generates personalized walking recommendations based on the user's location, weather conditions, and local points of interest. Users start walks with Google Maps navigation, then verify completion by being within 50 meters of the destination. Points are awarded based on walk difficulty, contributing to character evolution.

### Architecture Components

| Layer | Component | Description |
|-------|-----------|-------------|
| **UI** | `RoamioFragment` | Launcher fragment that starts RoamioActivity |
| **UI** | `RoamioActivity` | Main screen with walk display, maps integration, location verification |
| **ViewModel** | `RoamioViewModel` | Manages score and async walk generation with callbacks |
| **Repository** | `RoamioRepository` | Orchestrates local/remote data and AI walk generation |
| **Local** | `RoamioManager` | SQLite database operations for score |
| **Remote** | `FirebaseRoamioManager` | Firestore score synchronization |
| **External** | `WellnestAiClient` | AI-powered walk generation with location and weather |
| **External** | `Google Maps` | Navigation to walk destination |
| **External** | `FusedLocationProvider` | GPS location services |

---

## 1. App Startup - Opening Roamio

This diagram shows the flow when a user opens the Roamio micro app from the home screen.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant HomeFragment
    participant RoamioFragment
    participant RoamioActivity
    participant RoamioViewModel
    participant RoamioRepository
    participant RoamioManager
    participant SQLiteDB as SQLite Database

    User->>HomeFragment: Taps Roamio card
    HomeFragment->>RoamioFragment: Navigate via NavController
    RoamioFragment->>RoamioActivity: startActivity with Intent
    RoamioFragment->>HomeFragment: navigateUp - remove from back stack
    
    RoamioActivity->>RoamioActivity: onCreate
    RoamioActivity->>RoamioActivity: setContentView activity_roamio
    RoamioActivity->>RoamioActivity: Setup edge-to-edge and immersive UI
    
    Note over RoamioActivity: Initialize views and location client
    RoamioActivity->>RoamioActivity: Initialize FusedLocationProviderClient
    RoamioActivity->>RoamioActivity: Initialize loading overlay components
    
    RoamioActivity->>RoamioViewModel: new RoamioViewModel
    
    Note over RoamioViewModel: Constructor initializes dependencies
    RoamioViewModel->>RoamioManager: new RoamioManager with db
    RoamioManager->>SQLiteDB: ensureSingletonRows for roamio_score table
    RoamioViewModel->>RoamioRepository: new RoamioRepository
    
    RoamioActivity->>RoamioViewModel: getScore
    RoamioViewModel->>RoamioRepository: getRoamioScore
    RoamioRepository->>RoamioManager: getRoamioScore
    RoamioManager->>SQLiteDB: SELECT score FROM roamio_score WHERE id=1
    SQLiteDB-->>RoamioManager: Score value
    RoamioManager-->>RoamioActivity: RoamioScore object
    
    RoamioActivity->>RoamioActivity: Update score TextView
    RoamioActivity->>RoamioActivity: Set character image based on score
    
    RoamioActivity->>RoamioActivity: showLoadingOverlay with fade-in
    RoamioActivity->>RoamioViewModel: generateWalk with callback
    
    Note over RoamioViewModel: Async walk generation starts
    RoamioActivity-->>User: Display loading overlay with progress
```

---

## 2. Walk Generation via AI

This diagram shows the detailed AI-powered walk generation flow.

```mermaid
sequenceDiagram
    autonumber
    participant RoamioActivity
    participant RoamioViewModel
    participant ExecutorService
    participant RoamioRepository
    participant WellnestAiClient
    participant FusedLocation as FusedLocationProvider
    participant Geocoder
    participant OpenMeteo as Open-Meteo API
    participant TavilyProxy as Tavily Search Proxy
    participant OpenAIProxy as OpenAI Proxy

    RoamioActivity->>RoamioViewModel: generateWalk with callback
    RoamioViewModel->>ExecutorService: execute on background thread
    ExecutorService->>RoamioRepository: generateWalk with progress callback
    RoamioRepository->>WellnestAiClient: pickWalkAndStory with context and callback
    
    WellnestAiClient->>WellnestAiClient: Check location permissions
    WellnestAiClient-->>RoamioActivity: onProgress 5% - Checking permissions
    
    WellnestAiClient->>FusedLocation: getCurrentLocation HIGH_ACCURACY
    WellnestAiClient-->>RoamioActivity: onProgress 10% - Finding your location
    FusedLocation-->>WellnestAiClient: Location with lat/lng
    
    WellnestAiClient->>Geocoder: getFromLocation for reverse geocoding
    WellnestAiClient-->>RoamioActivity: onProgress 20% - Identifying neighborhood
    Geocoder-->>WellnestAiClient: Address with locality and admin area
    WellnestAiClient->>WellnestAiClient: Build location name string
    
    WellnestAiClient->>OpenMeteo: GET forecast with lat/lng
    WellnestAiClient-->>RoamioActivity: onProgress 30% - Checking weather conditions
    OpenMeteo-->>WellnestAiClient: Weather JSON with temperature, wind, code
    WellnestAiClient->>WellnestAiClient: Format weather summary string
    
    WellnestAiClient->>OpenAIProxy: askNanoForQuery - scenic walking spots
    WellnestAiClient-->>RoamioActivity: onProgress 40% - Searching for scenic spots
    OpenAIProxy-->>WellnestAiClient: Search query text
    
    WellnestAiClient->>TavilyProxy: tavilySearch with query
    TavilyProxy-->>WellnestAiClient: Search results JSON
    
    WellnestAiClient->>OpenAIProxy: askNanoForStrictJson - pick spot and story
    WellnestAiClient-->>RoamioActivity: onProgress 60% - Crafting your adventure
    OpenAIProxy-->>WellnestAiClient: JSON with name, addresses, story
    
    WellnestAiClient->>Geocoder: getFromLocationName for start_address
    WellnestAiClient-->>RoamioActivity: onProgress 80% - Finalizing details
    Geocoder-->>WellnestAiClient: Start coordinates
    
    WellnestAiClient->>Geocoder: getFromLocationName for end_address
    Geocoder-->>WellnestAiClient: End coordinates
    
    WellnestAiClient->>WellnestAiClient: calculateDistance between coordinates
    WellnestAiClient-->>RoamioActivity: onProgress 100% - Ready to explore
    
    WellnestAiClient-->>RoamioRepository: Walk object with all attributes
    RoamioRepository-->>RoamioViewModel: Walk object
    RoamioViewModel->>RoamioViewModel: mainHandler.post callback.onSuccess
    
    RoamioActivity->>RoamioActivity: hideLoadingOverlay with fade-out
    RoamioActivity->>RoamioActivity: Update walkTitle and walkDescription
    RoamioActivity->>RoamioActivity: calculateDifficulty from distance
    RoamioActivity->>RoamioActivity: updateDifficultyDisplay with dots
    RoamioActivity->>RoamioActivity: primaryButton.setText Start Walk
    RoamioActivity-->>User: Display walk details with Start Walk button
```

---

## 3. Starting a Walk - Maps Navigation

This diagram shows the flow when a user taps the Start Walk button.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant RoamioActivity
    participant GoogleMaps as Google Maps App
    participant Intent as Android Intent

    User->>RoamioActivity: Taps Start Walk button
    RoamioActivity->>RoamioActivity: handleWalkButtonClick
    
    alt currentWalk is null
        RoamioActivity->>RoamioActivity: Toast - No walk available
    else walkStarted is false
        RoamioActivity->>RoamioActivity: launchMapsNavigation
        
        RoamioActivity->>RoamioActivity: Get startAddress and endAddress from Walk
        RoamioActivity->>RoamioActivity: Build Google Maps directions URL
        Note over RoamioActivity: URL format: maps/dir/?api=1&origin=X&destination=Y&travelmode=walking
        
        RoamioActivity->>Intent: new Intent ACTION_VIEW with Maps URI
        RoamioActivity->>Intent: setPackage com.google.android.apps.maps
        
        alt Google Maps is installed
            RoamioActivity->>GoogleMaps: startActivity with maps intent
            GoogleMaps-->>User: Opens Maps with walking directions
        else Google Maps not installed
            RoamioActivity->>RoamioActivity: Build fallback geo: URI
            RoamioActivity->>Intent: startActivity with fallback intent
            Intent-->>User: Opens default maps app
        end
        
        RoamioActivity->>RoamioActivity: walkStarted = true
        RoamioActivity->>RoamioActivity: primaryButton.setText Finish Walk
        RoamioActivity-->>User: Button now shows Finish Walk
    end
```

---

## 4. Completing a Walk - Location Verification

This diagram shows the flow when a user taps Finish Walk and location is verified.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant RoamioActivity
    participant FusedLocation as FusedLocationProvider
    participant Geocoder
    participant RoamioViewModel
    participant RoamioRepository
    participant RoamioManager
    participant SQLiteDB as SQLite Database

    User->>RoamioActivity: Taps Finish Walk button
    RoamioActivity->>RoamioActivity: handleWalkButtonClick
    RoamioActivity->>RoamioActivity: checkLocationAndCompleteWalk
    
    RoamioActivity->>RoamioActivity: Check location permissions
    
    alt Permissions not granted
        RoamioActivity->>RoamioActivity: requestPermissions ACCESS_FINE_LOCATION
        RoamioActivity-->>User: Show permission dialog
    else Permissions granted
        RoamioActivity->>FusedLocation: getCurrentLocation HIGH_ACCURACY
        FusedLocation-->>RoamioActivity: Current Location object
        
        alt Location is null
            RoamioActivity-->>User: Toast - Unable to get current location
        else Location obtained
            RoamioActivity->>RoamioActivity: calculateDistanceToDestination
            RoamioActivity->>Geocoder: getFromLocationName for end_address
            Geocoder-->>RoamioActivity: Destination Address with lat/lng
            RoamioActivity->>RoamioActivity: Location.distanceBetween current vs destination
            
            alt Distance <= 50 meters
                RoamioActivity->>RoamioActivity: completeWalk
                RoamioActivity->>RoamioActivity: getPointsForDifficulty
                
                Note over RoamioActivity: Points: Easy=300, Medium=500, Hard=800
                
                RoamioActivity->>RoamioViewModel: addToScore with points
                RoamioViewModel->>RoamioRepository: addToRoamioScore with delta
                RoamioRepository->>RoamioManager: addToRoamioScore with delta
                RoamioManager->>SQLiteDB: BEGIN TRANSACTION
                RoamioManager->>SQLiteDB: UPDATE roamio_score SET score=score+delta WHERE id=1
                RoamioManager->>SQLiteDB: COMMIT
                
                RoamioActivity->>RoamioActivity: currentWalk.setCompleted true
                
                RoamioActivity->>RoamioViewModel: getScore
                RoamioViewModel-->>RoamioActivity: Updated RoamioScore
                RoamioActivity->>RoamioActivity: Update score TextView
                
                RoamioActivity-->>User: Toast - Walk completed! +N points
                RoamioActivity->>RoamioActivity: primaryButton.setText Walk Completed!
                RoamioActivity->>RoamioActivity: primaryButton.setEnabled false
            else Distance > 50 meters
                RoamioActivity-->>User: Toast - You are N meters from destination
            end
        end
    end
```

---

## 5. Score Synchronization - Local to Firebase

This diagram shows the once-daily score synchronization between local SQLite and Firebase Firestore.

```mermaid
sequenceDiagram
    autonumber
    participant RoamioViewModel
    participant ExecutorService
    participant RoamioRepository
    participant SharedPreferences
    participant RoamioManager
    participant SQLiteDB as SQLite Database
    participant FirebaseRoamioManager
    participant Firestore

    RoamioViewModel->>ExecutorService: execute syncScore on background
    ExecutorService->>RoamioRepository: syncRoamioScoreOnceDaily
    
    RoamioRepository->>SharedPreferences: Get uid from user_repo_prefs
    
    alt uid is null or empty
        RoamioRepository->>RoamioRepository: Log warning and return
    end
    
    RoamioRepository->>RoamioRepository: Calculate todayEpochDay
    RoamioRepository->>SharedPreferences: Get last_sync_roamio_score_epoch_day_uid
    
    alt lastEpochDay == todayEpochDay
        RoamioRepository->>RoamioRepository: Already synced today - return
    end
    
    RoamioRepository->>RoamioRepository: syncScoreInternal with uid
    
    RoamioRepository->>RoamioManager: getRoamioScore
    RoamioManager->>SQLiteDB: SELECT score FROM roamio_score WHERE id=1
    SQLiteDB-->>RoamioManager: localScore
    RoamioManager-->>RoamioRepository: RoamioScore with localScore
    
    RoamioRepository->>FirebaseRoamioManager: getScore with uid
    FirebaseRoamioManager->>Firestore: users/uid/microapp_scores/roamio.get
    Note over FirebaseRoamioManager: Uses Tasks.await for blocking call
    Firestore-->>FirebaseRoamioManager: DocumentSnapshot
    FirebaseRoamioManager-->>RoamioRepository: RoamioScore with remoteScore
    
    alt remoteScore > localScore
        RoamioRepository->>RoamioManager: upsertRoamioScore with remoteScore
        RoamioManager->>SQLiteDB: UPDATE roamio_score SET score=remoteScore WHERE id=1
    else localScore > remoteScore
        RoamioRepository->>FirebaseRoamioManager: upsertScore with localScore
        FirebaseRoamioManager->>Firestore: users/uid/microapp_scores/roamio.set
    else Scores are equal
        RoamioRepository->>RoamioRepository: No sync needed
    end
    
    RoamioRepository->>SharedPreferences: Save todayEpochDay as last sync date
```

---

## 6. Walk Generation Error Handling

This diagram shows error handling during walk generation.

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant RoamioActivity
    participant RoamioViewModel
    participant RoamioRepository
    participant WellnestAiClient

    RoamioActivity->>RoamioActivity: showLoadingOverlay
    RoamioActivity->>RoamioViewModel: generateWalk with callback
    
    alt Location permission denied
        WellnestAiClient-->>RoamioRepository: null - permission error
        RoamioRepository-->>RoamioViewModel: null
        RoamioViewModel->>RoamioViewModel: callback.onError - permissions message
    else Location unavailable
        WellnestAiClient-->>RoamioRepository: null - location error
        RoamioRepository-->>RoamioViewModel: null
        RoamioViewModel->>RoamioViewModel: callback.onError - location message
    else Geocoding failed
        WellnestAiClient-->>RoamioRepository: null - geocoding error
        RoamioRepository-->>RoamioViewModel: null
        RoamioViewModel->>RoamioViewModel: callback.onError - generic message
    else Network/API error
        WellnestAiClient-->>RoamioRepository: Exception thrown
        RoamioRepository-->>RoamioViewModel: Exception propagated
        RoamioViewModel->>RoamioViewModel: callback.onError with exception message
    end
    
    RoamioViewModel-->>RoamioActivity: onError callback on main thread
    RoamioActivity->>RoamioActivity: hideLoadingOverlay
    RoamioActivity->>RoamioActivity: walkTitle.setText Walk Generation Failed
    RoamioActivity->>RoamioActivity: walkDescription.setText error message
    RoamioActivity->>RoamioActivity: primaryButton.setEnabled false
    RoamioActivity-->>User: Display error state
```

---

## Key Interactions Notes

### Data Flow Pattern
The Roamio micro app follows a clean MVVM architecture:
1. **UI Layer** → Activity handles user interactions, maps launch, and location verification
2. **ViewModel Layer** → Exposes score and async walk generation with callbacks to UI
3. **Repository Layer** → Orchestrates local SQLite, remote Firebase, and AI generation
4. **Data Layer** → Separate managers for SQLite and Firestore operations
5. **AI Layer** → WellnestAiClient handles all external API calls

### AI-Powered Walk Generation
- Uses FusedLocationProviderClient for GPS location
- Reverse geocodes coordinates to human-readable location name
- Fetches weather from Open-Meteo API
- Generates search query via GPT-5-nano
- Searches for scenic spots via Tavily API
- Synthesizes results and generates story via GPT-5-nano
- Geocodes start/end addresses to calculate distance

### Point System Based on Difficulty
Walk difficulty is determined by distance:
- **Easy** (<1000m): 300 points
- **Medium** (1000-2500m): 500 points
- **Hard** (>2500m): 800 points

### Character Evolution
The puffin character evolves based on total score:
- **Baby Puffin**: 0-499 points
- **Teen Puffin**: 500-999 points
- **Adult Puffin**: 1000-1499 points
- **Senior Puffin**: 1500+ points

### Location Verification
- Uses 50-meter completion radius
- Geocodes destination address to coordinates
- Calculates distance using Location.distanceBetween
- Requires location permissions for verification

### Score Synchronization
- Once-daily sync using epoch day tracking
- Higher score wins strategy between local and remote
- Uses SharedPreferences to track last sync date per user

### Threading Model
- UI operations: Main thread
- Walk generation: Background ExecutorService with main thread callbacks
- Score operations: Called synchronously from ViewModel
- Firebase operations: Use Tasks.await for blocking calls - must be on background thread
- Progress callbacks: Posted to main thread via Handler

### External Dependencies
- **Google Play Services**: FusedLocationProviderClient for GPS
- **Google Maps**: Navigation via Intent
- **Android Geocoder**: Address-to-coordinates and reverse geocoding
- **Open-Meteo API**: Weather data (no authentication required)
- **Tavily API**: Web search for local points of interest (via Vercel proxy)
- **OpenAI API**: GPT-5-nano for query generation and story synthesis (via Vercel proxy)