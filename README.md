# ES-DE Companion (rebuild)

Ground-up rebuild of ES-DE Companion. See `CLAUDE.md` for the architecture, structure, and
coding standards this project follows — read that first.

## Current scope

The app is feature-complete across all layers for a first real-world install. What it does:

- **Log pipeline**: tails `es_log.txt` via `FileObserver` (with a 3-second fallback poll),
  parses `Scripting::fireEvent()` lines into typed `EsdeEvent`s, and reduces them into a
  single current `AppState` through a pure, Android-free `AppStateReducer`. Handles
  truncate-on-restart, the `game-select`/`game-start` race, and `screensaver-end`
  restoration via a `previousState` field.
- **Cold start**: reconstructs correct state on launch by replaying from the last
  self-contained ("anchor") event forward, not just the most recent line — see CLAUDE.md
  for why a naive last-event replay is insufficient.
- **Media resolution**: resolves a selected game/system into on-disk media
  (backdrops, logos, etc.) across all `MediaType`s under `downloaded_media`, including
  ES-DE's directory-as-file convention (`.m3u` multi-disc folders).
- **UI**: crossfading backdrop/logo display (`CrossfadeAsyncImage`), a toggleable debug
  `StateOverlay`, and a scrollable, categorized Settings screen.
- **App Drawer**: a full-screen overlay (not a nav destination) with spring-physics
  gesture handling, a lazy grid of installed apps with async icons, and double-tap launch
  to a secondary display.
- **Onboarding**: a multi-step first-run flow (storage permission → log folder → media
  folder), re-enterable later from Settings.
- **Home screen launcher**: registered as a `HOME`/`DEFAULT` launcher (`singleTask`,
  excluded from recents) for kiosk-style deployment on a secondary display.

See "Everything Else Is Iterative" in CLAUDE.md for what's still explicitly a sketch rather
than a finished feature.

## Project layout

```
app/src/main/java/com/esde/companion/
├── domain/                    # pure Kotlin, no Android imports — the state pipeline lives here
│   ├── model/                   EsdeEvent, AppState, EsdeConnectionState, GameMedia,
│   │                             LogFolderValidation, MediaFolderValidation, ThemePreference
│   ├── parser/                  EsdeEventParser, GameMediaPathResolver
│   ├── state/                   AppStateReducer — (AppState, EsdeEvent) -> AppState
│   ├── repository/              EsdeLogRepository, GameMediaRepository, SystemMediaRepository,
│   │                             InstalledAppsRepository, OnboardingRepository,
│   │                             AppDrawerSettingsRepository (interfaces only)
│   └── usecase/                 ObserveAppStateUseCase, ObserveConnectionStateUseCase,
│                                 ResolveGameMediaUseCase, ResolveRandomSystemFanartUseCase,
│                                 CompleteOnboardingUseCase, Validate*FolderUseCase,
│                                 ObserveInstalledAppsUseCase, App Drawer setting use cases
├── data/
│   ├── log/                     EsdeLogFileRepository, ReactiveEsdeLogRepository
│   ├── media/                   FileGameMediaRepository, ReactiveGameMediaRepository,
│   │                             ReactiveSystemMediaRepository
│   ├── apps/                    PackageManagerAppsRepository (installed apps + launch)
│   ├── settings/                FileOnboardingRepository, FileAppDrawerSettingsRepository
│   │                             (DataStore-backed)
│   └── storage/                 AllFilesAccessPermission
├── ui/
│   ├── MainActivity.kt          BackPressGate-aware back handling, HOME launcher entrypoint
│   ├── main/                    MainViewModel, MainScreen, StateOverlay, CrossfadeAsyncImage,
│   │                             display formatting
│   ├── drawer/                  App Drawer overlay, gesture handling, SecondaryDisplayResolver
│   ├── onboarding/               OnboardingScreen, OnboardingViewModel, OnboardingUiState
│   ├── settings/                 SettingsScreen, SettingsCategory + per-category subscreens
│   └── theme/                    EsdeCompanionTheme
├── AppContainer.kt            # hand-rolled composition root (see CLAUDE.md for why not Hilt yet)
└── CompanionApplication.kt
```

`app/src/test/...` mirrors the domain package structure with unit tests for the parser and
reducer (JUnit4, arrange-act-assert style) — this is the part of the app expected to have
the strongest test coverage.

## Running it

Open the project root in Android Studio (a recent stable release) and let it sync — it will
likely prompt to update the Android Gradle Plugin/Gradle wrapper to whatever the current
stable versions are; accept that rather than fighting the versions pinned here, which will
drift over time.

Minimum SDK is 29 (Android 10).

On first launch the app walks through onboarding: grant "All files access"
(`MANAGE_EXTERNAL_STORAGE`), then confirm the ES-DE root folder and the `downloaded_media`
folder (both default to the standard ES-DE paths and can be changed later from Settings).
No log path is hardcoded anymore — see `FileOnboardingRepository` / `AppContainer` for how
the configured folders flow reactively into the log and media repositories without an app
restart.

The app is intended to run as a home-screen replacement in a kiosk-style dual-screen setup
(see "Second-display presentation" in CLAUDE.md); it registers itself as a `HOME`/`DEFAULT`
launcher. If you just want to try it as a normal app, install it and open it directly rather
than setting it as the default launcher.

## Useful commands

```bash
./gradlew ktlintCheck detekt         # lint
./gradlew testDebugUnitTest          # unit tests
./gradlew connectedDebugAndroidTest  # instrumented/UI tests
./gradlew assembleDebug              # build APK
```
