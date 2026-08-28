# ES-DE Companion

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.esde.companion%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FRobZombie9043%2Fes-de-companion%22%2C%22author%22%3A%22RobZombie9043%22%2C%22name%22%3A%22ES-DE%20Companion%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Afalse%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D">
  <img
    src="https://github.com/ImranR98/Obtainium/blob/main/assets/graphics/badge_obtainium.png?raw=true"
    alt="Add ES-DE Companion to Obtainium"
    width="182"
  >
</a>

![Version](https://img.shields.io/badge/version-1.0.6-blue)
![Android](https://img.shields.io/badge/Android-10%2B-green)

ES-DE Companion is a companion application for [ES-DE](https://es-de.org) that enhances your gaming experience by transforming dual-screen Android devices into immersive retro gaming setups. The app displays beautiful game artwork, videos, and customizable overlay widgets on a secondary screen while you browse and play games in ES-DE.

> **Note**: v1.0.0 is the first stable release of the ground-up rebuild that replaced the old script-writing integration with direct `es_log.txt` reading (see [How It Works](#how-it-works)). **Coming from 0.6.0 or earlier?** See [Upgrading from 0.6.0](#upgrading-from-060) before you install.

The goal of this companion app is to provide a high-quality second-screen experience that seamlessly integrates with ES-DE, requires minimal setup, and offers extensive customization options for your gaming display.

ES-DE Companion is designed for dual-screen Android devices and external display setups, automatically displaying game artwork, marquees, videos, and system information as you browse your collection in ES-DE on the primary screen.

> **Note**: This is a semi-official companion app for ES-DE. While not part of the core ES-DE project, it's designed by one of the ES-DE team members as a side project to seamlessly integrate with ES-DE.

> **Note**: This app was developed with AI assistance. The full source code is available for transparency.

## Download

Visit the [Releases page](https://github.com/RobZombie9043/es-de-companion/releases) to download the latest APK.

The app requires ES-DE to be installed on your device.

## Additional Information

[USERGUIDE.md](USERGUIDE.md) - Comprehensive guide covering setup, features, and advanced configuration

[FAQ.md](FAQ.md) - Frequently asked questions and troubleshooting

## Some Feature Highlights

Here are some highlights of what ES-DE Companion can do for your gaming setup.

### Dynamic Display

_The companion screen automatically updates as you browse games in ES-DE, showing high-quality artwork and your configured widgets._

**Display Features:**
- Real-time synchronization with ES-DE by reading its own `es_log.txt` directly — no scripts to install or maintain
- Optional video playback while browsing games, as a placeable/resizable widget with a configurable start delay, loop, and audio toggle
- Optional background music tied to what you're browsing, with volume ducking or pause while a video plays
- System logo display (207 built-in logos + custom logo overrides)
- Per-widget image/logo transitions, Logo Glint, and Pan & Zoom
- Per-widget blur and darken effects

### Widget Overlay System

_Create fully customizable overlay widgets to display game artwork - marquees, box art, screenshots, fanart, and more. Position and resize each widget exactly how you want._

**Widget Features:**
- Two independent canvases — System View and Game View — each with their own widget layout
- 6 widget types on System View: System Logo, System Image, Random Game Fan Art, Random Game Screenshot, Custom Image, and Color Background
- 16 widget types on Game View: Marquee, System Logo, System Image, Description, Rating, Box Cover, 3D Box, Mix Image, Screenshot, Fan Art, Title Screen, Back Cover, Physical Media, Custom Image, Color Background, and Video
- Drag-and-drop positioning and resizing, snapped to a grid
- Independent Left/Right/Top/Bottom resize handles with edge-snapping near the screen boundary
- Layer ordering (move forward/backward)
- Per-widget transitions (Slide/Scale for logos and marquees, Fade for backdrop images), an optional Logo Glint sweep, and a Pan & Zoom effect on full-bleed backgrounds — all configured per widget, not globally
- Reached anytime via long-press anywhere on the screen (or an optional corner button — see [Floating Action Buttons](#floating-action-buttons)) — no lock setting to fight with

### App Drawer & App Dock

_A full Android app launcher accessible from the companion screen, plus an optional pinned-apps dock. Launch apps on either display with per-app preferences._

**Drawer Features:**
- Swipe up anywhere on the companion screen to open
- Optional search field (on by default) that filters by name as you type, including hidden apps — the way to find one again without leaving the drawer — plus one-tap shortcuts to Android's system Settings and this app's own Settings menu
- Customizable grid layout (3-6 columns)
- App visibility control (hide unwanted apps)
- Per-app display preferences (launch on this screen or the other screen)
- Group apps into named, renameable folders that collapse to a mosaic tile, with an optional "sort folders on top" toggle

**Dock Features (optional, off by default):**
- A persistent row of pinned apps at the bottom of the screen
- 2-5 slots, three size presets
- Same this-screen/other-screen launch preferences as the App Drawer
- A pinnable App Drawer shortcut slot for one-tap drawer access alongside your apps

### Game Launch Override

_Automatically launch a specific app whenever ES-DE starts playing a game — a standalone emulator or launcher instead of ES-DE's own setup, on a per-system or per-game basis._

**Game Launch Override Features:**
- Per-system defaults and per-game overrides, browsed from a `gamelist.xml`-backed system/game picker in Settings
- A per-game override can also be set to launch nothing, suppressing that system's default for just that one game
- A single This Screen/Other Screen choice controls which display the launched app opens on
- Input focus automatically returns to the game once the launched app appears, so it doesn't get stuck on the wrong screen (Thor devices only)
- Optional "Close App on Game End" toggle force-stops the launched app once the game ends (Thor devices only)

### Easy Setup

_The built-in onboarding wizard guides you through configuration on first launch — no scripts to generate, just a few settings to confirm._

**Setup Features:**
- Step-by-step wizard that skips steps automatically when nothing needs fixing
- Auto-detects your ES-DE and media folders where possible
- Watches ES-DE's own settings file to confirm the three required toggles are enabled
- Live confirmation step that waits for real activity from ES-DE before finishing
- Afterward, Settings and the widget editor are both one long-press away — long-press anywhere on the screen (or tap a corner button assigned to Settings) to open a popup menu; there's no separate Settings screen to hunt for
- Checks for app updates automatically on every launch, with one-tap download/install and a "what's new" prompt after each update — see [Automatic Updates](#automatic-updates)

### Floating Action Buttons

_Up to four small buttons — one per screen corner — each independently configurable to whatever's most useful to you._

**FAB Features:**
- Nine types per corner: Music Playback, Settings, Game Manual, App Drawer, a specific app you pick, Clock, System Status (battery/wifi/bluetooth), Clock & System Status combined, or none
- Music Playback and the Clock/System Status types are offered only in the two top corners
- Music Playback can occupy either top corner (but only one at a time) — assigning it to the other top corner swaps the displaced button into place rather than discarding it
- The Game Manual button appears automatically whenever the current game has a scanned manual, independent of the automatic "Manual" screen behavior setting
- A custom-app button follows the same this-screen/other-screen launch preference (and indicator dot) as the App Drawer and App Dock

### Visual Customization

_Extensive customization options let you tailor the companion display to your preferences._

**Customization Options:**
- Per-widget image/logo transitions, Logo Glint, and Pan & Zoom — set individually per widget from its own Configure Widget dialog, not a global toggle
- Per-widget blur and darken effects
- Custom system images, system logos, and background music folders
- Screen behavior control during gameplay and the screensaver (on, dimmed, off, or the game's manual) — plus a manual double-tap-anywhere gesture to blank/unblank the screen at any time
- Adjustable overlay opacity shared across the App Drawer, App Dock, and other overlay surfaces

### Automatic Updates

_ES-DE Companion checks GitHub for newer releases itself — no need to watch the Releases page._

**Update Features:**
- Silent check on every app start, auto-showing a dialog when a newer version is found
- Manual "Check for Updates" entry in Settings → Other Settings
- One-tap download and install, handling the Android "install unknown apps" permission prompt for you
- A one-time "what's new" dialog with that release's notes after the app actually updates

### Backup & Restore

_Export your entire configuration to a file, and bring it back with one restore — handy before a reinstall, a factory reset, or setting up a replacement device._

**Backup Features:**
- Settings → Setup → "Backup & Restore" exports every setting — Setup folder paths, UI/Music/Other Settings, Floating Action Button assignments, Game Launch Override, App Drawer/Dock configuration and folders, both widget canvases (Video widget config included), and Thor Settings on supported hardware — to a single JSON file you choose the name and location for
- Restoring reads a previously exported file back, with a confirmation step first since it overwrites every current setting
- No extra permissions or setup required beyond picking where the file goes

## Requirements

**Hardware:**
- Dual-screen Android device or external display support
- Android 10+ (API level 29 or higher)

**Software:**
- ES-DE installed with downloaded media
- Storage permissions for accessing media files

**Recommended:**
- [Mjolnir](https://github.com/blacksheepmvp/mjolnir) for optimal dual-screen home screen management

## Quick Start

1. **Download and Install**
   - Get the latest APK from the [Releases page](https://github.com/RobZombie9043/es-de-companion/releases)
   - Install on your Android device

2. **Initial Setup**
   - Launch ES-DE Companion
   - Follow the onboarding wizard — it walks you through storage permission and folder confirmation, and skips any step it can auto-detect

3. **Enable Settings in ES-DE**
   - Open ES-DE
   - Press START → Other Settings
   - Toggle ON "Custom Event Scripts"
   - Toggle ON "Browsing Custom Event Scripts"
   - Toggle ON "Debug Mode"
   - Back out of the settings menu (ES-DE only writes these changes to disk once you leave the menu)

4. **Start Browsing**
   - Browse games in ES-DE on your primary screen
   - Watch the companion display automatically update with artwork

## Upgrading from 0.6.0

Version 1.0.0 is a ground-up rebuild — same app (`com.esde.companion`), so installing the new APK over 0.6.0 works as a normal in-place update, but the integration mechanism underneath changed completely. A few things to know before/after you upgrade:

- **No more scripts.** The old app generated shell scripts into ES-DE's `scripts/` folder and had ES-DE call them on each event. That's gone — the app now only *reads* ES-DE's own `es_log.txt`. The onboarding wizard detects leftover script files from the old version on first launch and offers a one-tap "Delete these files" cleanup step.
- **One more ES-DE toggle required.** The old app needed "Custom Event Scripts" and "Browsing Custom Event Scripts" enabled in ES-DE. This version also requires **"Debug Mode"** — onboarding won't finish until all three are on (see [Quick Start](#quick-start), step 3).
- **Settings moved.** There's no dedicated Settings screen anymore. Long-press anywhere on the companion screen (or tap a corner button assigned to Settings — see [Floating Action Buttons](#floating-action-buttons)) to open it as a popup. The old "Show Settings Button" toggle and widget "lock mode" are both gone — widget editing is always reachable this way, with no separate unlock step.
- **Widget layouts reset to new defaults.** The widget system was rebuilt on new grid-based canvases (System View / Game View) with its own persistence, so your old widget positions don't carry forward — you'll start from the new fresh-install defaults and can re-customize from there.
- **Corner buttons are now fully configurable.** The old fixed Settings/Music corner buttons are replaced by [Floating Action Buttons](#floating-action-buttons) — assign any of Music, Settings, Game Manual, App Drawer, a specific app, or none to each of the four corners.
- **New since 0.6.0, worth trying:** the App Dock (an optional persistent pinned-apps row), App Drawer folders, per-widget transitions/Logo Glint/Pan & Zoom, in-app update checking (see [Automatic Updates](#automatic-updates)), and opt-in debug logging for troubleshooting.

Your ES-DE folder, media folder, and other Setup paths are unaffected — you shouldn't need to reconfigure those unless onboarding prompts you to confirm them.

## How It Works

ES-DE Companion reads ES-DE's own activity log — it does not write anything into ES-DE's configuration:

1. **ES-DE writes its own log**: ES-DE already logs every browsing/launch/screensaver event via its built-in `Scripting::fireEvent()` calls, to `es_log.txt` in its own log folder — as long as the three settings above are enabled.
2. **File tailing**: The companion app tails that file in real time and parses the events it cares about.
3. **State reduction**: Parsed events are reduced into a single current app state (idle, browsing a system, browsing a game, playing a game, or in the screensaver).
4. **Media display**: The app resolves the current system/game against ES-DE's downloaded media folder and renders the right artwork.
5. **Widget overlays**: Your configured widgets render on top, per the current canvas (System View or Game View).

There is no scripts folder, no script generation, and nothing for the companion app to write into ES-DE's installation.

## File Structure

The app uses these default paths (all configurable in Settings → Setup):

| Path | Default Location | Purpose |
|------|-----------------|---------|
| **ES-DE folder** | `/storage/emulated/0/ES-DE` | Root ES-DE install — the log is read from `<ES-DE folder>/logs/es_log.txt` |
| **Media folder** | `/storage/emulated/0/ES-DE/downloaded_media` | Game artwork and videos from ES-DE |
| **Custom System Images folder** | Not set | Optional system-view background image overrides |
| **Custom Logos folder** | Not set | Optional system logo overrides |
| **Custom Music folder** | Not set | Background music source — music will not play until this is set, even with the feature enabled |

## Custom Media

### System Images (Optional Override)

To override the default random game artwork in System View:
1. Set a Custom System Images folder in Settings → Setup
2. Place custom images in that folder using filenames matching ES-DE system short names: `snes.webp`, `arcade.png`, `psx.jpg`, etc.
3. Supported formats: PNG, JPG, JPEG, WEBP, GIF

### System Logos (Optional Override)

To add custom system logos or override the built-in ones:
1. Set a Custom Logos folder in Settings → Setup
2. Place custom images in that folder using filenames matching ES-DE system short names: `snes.svg`, `snes.png`, `arcade.webp`, etc.
3. Custom logos take priority over the 207 built-in logos bundled with the app

### Background Music (Optional)

To enable background music:
1. Set a Custom Music Folder in Settings → Setup
2. Enable Background Music in Settings → Background Music
3. Place music in the root of that folder for general playback, or in `systems/<system-short-name>/` subfolders for per-system playback
4. Supported formats: MP3, OGG, FLAC, WAV, M4A, AAC, OPUS

## Development

This app is a Kotlin/Jetpack Compose Android app, built with a strict Clean Architecture layering (`domain` / `data` / `ui`). See `CLAUDE.md` for the architecture, coding standards, and project conventions this codebase follows.

```bash
./gradlew ktlintCheck detekt         # lint
./gradlew testDebugUnitTest          # unit tests
./gradlew connectedDebugAndroidTest  # instrumented/UI tests
./gradlew assembleDebug              # build APK
```

## Credits

**Integration:**
- Designed for [ES-DE](https://es-de.org) by Leon Styhre

**Community:**
- Special thanks to the ES-DE community for feedback

## License

This project is open source. See the LICENSE file for details.

## Support

**Issues and Bug Reports:**\
https://github.com/RobZombie9043/es-de-companion/issues

**Documentation:**\
See [USERGUIDE.md](USERGUIDE.md) for comprehensive documentation
