# Wellnest

Android wellness app with modular micro‑apps: **SnapTask**, **ActivityJar**, and **Roamio**. This README covers workflow, structure, and code standards so you can ship confidently.


## 1) Git Workflow

We use a simplified **Git Flow**:

* **master** → production/stable, protected. Only fast‑forward merges from `development` via release PR.
* **development** → integration branch for tested features. Default base for PRs.
* **documentation** → all SE docs (UML, ERDs, user flows, reports). Keep this branch independent from app code.

### Branch naming (industry standard)

Use lowercase with hyphens. Prefer a scope prefix so branches are easy to scan.

```
feature/<scope>-<short-desc>
bugfix/<area>-<short-desc>
hotfix/<area>-<short-desc>
refactor/<area>-<short-desc>
docs/<topic>
chore/<area>-<short-desc>
```

**Scope** examples: `snaptask`, `activityjar`, `roamio`, `ui`, `auth`, `room`, `nav`, `build`.

> Examples
>
> * `feature/snaptask-swipe-complete`
> * `bugfix/room-migration-crash`
> * `hotfix/auth-nullpointer`
> * `docs/er-diagrams-v2`

### Commit messages (Conventional Commits)

```
feat(ui): add MicroAppCardFragment ripple
fix(room): correct foreign key on CompletionTag
refactor(auth): extract repository interface
chore(build): bump AGP to 8.13.0
```

Keep commits small and focused. Avoid “mega” commits; small commits make revert and blame easy.

### PR rules

* Open PRs into `development`.

* Add a 1–2 sentence summary, screenshots for UI changes, and a checklist of tests run.

* Releases: open a PR from `development → master`, tag `vX.Y.Z` on merge.


## 2) Documentation Branch

All course deliverables and SE artifacts live in **`documentation`**:

* Class, ER, user‑flow diagrams
* Reports and appendices
* Meeting notes & decisions log


## 3) Project Structure (where things live)

**Module:** `app/` (single‑module project; Kotlin Gradle **.kts** build files). Key directories:

```
app/
├─ build.gradle.kts            # Android app module config
├─ google-services.json        # Firebase config
└─ src/
   ├─ main/
   │  ├─ AndroidManifest.xml
   │  ├─ java/com/code/wlu/cp470/wellnest/
   │  │  ├─ MainActivity.java                    # Single-activity entry point
   │  │  ├─ data/                                # Data layer
   │  │  │  ├─ auth/                             # AuthRepository, sign-in/out, etc.
   │  │  │  ├─ local/
   │  │  │  |  ├─ contracts/                     # Contracts for the User and Micro App entity groups
   │  │  │  |  ├─ managers/                      # Managers for the User and Micro App entity groups
   │  │  │  |  └─ WellnestDatabaseHelper.java    # SQLite database helper file;
   │  │  │  ├─ <interfaces>                      # Interfaces for managers goes in `data/`
   │  │  │  ├─ <repositories>                    # Repositories for managers goes in `data/`
   │  │  │  └─ WellnestApp.java                  # Application class;
   │  │  ├─ ui/                                  # **All visual Fragments & UI components**
   │  │  │  ├─ home/                             # HomeFragment, MicroAppCardFragment
   │  │  │  ├─ auth/                             # AuthFragment (login/register)
   │  │  │  ├─ welcome/                          # Welcome/onboarding screens
   │  │  │  ├─ snaptask/                         # SnapTask micro‑app (Activity‑based: SnapTaskActivity, SnapTaskDetailActivity; SnapTaskFragment = thin launcher)
   │  │  │  ├─ activityjar/                      # ActivityJarFragment (micro‑app)
   │  │  │  ├─ roamio/                           # RoamioFragment (micro‑app)
   │  │  │  └─ effects/                          # UI helper effects (touch, text, shake, click)
   │  │  └─ viewmodel/                           # ViewModels
   │  └─ res/
   │     ├─ layout/                              # XML layouts (activity_main, fragment_*.xml)
   │     ├─ navigation/nav_graph.xml             # Navigation graph (destinations & actions)
   │     ├─ values/                              # colors.xml, dimens.xml, strings.xml, styles.xml, themes.xml
   │     ├─ drawable/                            # Shapes, icons, backgrounds
   │     ├─ font/                                # Typeface resources
   │     └─ xml/                                 # backup_rules.xml, data_extraction_rules.xml
   ├─ test/java/...                              # Unit tests (JVM)
   └─ androidTest/java/...                       # Instrumented tests (Espresso, SQLite, etc.)
```

### App Architecture (Hybrid)

- Main app uses a single-activity, Fragment-based navigation model.
- Micro apps use an Activity-based architecture for modularity and isolation.
- SnapTask is Activity-based:
  - [SnapTaskActivity](app/src/main/java/com/code/wlu/cp470/wellnest/ui/snaptask/SnapTaskActivity.java)
  - [SnapTaskDetailActivity](app/src/main/java/com/code/wlu/cp470/wellnest/ui/snaptask/SnapTaskDetailActivity.java)
  - [SnapTaskFragment](app/src/main/java/com/code/wlu/cp470/wellnest/ui/snaptask/SnapTaskFragment.java) acts as a thin launcher stub for nav compatibility.

### Micro Apps: SnapTask

- Intent-based navigation with `Bundle` extras between Activities
- Custom transition animations for smooth UI flow
- Proper Activity lifecycle management and state preservation
- ViewModel pattern maintained for data persistence
- Activities registered in [AndroidManifest.xml](app/src/main/AndroidManifest.xml)

#### Technical implementation details

- Launcher stub pattern: `Fragment` launches the micro app `Activity` via `Intent`
- Back stack handling integrates cleanly with the main app
- Memory optimized for Activity-based flow

#### Developer guide: micro apps

- New micro apps should follow the Activity-based pattern used by SnapTask.
- Use SnapTask as a reference implementation when adding future micro apps.

### Where to put new code

* **New screens / components** → Main app: `Fragment` + XML under `ui/<feature>/`. Micro apps (e.g., SnapTask): Activity‑based under `ui/<microapp>/` with `Activity` screens and an optional thin launcher `Fragment` for navigation compatibility.
* **Shared UI helpers** → `ui/effects/` or create a `ui/components/` package for reusable views.
* **Navigation** → For in‑app fragments, add destinations/actions in `res/navigation/nav_graph.xml` (SafeArgs). For micro apps, use Intent‑based navigation between `Activity` screens with `Bundle` extras.
* **Database** → add schemas and sql queries to `data/local/contracts/{relavent contract class}`  and create data manipulation methods in `data/local/managers/{relavent manager class}`.
* **Auth / remote** → SignIn and SignUp logic is in `data/auth/AuthRepository.java`.
* **ViewModels** → `viewmodel/` (one per screen or feature).
* **Resources** → images in `drawable/`, strings in `values/strings.xml`, style tokens in `values/styles.xml` & `themes.xml`.

## 4) Backend Architecture

The **Wellnest** backend is built on a **two-tiered architecture** consisting of:
- A **local SQLite database** for offline persistence.
- A **remote Firestore database** for cloud synchronization and cross-device access.

To simplify backend complexity for the frontend layer, we use **Manager classes** that handle all query logic and expose clean, high-level methods for data access and manipulation. This allows UI components to interact with data through a consistent, well-defined interface rather than dealing directly with database APIs.

### Local Layer
Our **local database schemas** are defined in dedicated **Contract classes**, which serve as the single source of truth for table and column definitions. These contracts ensure consistency across DAOs, helpers, and repositories.

### Local ↔ Remote Integration
To ensure seamless operation between the local and remote data layers, two key challenges must be addressed:

1. **Standardization:** Manager classes for local and remote data must share the same method signatures and return types.
2. **Synchronization:** Data changes must propagate safely between local and remote databases to keep the app state consistent.

We address these challenges through the following design patterns:

1. **Interfaces**  
   Each data domain (e.g., `User`, `SnapTask`, `ActivityJar`) defines an interface that prescribes the available methods and their return shapes.  
   - Local and remote Manager classes implement the same interface.  
   - The frontend can therefore consume the data agnostically without needing to know whether it’s sourced from SQLite or Firestore.

2. **Repositories**  
   Repositories act as synchronization coordinators between local and remote sources. They:  
   - Reconcile updates in both directions.  
   - Decide when to prioritize local vs. remote data.  
   - Handle network availability gracefully.

## 5) Build & Run

**Requirements**

* Android Studio (Koala/Koala+)
* JDK 17 (AGP 8+ requires 17)
* Android SDK 36 (compileSdk = 36), minSdk = 26

**Steps**

1. Open the project root in Android Studio.
2. Sync Gradle. If protobuf fails on first sync, re‑sync once (the plugin generates lite Java classes).
3. Run `app`. Launch target is `MainActivity`.

**Gradle tips**

* JVM unit tests: `./gradlew testDebugUnitTest`
* Instrumented tests: `./gradlew connectedDebugAndroidTest`
* Lint: `./gradlew lint`  (treat warnings as warnings; fix critical issues before merge)

## 6) Testing Policy

* **Before merging to `development`**: your feature should have passing tests or a clear note why it’s temporarily exempt.
* **Allowed rare exception**: Non‑critical, time‑sensitive fixes may merge **untested** if you ran the code on an emulator/device and verified behavior. Follow up with tests ASAP.
* **Before merging `development`** → `master` **:** feature and regression tests must pass; run instrumented tests on at least 1 phone & 1 tablet emulator.

**Test locations**

* Unit tests → `app/src/test/java/...`
* Instrumented (Espresso/Room) → `app/src/androidTest/java/...`

## 7) Code Quality Standards

### Must‑do items

* **Avoid duplication**: extract helpers, adapters, and styles; DRY layouts using `<include>` and style refs.
* **Prefer reusable fragments/components** for any UI used more than once or that’s complex.
* **Do not hard‑style in layout XML**: use `styles.xml` and theme attributes. Keep colors & dims in `values/`.
* **Strings**: all user‑visible text must come from `values/strings.xml`.

  * Provide translations in our second language (Canadian) via `res/values/strings.xml(en-rCA)` .
  * In Android Studio, hover string literal → lightbulb → **Extract string resource**.
* **UI package layout**: place visual fragments under `/ui/<feature>/`. Small, page‑specific components can live beside the page; shared ones go in `ui/components/` or `ui/effects/`.
* **Annotation & comments**: annotate nullability (`@NonNull/@Nullable`) and document public methods/classes with Javadoc. Add usage notes where it helps other devs.

### General guidance

* Follow Material 3; prefer ConstraintLayout. Keep view hierarchies shallow.
* Use Navigation SafeArgs for fragment‑to‑fragment arguments; for micro apps use Intent `Bundle` extras between `Activity` screens.
* Keep ViewModels UI‑only; data work goes in repositories/DAOs.
* One responsibility per class/file; keep files readable (<300–400 lines is a good heuristic).

## 8) Lint, Style & Formatting

* Use Android Studio’s **Code Cleanup** and **Reformat** before committing.
* Enable “Optimize imports on the fly”.
* No trailing `TODO`s in merged code.

## 9) Security & Config

* Do **not** commit real API keys or secrets. `google-services.json` for class demo is fine, but rotate keys for public builds.
* Keep any future secrets in `local.properties` or an untracked `.env`.

## 10) Release Checklist

* Version bump in `build.gradle.kts` (versionCode/versionName).
* Changelog update in PR description.
* All tests green and manual smoke on emulator.
* Merge `development` → `master`, create tag `vX.Y.Z`.

## 11) FAQs

* **Where do I add a new micro‑app?**
  * Prefer Activity‑based micro apps. Create a package under `ui/<microapp>/` with one or more `Activity` screens; add an optional launcher `Fragment` for navigation compatibility; start via `Intent` from `HomeFragment`/cards; register Activities in `AndroidManifest.xml`.
  * Use SnapTask as the reference implementation.

* **Where are the database tables?**
  * SQLite database schemas and SQL queries are under `data/local/contracts/`.
 
*  **How do I query the database?**
   *  Instead of writing SQL queries on the frontend, data handelling has been abstracted into backend contracts and managers.

For the majority of your data handelling needs, there already exists a method in the manager classes to do so. Here is an example of how you may use these methods.
```java
// First you must instantiate the database helper object and open a database session
WellnestDatabaseHelper dbHelper = new WellnestDatabaseHelper(context);
SQLiteDatabase db = dbHelper.getWritableDatabase();

// Now instantiate the manager associated with your task.
// For example if you want to get all the users friends,
//     you should use the UserManager since what you're querying for relates to the User.

// You can also check the ER and Class Diagram documentation to see a list of all the methods each manager contains.
UserManager userManager = new UserManager(db); // <--- instantiating the UserManager class

// Now you can run any queries by simply calling the relavent method
List<Friend> friends = userManager.getFriends();
```
If the query you need does not already exist in any manager class, you should create it!
To do so you should update the relavent interface by adding the new method, then implement it in the manager.
Now you can use your query by following the steps laid out in the code block above.

### Maintainers

* Default branch owner: `development`
* Release owner: `master`
* Docs owner: `documentation`

Happy building! 🚀
