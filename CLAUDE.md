# CLAUDE.md

Guidance for Claude Code when working in this repository. This project is a ground-up rebuild of **ES-DE Companion**, a dual-screen companion app for the ES-DE emulation frontend, using modern Android best practices.

This document is deliberately weighted toward **structure, layering, and code quality** rather than a fixed feature spec. The core state pipeline is foundational and must be built to a higher standard of rigor than everything else in the app.

## The One Thing That Has To Be Rock Solid

Everything this app does depends on correctly understanding what ES-DE is currently doing. The pipeline is:

```
es_log.txt (raw text, growing/truncating file)
    → tail new lines
    → filter for "Scripting::fireEvent():" lines
    → parse into a typed Event
    → reduce events into a single current AppState
    → AppState drives everything downstream (media lookup, UI, widgets, whatever comes later)
```

This pipeline is the foundation everything else is built on. Treat it accordingly:

- It lives entirely in `domain`, as plain Kotlin with no Android framework dependencies where at all possible — the parsing and state-reduction logic should be testable with plain unit tests against fixture log lines, with no emulator, no instrumentation, no mocked Android classes.
- It should have the best test coverage of anything in the codebase. Every event type, malformed/unexpected line, empty-args case, and the truncate-on-restart case need explicit tests.
- Resist the urge to let UI or media-resolution concerns leak into this layer. Its only job is: raw log text in, correct `AppState` out. Nothing else should influence it, and it should not know anything about widgets, screens, or file paths for media.
- If this layer has a bug, the entire app is wrong regardless of how polished anything built on top of it is. Prioritize correctness and clarity here over cleverness or premature abstraction.
- **Cold start is not "replay the last event."** A freshly-launched app has no state to reduce onto, and context-dependent events (e.g. `screensaver-end`, which needs to know what was active *before* the screensaver started) produce the wrong result if the reducer only sees the single most recent line. `EsdeLogFileRepository` instead finds the last self-contained ("anchor") event and replays forward from there, growing its read window backward until it finds one or hits the start of the file. See `EsdeLogFileRepositoryTest` for the anchor-replay cases.

## Architecture & Structure

**Clean Architecture, three layers, strictly one-directional dependencies:**

```
ui        → depends on → domain
data      → depends on → domain
domain    → depends on → nothing (no Android imports, no other layers)
```

- `domain`: models, the event parser, the state reducer, use-case classes, repository *interfaces*. Pure Kotlin. This is the layer that should be easiest to read, test, and reason about — it's the specification of what the app does, independent of how.
- `data`: repository *implementations* — file I/O, log tailing, media file resolution, persistence. Talks to Android/filesystem APIs. Implements the interfaces `domain` defines, never the reverse.
- `ui`: Compose screens + ViewModels. ViewModels depend on `domain` use cases/repository interfaces only — never directly on `data` implementations, never directly on `java.io.File`, `PackageManager`, or `FileObserver`.

Keep this a **single Gradle module** with package-based layering (`domain/`, `data/`, `ui/` packages) rather than splitting into multiple Gradle modules. Multi-module setups add build-config overhead that isn't justified at this project's size; package boundaries plus code review discipline are enough to keep the dependency rule honest. Revisit this only if the codebase grows enough that build times or team size actually demand it.

**Within each layer, favor small, single-purpose classes over large ones.** A class that parses log lines should not also decide file paths. A ViewModel for one screen should not accumulate responsibilities for another. If a class is hard to name in one sentence, it's probably doing too much.

## Code Quality & Maintainability Standards

- **Immutability by default.** Data classes with `val`, not `var`. State changes happen by producing a new value, not mutating an existing one. This matters most in the state-reduction pipeline, where a mutable shared state object is the classic source of subtle bugs.
- **No `!!`.** Handle nullability explicitly at the point where it arises, not by asserting it away.
- **Explicit over implicit.** Sealed classes for anything with a closed set of variants (event types, UI state, results). Exhaustive `when` blocks with no `else` branch where the compiler can enforce completeness — this is what catches "forgot to handle the new event type" at compile time instead of runtime.
- **Errors are values where they're expected, exceptions where they're not.** Parsing a malformed log line is an expected, routine outcome — represent it as a nullable/`Result` return, not a thrown exception. A genuinely unexpected failure (e.g. can't open a file that should exist) can throw, but should be caught at a clear boundary rather than left to propagate into UI code.
- **Small, well-named functions over comments explaining what a large function does.** If a comment is needed to explain *what* a block does, that block is usually better extracted into a named function. Comments should explain *why*, not *what*.
- **Coroutines + Flow for all async work** — no callbacks, no `AsyncTask`, no `LiveData`. `StateFlow` for observable state, `SharedFlow`/`Channel` for one-off events.
- **Dependency injection, consistently applied.** Either Hilt or a hand-rolled composition root — pick one deliberately and use it everywhere; don't mix DI approaches or fall back to ad-hoc singletons partway through the codebase.
- **Testability is a design constraint, not an afterthought.** If a class is hard to unit test, that's usually a sign it's reaching across layers it shouldn't, or mixing concerns. Prefer constructor injection of dependencies (including things like a `Clock`/time source, or a `FileSystem` abstraction) so tests can substitute fakes.
- **Consistent formatting, enforced by tooling, not convention.** `ktlint`/`detekt` (or equivalent) run as part of the build, not as a manual habit.

## What This Rebuild Explicitly Removes

The legacy app generated and wrote its own shell scripts into ES-DE's `scripts/` folder and had ES-DE call them on each event, producing separate companion-owned log files. **That mechanism is gone.** ES-DE already writes every event to its own `es_log.txt` via `Scripting::fireEvent()` calls — the rebuild only ever *reads* that file. No script writing, no script folder configuration, no "enable custom event scripts" setup step. Confirmed: ES-DE writes a fresh `es_log.txt` on every restart (truncated/recreated, not appended-forever), so the tailer must detect a size decrease and reset its read position rather than assuming monotonic growth.

## Feature Status

- **Log pipeline, state reduction, media resolution, onboarding, Settings, and the App Drawer are built** and described in README.md — treat their existing structure (package layout, repository interfaces, use cases) as the pattern to extend, not a sketch to redesign.
- **Widget overlay system** (beyond the current debug-only `StateOverlay`) remains an unbuilt sketch from early design conversations — its data shapes and screens are still open when it's actually tackled.
- **Second-display presentation**: the app is a plain single-Activity app placed on the secondary display via the OS/Mjolnir, launching specific apps onto it via `ActivityOptions.setLaunchDisplayId` (see `SecondaryDisplayResolver`). No in-app `Presentation`/`DisplayManager` complexity beyond that.
- Don't treat "Widget overlay system" above as a final design — confirm current intent before implementing details that aren't in the active task.

## Known Gotchas (Confirmed Fixes — Don't Re-Litigate)

These cost real debugging time to track down. If you hit a symptom that matches one of these, the fix below is already applied; look for where it lives before proposing a new one.

- **`navigation-compose` predictive-back crash (b/384186542 / b/375343407).** Adding `SETTINGS` (or any similarly deep screen) as a real `NavHost` destination triggers an `IndexOutOfBoundsException` in `PredictiveBackHandler` on the versions this project uses. Fix: `SETTINGS` is *not* in `MainActivity`'s `NavHost` graph at all — it's shown/hidden via a plain `showSettings` boolean, and `SettingsScreen`'s own internal drill-down (category list → subpage → Manage Apps) is likewise plain `rememberSaveable` state with a manual `onBack` chain, not a nested `NavHost`. See the doc comment on `SettingsCategory`. Do not reintroduce nav-compose destinations for these screens.
- **`CrossfadeAsyncImage` needs exactly one call site per role.** Multiple `when`-branch `AsyncImage`s for what is conceptually "the current backdrop" cause Compose to dispose/remount on state-shape change, discarding `remember`ed crossfade animation state and producing a flash to the empty background. `MainScreenImages` keeps a single persistent `CrossfadeAsyncImage` for the backdrop and swaps only its `model`. Note the *logo* overlay deliberately uses a plain `AsyncImage` instead — it's a transparent PNG/SVG with nothing opaque behind it, and crossfading it briefly double-exposed the outgoing and incoming logos.
- **Directory-as-file media paths.** ES-DE can report a ROM *directory* (e.g. a `.m3u` multi-disc folder) as the launchable unit, and does not strip the trailing extension in that case the way it does for an ordinary file — the extension there is the directory's literal name, not a stripped file extension. This can't be determined from the path string alone; `GameMediaPathResolver` takes an explicit `romIsDirectory: Boolean` computed via real filesystem `File.isDirectory` checks in the data layer, keeping the resolver itself pure and unit-testable against fixture paths.
- **Hardware/system back must never exit the app.** It's meant to run continuously on a secondary display. Handling is currently a single, unconditional `BackHandler` in `MainScreen` (closes the App Drawer if open, otherwise consumes the event and does nothing) plus a separate `BackHandler` in `SettingsScreen` driving its own back-stack state. There is no debounce/gating layer over this — an earlier suspected vendor-ROM synthetic-back-event issue did not require one once the above fixes (particularly removing `SETTINGS` from `NavHost`) were in place. If a back-button freeze or double-fire resurfaces, verify it's not one of the already-fixed causes above before adding new debounce machinery.

## Tech Stack

| Concern | Library |
|---|---|
| UI | Jetpack Compose (Material 3) |
| Navigation | Navigation-Compose, pinned at 2.8.4 — used only for the `ONBOARDING`/`MAIN` top-level switch; deep/nested screens deliberately avoid it (see Known Gotchas) |
| Async | Kotlin Coroutines + Flow |
| Image loading | Coil 3, with the SVG decoder |
| Persistence | Jetpack DataStore (Preferences) |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) |
| Testing | JUnit4, Turbine (Flow testing) |

Minimum SDK: API 29 (Android 10).

Media3/`ExoPlayer` is not yet a dependency — add it if/when video media playback is actually tackled, not preemptively.

## What NOT to Do

- Don't let the event-parsing/state-reduction layer depend on anything outside plain Kotlin — no Android imports, no leaking UI or media concerns into it.
- Don't reintroduce script generation/writing to ES-DE's `scripts/` folder.
- Don't bypass the repository layer to touch the filesystem or `PackageManager` from UI code.
- Don't add a network layer — this app has none and shouldn't gain one.
- Don't split into multiple Gradle modules preemptively — package-based layering is sufficient at this size.
- Don't add `NavHost` destinations for `SETTINGS` or other deep screens — see Known Gotchas.
- Don't add back-press debounce/gating logic to work around a suspected double-fire without first confirming it isn't caused by one of the already-fixed issues above.

## Useful Commands

```bash
./gradlew ktlintCheck detekt         # lint
./gradlew testDebugUnitTest          # unit tests
./gradlew connectedDebugAndroidTest  # instrumented/UI tests
./gradlew assembleDebug              # build APK
```
