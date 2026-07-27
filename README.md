# ES-DE Companion (rebuild)

Ground-up rebuild of ES-DE Companion. See `CLAUDE.md` for the architecture, structure, and
coding standards this project follows — read that first.

## Current scope

This is the base app, deliberately minimal:

- Reads `/storage/emulated/0/ES-DE/logs/es_log.txt` (hardcoded for now — see `AppContainer`,
  a Settings screen to make this configurable is a later, iterative piece).
- Tails the log, parses `Scripting::fireEvent()` lines into typed events, and reduces them
  into a single current `AppState`.
- Displays that `AppState` as plain text on screen, updating live as ES-DE's state changes.

Nothing else — no widgets, no media lookup, no settings, no permission-request flow — is
implemented yet. Those come later, one at a time.

## Project layout

```
app/src/main/java/com/esde/companion/
├── domain/          # pure Kotlin, no Android imports — the state pipeline lives here
│   ├── model/        EsdeEvent, AppState
│   ├── parser/        EsdeEventParser — raw log line -> EsdeEvent
│   ├── state/          AppStateReducer — (AppState, EsdeEvent) -> AppState
│   ├── repository/    EsdeLogRepository interface
│   └── usecase/        ObserveAppStateUseCase
├── data/
│   └── log/            EsdeLogFileRepository — polls and tails es_log.txt
├── ui/
│   ├── MainActivity.kt
│   ├── main/            MainViewModel, MainScreen, display formatting
│   └── theme/
├── AppContainer.kt    # hand-rolled composition root (see CLAUDE.md for why not Hilt yet)
└── CompanionApplication.kt
```

`app/src/test/...` mirrors the domain package structure with unit tests for the parser and
reducer — this is the part of the app expected to have the strongest test coverage.

## Running it

Open the project root in Android Studio (a recent stable release) and let it sync — it will
likely prompt to update the Android Gradle Plugin/Gradle wrapper to whatever the current
stable versions are; accept that rather than fighting the versions pinned here, which were
current as of when this scaffold was generated and will drift.

Minimum SDK is 29 (Android 10). No storage permission request flow is implemented yet, so on
a real device you'll need to grant "All files access" to the app manually via Android
Settings before the log file can be read.

The log path is hardcoded to `/storage/emulated/0/ES-DE/logs/es_log.txt`. If your ES-DE
installation logs somewhere else, edit `AppContainer.logFilePath` directly for now.
