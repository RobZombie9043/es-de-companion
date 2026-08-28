# ES-DE Companion - User Guide

This comprehensive guide covers everything you need to know about ES-DE Companion, from initial setup to advanced customization.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Onboarding Wizard](#onboarding-wizard)
3. [Screen Gestures](#screen-gestures)
4. [Floating Action Buttons](#floating-action-buttons)
5. [Widget Overlay System](#widget-overlay-system)
6. [App Drawer](#app-drawer)
7. [App Dock](#app-dock)
8. [Game Launch Override](#game-launch-override)
9. [Video & Music](#video--music)
10. [Settings Reference](#settings-reference)
11. [File Structure](#file-structure)
12. [How Log Events Work](#how-log-events-work)
13. [In-App Updates](#in-app-updates)
14. [Backup & Restore](#backup--restore)
15. [Advanced Topics](#advanced-topics)

---

## Getting Started

### What is ES-DE Companion?

ES-DE Companion is a secondary screen application that displays game artwork, videos, and customizable widgets while you browse and play games in ES-DE on your primary screen. It's designed specifically for dual-screen Android devices.

### Requirements

**Hardware:**
- Dual-screen Android device (Ayn Thor, Ayaneo Pocket DS, Anbernic RG DS etc.)
- Android 10 or higher (API level 29+)

**Software:**
- ES-DE installed and configured, with game media downloaded (scraped)
- Approximately 50MB free storage for the app

**Permissions:**
- "All files access" (storage), for reading ES-DE's log and media folders directly

### Installation

1. **Download the APK**
   - Visit the [Releases page](https://github.com/RobZombie9043/es-de-companion/releases)
   - Download the latest APK file

2. **Install the App**
   - Enable "Install from Unknown Sources" if prompted
   - Open the downloaded APK
   - Follow Android's installation prompts

3. **Launch ES-DE Companion**
   - Open the app on your secondary screen
   - The onboarding wizard starts automatically

4. **Enable Settings in ES-DE**
   - Open ES-DE on your primary screen
   - Press START to open the main menu
   - Navigate to: Other Settings
   - Toggle ON: "Custom Event Scripts"
   - Toggle ON: "Browsing Custom Event Scripts"
   - Toggle ON: "Debug Mode"
   - Back out of the settings menu — ES-DE only saves these to disk once you leave the menu

### First Launch

On first launch, ES-DE Companion will:
1. Request "All files access"
2. Confirm (or ask you to pick) the ES-DE folder and media folder
3. Offer to clean up any leftover files from an older version of the app, if found
4. Check that the three ES-DE settings above are enabled, watching live for you to enable them
5. Wait for you to browse a system or game in ES-DE, to confirm the connection is actually working

**Tip:** Keep both screens visible during setup to easily verify the connection between ES-DE and the companion app.

---

## Onboarding Wizard

The onboarding wizard adapts to what it finds — steps it can auto-detect or that don't apply are skipped automatically, so a typical setup is shorter than the full list below.

### Step 1: Storage Permission

The app needs "All files access" to read ES-DE's log file and media folder directly.

**What happens:** Tapping "Grant access" opens the system permission screen. The wizard automatically continues once permission is granted, including if you grant it and return from system Settings.

**Skipped if:** permission is already granted when the app starts — onboarding begins directly at the next step.

### Step 2: ES-DE Folder

Select the folder ES-DE itself is configured to use.

**Default guess:** `/storage/emulated/0/ES-DE`

The wizard shows this guess automatically; if it's correct you can just confirm it, or use "Choose different folder" to pick manually. The folder is validated by checking for `settings/es_settings.xml` inside it. Picking a valid folder manually advances the wizard automatically, without needing a separate Next tap.

### Step 3: Media Folder

Select where ES-DE stores downloaded game media.

**Default guess:** `<ES-DE folder>/downloaded_media`

**Skipped entirely if:** the wizard can auto-detect a valid media directory from ES-DE's own settings file. It's only shown if auto-detection fails or points at a folder that doesn't exist.

### Step 4: Leftover Script Files

If any files remain in the ES-DE folder from an older version of ES-DE Companion, this step lists how many were found and offers a one-tap "Delete these files" button.

**Skipped entirely if:** no leftover files are found.

### Step 5: ES-DE Settings

Checks that the following are enabled in ES-DE itself (Main Menu → Other Settings):
- Custom Event Scripts
- Browsing Custom Event Scripts
- Debug Mode

The wizard watches ES-DE's settings file live and auto-confirms this step the moment all three are enabled — note that ES-DE only writes its settings file when you back **out** of its settings menu, not while it's still open.

**Skipped entirely if:** all three are already enabled, and the `DebugSkipInputLogging` warning below doesn't apply, when this step is reached.

If ES-DE's `settings/es_settings.xml` has `DebugSkipInputLogging` set to `true` — a debug flag not exposed anywhere in ES-DE's own settings menu — this step also shows a warning: the companion app won't be able to tell which direction you're navigating, so widget slide animations won't play correctly. This isn't required to fix, and doesn't block continuing — if you want to fix it, edit `es_settings.xml` directly and set it back to `false`.

### Step 6: Confirm It's Working

Browse to a system or game in ES-DE now. This step watches the connection and shows one of:
- "es_log.txt not found..." — the log file isn't where expected
- "Waiting for activity..." — the log is found, but no browsing activity has been seen yet
- "Working! ES-DE activity detected." — confirmed

"Finish setup" only becomes available once this step confirms activity.

### Re-entering Onboarding

**There is no manual "redo setup" button in Settings.** Onboarding only runs automatically:
- On first launch (until it's completed once), or
- If "All files access" has since been revoked in Android's system Settings

In the revoked-permission case, onboarding restarts at the permission step but pre-fills your previously confirmed folder paths, rather than resetting to the defaults.

---

## Screen Gestures

Three gestures work anywhere on the main companion screen (when the App Drawer is closed):

| Gesture | Result |
|---|---|
| **Swipe up** | Opens the [App Drawer](#app-drawer) |
| **Long-press** (or tap a corner button assigned to Settings, if any) | Opens the Settings/Widgets popup menu — see [Widget Edit Mode](#widget-edit-mode) and [Settings Reference](#settings-reference) |
| **Double-tap** | Manually blanks the screen to black; double-tap again to restore |

The double-tap blank gesture is always available, independent of the automatic Game Playing/Screensaver Screen Behavior settings (see [Settings Reference](#settings-reference)) — it uses the same black cover, so it also restores with a double-tap when triggered automatically by a Screen Behavior set to "Off."

Long-press always opens Settings regardless of whether a corner button is also assigned to it — see [Floating Action Buttons](#floating-action-buttons) for the configurable corner buttons themselves.

---

## Floating Action Buttons

ES-DE Companion can show up to four small floating buttons on the main screen — one per corner — each independently configurable in **Settings → UI Settings → Floating Action Buttons**.

### Available Types

| Type | Behavior |
|---|---|
| **Music** | Toggles the floating music controls panel (title, play/pause, skip). Only offered for the two *top* corners — see [Music Placement](#music-placement) — and only actually shown once a track is loaded. |
| **Settings** | Opens the same long-press Settings/Widgets popup described in [Screen Gestures](#screen-gestures). |
| **Manual** | Shown whenever the game currently playing, browsing, or displayed by the screensaver has a scanned manual PDF. Tapping opens it in the manual viewer — the same viewer the "Manual" Game Playing Screen Behavior option (see [Settings Reference](#settings-reference)) shows automatically, but on demand and independent of that setting. |
| **App Drawer** | Opens the [App Drawer](#app-drawer), the same as swiping up. |
| **App** | Launches a specific app you choose. See [Launching a Custom App](#launching-a-custom-app) below. |
| **Clock** | Display-only — shows the current time, ticking every second, using your device's own 12h/24h format preference. Top corners only. |
| **System Status** | Display-only — shows Battery (always, with charge tier and a charging bolt), Wifi, and Bluetooth. Wifi/Bluetooth icons only appear while actually connected; there's no "off" icon variant for either. Top corners only. |
| **Clock & System Status** | Both of the above combined in one wide button. Top corners only. |
| **None** | No button shown in that corner. |

Clock, System Status, and Clock & System Status are variable-width (unlike every other FAB type, which is a fixed square) since their content isn't a fixed size. Showing Bluetooth status requires the Bluetooth permission on Android 12+; the first time you assign a System Status-family FAB, a one-time rationale dialog asks for it (declining just means the Bluetooth icon never appears).

### Defaults

| Corner | Default |
|---|---|
| Top Left | Music |
| Top Right | Settings |
| Bottom Left | None |
| Bottom Right | Manual |

### Music Placement

Music can occupy only one of the two top corners at a time. Assigning it to the other top corner swaps whatever was already there into the newly-vacated corner instead of discarding it — for example, with the defaults above, setting Top Right to Music moves Settings to Top Left. Music is never offered as an option for either bottom corner.

### Launching a Custom App

Assigning **App** to a corner adds a second row underneath its type dropdown for picking which app — tap it to open a "Select App" list of every installed app (independent of the App Drawer's own hidden-apps list — an app hidden from the drawer can still be picked here). Once picked, the corner button shows that app's own icon instead of a generic one.

Tapping it follows the same this-screen/other-screen launch convention as the App Drawer and App Dock (see [App Drawer](#app-drawer)), and shares the same remembered preference: a single tap launches on whichever screen that app last launched on (this screen, by default), and a double tap launches it on the *other* screen and remembers that as the new preference. A small dot appears on the icon — identical to the App Drawer/App Dock indicator — whenever the other screen is currently preferred.

---

## Widget Overlay System

Widgets are customizable overlay elements that display game and system artwork.

### Canvases

ES-DE Companion has two independent widget canvases, each with its own layout:

| Canvas | Shown when... |
|---|---|
| **System View** | Browsing the system carousel |
| **Game View** | Browsing a game, playing a game, or during the screensaver |

While ES-DE Companion hasn't seen any activity yet (idle), no widget canvas is shown at all.

### Available Widget Types

#### System View Widgets

| Widget Type | Description |
|------------|-------------|
| **System Logo** | Current system's logo (custom override, then one of 207 built-in logos, then the system name as text) |
| **System Image** | A whole-system representative image (custom override, then random fanart, then random screenshot, then a generic background) |
| **Random Game Fanart** | Random fanart pulled from any game in the current system |
| **Random Game Screenshot** | Random screenshot pulled from any game in the current system |
| **Custom Image** | An image you pick from your device |
| **Color Background** | A solid color panel with adjustable transparency |

#### Game View Widgets

| Widget Type | Description | Media Folder |
|------------|-------------|--------------|
| **Marquee** | Arcade-style marquee artwork | `marquees/` |
| **System Logo** | The current game's system logo (same source as the System canvas widget) | (none — logo lookup only) |
| **System Image** | The current game's system representative image (same source as the System canvas widget) | (none — image lookup only) |
| **Description** | Scrollable text description, from `gamelist.xml` | (none — text only) |
| **Rating** | The game's `<rating>` from `gamelist.xml`, shown as five filled/unfilled stars | (none — text/star only) |
| **Box Cover** | Front cover of the game box | `covers/` |
| **3D Box** | 3D perspective box art | `3dboxes/` |
| **Mix Image** | ES-DE's composite image (screenshot + metadata) | `miximages/` |
| **Screenshot** | In-game screenshot | `screenshots/` |
| **Fan Art** | High-quality background artwork | `fanart/` |
| **Title Screen** | Game's title screen | `titlescreens/` |
| **Box Back Cover** | Back of the game box | `backcovers/` |
| **Physical Media** | Disc/cartridge artwork | `physicalmedia/` |
| **Custom Image** | An image you pick from your device | N/A |
| **Color Background** | A solid color panel with adjustable transparency | N/A |
| **Video** | The game's video, playing while browsing (never during gameplay) — see [Video Playback](#video-playback) | `videos/` |

System Logo and System Image are offered on both canvases — the system a game belongs to is meaningful context either way.

### Per-Widget Configuration

Configuration options depend on the widget type, shown in this order in the Configure Widget dialog:

- **Image Scaling** — every image-backed widget (System Logo, System Image, Random Fanart/Screenshot, all Game View media widgets, Custom Image): **Contain** (shows the whole image, letterboxed) or **Cover** (crops to fill the frame).
- **Transitions** — one of two pickers, depending on widget style, both hidden when they'd have no effect:
  - **Logo Transitions** (**None**/**Slide**/**Scale**, no Fade) — logo-style widgets only: System Logo, and Marquee media widgets.
  - **Image Transitions** (**None**/**Fade**) — backdrop-style widgets (System Image, Custom Image, and non-Marquee media widgets), hidden entirely when the widget is Contain-scaled (a letterboxed swap would read as a hard cut anyway) or when it's a box-art type — Box Cover, 3D Box, Mix Image, Back Cover, Physical Media always snap in instantly, since a cross-dissolve between two unrelated pieces of box art looks like a smear rather than a clean cut.
- **Logo Glint** — an independent on/off toggle shown alongside Logo Transitions, on the same logo-style widgets. Adds a periodic light sweep across the widget; it runs alongside whichever transition is selected, not in place of it.
- **Pan & Zoom** — an independent on/off toggle for eligible Cover-scaled backdrop widgets (System Image, Custom Image, Fan Art, Screenshot, Title Screen). Slowly zooms and pans across the image while it's displayed. Never available on logos, Color Background, or Description.
- **Blur** and **Darken** sliders (0-100% each) — every image-backed widget.
- **Color Background** (its own widget type, no image options): 8 preset swatches or a free-form hex color, plus a **Transparency** slider (0-100%).
- **Description** (its own widget type): **Text Size** (10-36sp), **Text Color**, **Background Color**, and **Background Transparency**.
- **Rating** (its own widget type): filled/outline star colors, background color/transparency, and a **No Rating Behavior** choice (Hide, or show empty stars) for games with no `<rating>` at all.
- **Video** (its own widget type, no image options): **Scale Mode** (Contain/Cover), **Pillarbox** (Contain only — black bars or transparent), **Audio** on/off, **Start Delay** (0-10s), **Loop** on/off, and **Render Above UI** — see [Video Playback](#video-playback).

All of the above is set per widget instance, from that widget's own Configure Widget dialog — there's no global transitions/glint/pan-and-zoom setting in Settings. A freshly-added System Logo or Marquee widget starts with the Slide transition and Logo Glint on; a freshly-added System Image or Fan Art background starts with Pan & Zoom on and 10% darken.

### Widget Edit Mode

**To enter edit mode:**
1. Long-press anywhere on the companion screen (or tap a corner button assigned to Settings, if any — see [Floating Action Buttons](#floating-action-buttons)) to open the Settings popup
2. Tap **Widgets** at the top of the list — this jumps straight into the widget editor rather than showing a subpage, since there's nothing else in that category
3. Edit mode opens, showing the last system/game you browsed as a preview (not necessarily what's live right now)

There's no lock setting gating this — widget editing is always reachable this way.

### The Options Menu

Once you're inside the editor, a small corner button (⋮) opens a separate edit-mode menu — distinct from the long-press popup that got you here:

1. **System View / Game View** — switch which canvas you're editing
2. **Add Widget** — pick a widget type from the canvas's catalog; it's placed centered on the grid, sized to fit, and auto-selected
3. **Configure Widget** *(shown only when a widget is selected)* — opens that widget's configuration options
4. **Move Forwards / Move Backwards** *(shown only when a widget is selected)* — nudges the widget one layer up or down in stacking order
5. **Remove Widget** *(shown only when a widget is selected)* — deletes it immediately, no confirmation
6. **Done** — exits edit mode

### Moving and Resizing Widgets

**Selecting a widget:** tap it — a selected widget shows resize handles.

**Moving:** drag a selected widget; it snaps to the grid as you cross cell boundaries.

**Resizing:** four independent edge handles (left, right, top, bottom) — dragging one grows or shrinks that edge only, snapping to the grid, with the opposite edge staying anchored. Dragging near the true screen edge snaps straight to the boundary once you're close, so widgets can reach the very edge of the screen.

**Layering:** use "Move Forwards"/"Move Backwards" in the menu to change which widget draws on top when two overlap.

**Deleting:** select the widget, then "Remove Widget" in the menu.

A faint grid line overlay is always shown in edit mode to help with alignment — all placement is grid-cell-snapped, there's no free/pixel placement.

**Exiting:** tap Done in the menu, or use system Back — Back closes an open dialog first, then deselects the current widget, then exits edit mode, one step at a time.

---

## App Drawer

The app drawer provides full access to all installed Android apps directly from the companion screen.

### Opening and Closing

**Open:** swipe up anywhere on the companion screen.

**Close:** drag the handle at the top of the drawer back down, drag the drawer grid down, or press Back.

### Using the App Drawer

**Search and shortcuts:**
- A search field sits above the grid by default (Settings → App Drawer and Dock → "Show Search Bar" toggle, on by default — see [Settings Reference](#settings-reference)) — type to filter the grid by app name, case-insensitive
- Search matches *every* app, including hidden ones — it's the way to find a hidden app again without going to Settings → Manage Apps. A hidden app found this way carries a small bold **H** mark on its icon
- Apps that belong to a folder show up as ordinary flat tiles when they match a search, not nested inside a folder tile — folder tiles themselves never appear in search results
- Tap the **×** in the field to clear it, or just close the drawer — the query always resets when the drawer closes (and also when the "Show Search Bar" toggle itself is turned off)
- Two icon buttons sit to the right of the search field: a gear opens Android's own system Settings, and a hamburger opens this app's own Settings/Main Menu popup on top of the still-open drawer

**Launching apps:**
- **Single tap** launches the app at whichever screen it last launched on (defaults to "this screen" the first time).
- **Double tap** launches it on the *other* screen than the single-tap default, and remembers that as the new preference. If no secondary display is available, this just launches on the current screen instead, without changing the saved preference.
- **Long-press** opens a menu: "Launch on this screen," "Launch on other screen," "App Info," then either "Remove from Folder" (an app already inside one, including a folder member found via search — see [Folders](#folders)) or "Add to Folder" (omitted for a hidden app found via search), and finally "Hide App" (or "Unhide App" for a hidden app found via search).

Launching any app automatically closes the drawer.

### Managing App Visibility

Go to **Settings → App Drawer and Dock → Manage Apps** to see every installed app with a checkbox — unchecking an app hides it from the drawer (and from the dock's add-app picker). You can also unhide an app directly from the drawer: search for it by name (search matches hidden apps too — see [Using the App Drawer](#using-the-app-drawer)) and choose "Unhide App" from its long-press menu.

### Folders

Group related apps into a named folder that collapses to a single tile in the grid.

- **Create a folder:** long-press an app → **Add to Folder** → **New Folder**, then name it. The app is added to the new folder immediately.
- **Add more apps:** long-press any other app → **Add to Folder** → pick an existing folder from the list.
- **Open a folder:** tap its tile. It shows a mosaic preview of up to 4 member app icons (or a plain folder glyph if empty) and opens a small popup grid with its contents over a blurred backdrop.
- **Launch from inside a folder:** the same single tap / double tap / long-press behavior as the top-level drawer applies to apps inside a folder — see [Using the App Drawer](#using-the-app-drawer).
- **Remove an app from a folder:** long-press it (while inside the folder popup) → **Remove from Folder**. This puts it back in the main grid as an ordinary app; it doesn't hide or uninstall it. A folder emptied this way closes its popup automatically and disappears from the grid.
- **Rename a folder:** long-press its tile directly in the grid, or long-press its title bar while the folder popup is open — either works.
- **Ordering is fully automatic** — there's no manual drag-to-reorder. Folders sort alphabetically by name among themselves, and apps (whether ungrouped or inside a folder) sort alphabetically by label among themselves. Whether folders are grouped ahead of ungrouped apps, or interleaved with them in one alphabetical list, is controlled by the **Sort folders on top of apps** setting (Settings → App Drawer and Dock, on by default) — see [Settings Reference](#settings-reference).
- An app can belong to at most one folder at a time.
- A hidden or uninstalled app is dropped from its folder automatically, the same as it would be from the top-level grid.

---

## App Dock

The App Dock is an optional, persistent row of pinned apps at the bottom of the main screen, sitting just above where the App Drawer slides up from. It's **off by default**.

### Behavior

- When enabled, the dock is always visible at the bottom of the screen while the App Drawer is closed, and slides away along with the drawer when you open it.
- It always shows a fixed number of slots (2-5, configurable) — any empty slot is shown as an add-app target.

### Adding, Removing, and Reordering

- **Long-press an empty slot** to open a picker of installed apps not already pinned.
- **Long-press a filled slot** to open a menu: "Launch on this screen," "Launch on other screen," "App Info," "Move Left," "Move Right," "Remove from Dock." (There's no "Hide App" option here — hiding is managed from the App Drawer's Manage Apps screen.)
- **Single tap** and **double tap** on a dock icon behave the same way as in the App Drawer (last-used screen, or the other screen).

### App Drawer Shortcut

The add-app picker also offers a special **App Drawer** entry at the top of the list (as long as it isn't already pinned) — pin it into a slot like any other app for one-tap drawer access alongside your pinned apps. Tapping or double-tapping it opens the App Drawer instead of launching anything. Its long-press menu only offers **Move Left**, **Move Right**, and **Remove from Dock** — "Launch on this/other screen" and "App Info" don't apply to it.

### Dock Settings

Configured in **Settings → App Drawer and Dock**:
- **Enable Dock** — off by default
- **Maximum dock apps** — 2 to 5 slots (only shown when the dock is enabled)
- **Dock size** — Small, Medium, or Large (only shown when the dock is enabled)

The dock's transparency isn't a separate setting — it shares the same master "Overlay Opacity" setting (Settings → UI Settings) as the App Drawer.

---

## Game Launch Override

Automatically launch a specific app whenever ES-DE starts playing a game — useful for handing off to a standalone emulator or launcher instead of ES-DE's own RetroArch/core setup for particular systems or games.

- Configured in **Settings → UI Settings → Game Launch Override**, which opens a browser of every system that has a `gamelist.xml` (the same source the Description widget reads from — not a full ES-DE library browser, just enough to assign overrides).
- **System default**: pick an app for a whole system — every game in that system launches it unless a per-game override says otherwise.
- **Per-game override**: drill into a system to pick an individual game and assign it its own app, overriding that system's default. An override can also be set to "nothing," which suppresses the system default for that one game rather than falling back to it.
- **Display target**: a single global "This Screen" / "Other Screen" choice (default: This Screen) controls which display the launched app opens on — This Screen is Companion's own screen (temporarily replacing its UI there), Other Screen is whichever display ES-DE/the game itself is running on.
- **Focus automatically returns to the game** once the launched app appears, on Ayn Thor devices — launching an app on either screen steals hardware-key/controller focus, so Companion hands it back once the launched app has actually finished loading. Best-effort and automatic; there's no toggle for it.
- **Close App on Game End** (off by default, Thor devices only): force-stops the launched app once the triggering game ends, reusing the same mechanism as Thor Settings' Task Killer.
- Nothing launches for a system/game with no override configured — there's no separate on/off toggle needed.

---

## Video & Music

### Video Playback

Video is a **widget**, not a global setting — add a **Video** widget to the Game canvas (see [Widget Overlay System](#widget-overlay-system)) to enable it. There's no separate on/off toggle: no Video widget placed means no video plays.

- Plays **only while browsing a specific game** — never during actual gameplay, never during the screensaver, and never while browsing systems.
- **Start Delay**: 0-10 seconds before playback starts, shown as "Off" at 0 (the default for a freshly-added widget). Until playback actually begins, the widget canvas underneath remains visible.
- **Audio**: on/off toggle for whether the video plays with sound (default on).
- **Loop**: on/off toggle — repeats the video, or plays it once and holds on the final frame (default on).
- **Scale Mode**: Contain (letterboxed) or Cover (crops to fill); Contain-only adds a **Pillarbox** choice (black bars or transparent).
- **Render Above UI**: an opt-out per-widget toggle to draw this specific video above everything else (FABs, App Dock, App Drawer, dim/black screen covers) instead of at the normal widget-canvas layer.

All of the above is configured per widget instance, from that Video widget's own Configure Widget dialog — see [Per-Widget Configuration](#per-widget-configuration).

### Background Music

Optional music that follows what you're browsing.

- **Off by default**, and it also needs a **Custom Music Folder** set (Settings → Setup) — with the feature turned on but no folder set, nothing will play.
- Folder layout: place tracks in the root of your music folder for general playback, or in `systems/<system-short-name>/` subfolders for music specific to one system. If a system has no dedicated folder, general music plays instead.
- Supported formats: MP3, OGG, FLAC, WAV, M4A, AAC, OPUS.
- Track selection is always random — there's no shuffle toggle because random is the only mode, and no sequential/ordered playback option.
- **Play while browsing systems** — default on
- **Play while browsing games** — default on
- **Play during screensaver** — default off
- Music **never** plays during actual gameplay.
- **During Video Playback** (ducking): Unchanged, Lower volume (20%, default), or Pause — controls what happens to music while a video is playing.
- There is no music volume slider — only the ducking behavior above affects volume.
- The current track's title is shown briefly in a floating card when it changes (the filename minus its extension, not audio metadata) — there's no setting to hide this.

Configured in **Settings → Background Music** (plus the Custom Music Folder, which lives in **Settings → Setup**).

---

## Settings Reference

Settings isn't a dedicated screen — it's a popup reached by long-pressing anywhere on the main screen (or tapping a corner button assigned to Settings, if any; see [Screen Gestures](#screen-gestures) and [Floating Action Buttons](#floating-action-buttons)). This section describes every setting, organized by the category it appears under in that popup, in the order they're listed there.

Below the category list itself, a **Quit Companion App** row closes the app outright (equivalent to what "Close Companion App on ES-DE Quit" does automatically — see [Other Settings](#other-settings)) — useful if you want to exit manually without waiting for ES-DE to quit. Tapping it shows a "Really Quit?" confirmation prompt first, so it's not one accidental tap away from closing the app.

### Widgets

| Setting | Control | Default |
|---|---|---|
| Edit Widgets | Opens the widget editor | n/a |

Tapping this category in the Settings popup jumps straight to the widget editor rather than showing a subpage, since there's nothing else here. Per-widget options (scale mode, transitions, glint, pan & zoom, blur/darken, colors, text size) are configured from inside the editor itself, not from this Settings category — see [Widget Overlay System](#widget-overlay-system).

### UI Settings

| Setting | Control | Options | Default |
|---|---|---|---|
| Theme | Segmented control | Auto / Light / Dark | Auto |
| Overlay Opacity | Slider | 0-100% | 80% — applies to the App Drawer, App Dock, and other overlay surfaces |
| Floating Action Buttons | Four dropdowns, one per corner | Music / Settings / Manual / App Drawer / App / Clock / System Status / Clock & System Status / None (Music/Clock/System Status/Clock & System Status offered only in the top corners) | Top Left: Music, Top Right: Settings, Bottom Left: None, Bottom Right: Manual |
| Screensaver Screen Behavior | Dropdown | On / Dim / Off | On |
| Screensaver Dimming Amount | Slider (shown only when Screensaver Screen Behavior is Dim) | 0-100% | 50% |
| Game Playing Screen Behavior | Segmented control | On / Dim / Off / Manual | On |
| Game Playing Dimming Amount | Slider (shown only when Game Playing Screen Behavior is Dim) | 0-100% | 50% |
| Game Launch Override | Opens the system/game browser, plus a This Screen/Other Screen display picker | See [Game Launch Override](#game-launch-override) | This Screen |

`Screen Behavior` options: **On** leaves the screen as normal; **Dim** overlays a translucent black scrim (touches still pass through), with its darkness set independently by the matching Dimming Amount slider; **Off** shows an opaque black cover and blocks touches except a double-tap to restore (the same cover the manual double-tap-to-blank gesture uses — see [Screen Gestures](#screen-gestures)); **Manual** (Game Playing only) shows the game's manual PDF instead.

Image/logo transitions, Logo Glint, and Pan & Zoom are no longer set here — they're configured per widget from the Configure Widget dialog. See [Per-Widget Configuration](#per-widget-configuration).

See [Floating Action Buttons](#floating-action-buttons) for what each corner option does, the Music-in-top-corners-only rule, and how the App option's app picker works.

### App Drawer and Dock

| Setting | Control | Options | Default |
|---|---|---|---|
| Manage Apps | Opens a checklist of every installed app | Checked = visible in App Drawer | All visible |
| Grid columns | Slider | 3-6 | 5 |
| Sort folders on top of apps | Toggle | On/Off | On |
| Show Search Bar | Toggle | On/Off | On |
| Enable Dock | Toggle | On/Off | Off |
| Maximum dock apps | Slider (shown only if dock enabled) | 2-5 | 5 |
| Dock size | Segmented control (shown only if dock enabled) | Small / Medium / Large | Medium |

`Sort folders on top of apps`: when on, folders are grouped ahead of all ungrouped apps; when off, folders and apps are interleaved into one alphabetical list by name/label. See [Folders](#folders).

`Show Search Bar`: shows or hides the search field and Android/App Settings shortcut buttons above the drawer grid. Turning it off clears any in-progress search query. See [Using the App Drawer](#using-the-app-drawer).

### Background Music

| Setting | Control | Options | Default |
|---|---|---|---|
| Background Music | Toggle | On/Off | Off |
| Play while browsing systems | Toggle (shown only if enabled) | On/Off | On |
| Play while browsing games | Toggle (shown only if enabled) | On/Off | On |
| Play during screensaver | Toggle (shown only if enabled) | On/Off | Off |
| During Video Playback | Segmented control (shown only if enabled) | Unchanged / Lower volume / Pause | Lower volume |

> **Note:** With Background Music enabled but no Custom Music Folder set (Settings → Setup), nothing will play. See [Video & Music](#video--music).

### Other Settings

| Setting | Control | Default |
|---|---|---|
| Check for Updates | Runs a manual update check; shows "Up to date," "Update available," or "Check failed" | n/a |
| Close Companion App on ES-DE Quit | Toggle | Off |
| Launch ES-DE on Companion App Start | Toggle | Off |
| Debug Logging | Toggle | Off |

**Check for Updates** is the top row of this category — see [In-App Updates](#in-app-updates) for what happens when an update is found. **Close Companion App on ES-DE Quit** closes ES-DE Companion when ES-DE fires its quit event. **Launch ES-DE on Companion App Start** launches ES-DE on the other display as soon as Companion starts up — useful alongside Companion's own boot auto-start for bringing both apps up together with no manual step. **Debug Logging** writes an opt-in diagnostic log to help track down reported issues — see [Debug Logging](#debug-logging).

There's no "Show Settings Button" toggle anymore — Settings visibility on the main screen is controlled per corner via [Floating Action Buttons](#floating-action-buttons) instead (assign a corner to Settings, or to None to hide it from a given corner). Long-press always opens Settings regardless.

### Setup

| Setting | Control | Default |
|---|---|---|
| ES-DE folder | Required folder picker | `/storage/emulated/0/ES-DE` |
| Media folder | Required folder picker | `/storage/emulated/0/ES-DE/downloaded_media` |
| Custom System Images Folder | Optional folder picker | Not set |
| Custom Logos Folder | Optional folder picker | Not set |
| Custom Music Folder | Optional folder picker | Not set |
| Backup & Restore | Export Backup / Restore Backup buttons | n/a |

**Backup & Restore** is the bottom row of this category — see [Backup & Restore](#backup--restore) for the full export/restore flow.

---

## File Structure

Understanding where ES-DE Companion reads files from.

### Default Paths

```
/storage/emulated/0/
└── ES-DE/
    ├── logs/
    │   └── es_log.txt              # Written by ES-DE itself; read by the companion app
    ├── settings/
    │   └── es_settings.xml         # ES-DE's own settings file; checked during onboarding
    └── downloaded_media/           # ES-DE's scraped media
        └── {system}/               # Per-system folders
            ├── marquees/
            ├── covers/
            ├── 3dboxes/
            ├── miximages/
            ├── screenshots/
            ├── fanart/
            ├── titlescreens/
            ├── backcovers/
            ├── physicalmedia/
            ├── manuals/
            └── videos/
```

Custom System Images, Custom Logos, and Custom Music folders are wherever you point them in Settings → Setup — they have no default location.

### Media Subfolders

| Subfolder | Content | Widget Type |
|-----------|---------|-------------|
| `marquees/` | Arcade-style marquee art | Marquee |
| `covers/` | Front box covers | Box Cover |
| `3dboxes/` | 3D box art | 3D Box |
| `miximages/` | ES-DE composite images | Mix Image |
| `screenshots/` | In-game screenshots | Screenshot |
| `fanart/` | High-quality backgrounds | Fan Art |
| `titlescreens/` | Title screens | Title Screen |
| `backcovers/` | Back box covers | Box Back Cover |
| `physicalmedia/` | Disc/cartridge art | Physical Media |
| `videos/` | Game videos | (video playback, not a widget) |
| `manuals/` | Game manual PDFs | Used by the "Manual" Game Playing Screen Behavior option and the Manual floating action button, not a widget |

### Custom Media

**Custom System Images:**
- Override the random game art shown in System View
- Name after the system: `snes.webp`, `arcade.png`, etc.
- Formats: PNG, JPG, JPEG, WEBP, GIF

**Custom Logos:**
- Override the 207 built-in system logos
- Name after the system: `snes.svg`, `snes.png`, etc.

**Custom Music:**
- Root of the folder = general playback; `systems/<system-short-name>/` subfolders = per-system playback
- Formats: MP3, OGG, FLAC, WAV, M4A, AAC, OPUS

---

## How Log Events Work

ES-DE Companion doesn't write anything — it only reads `es_log.txt`, which ES-DE writes itself once the three settings from onboarding are enabled. Each of the following events updates what the companion screen shows:

| Event | What it means | What you see |
|---|---|---|
| System selected | You browsed to a system in the carousel | System View canvas, that system's widgets |
| Game selected | You highlighted a specific game | Game View canvas, that game's widgets |
| Game started | A game actually launched | Game View canvas, screen behavior follows "Game Playing Screen Behavior" |
| Game ended | You returned from a game to ES-DE | Back to whichever system/game you were browsing |
| Screensaver started | ES-DE's screensaver activated | Screen behavior follows "Screensaver Screen Behavior"; whatever was showing before is remembered |
| Screensaver game shown | The screensaver is cycling through a game preview | Game View canvas updates to that game, if the screensaver shows one |
| Screensaver ended | The screensaver deactivated | Restores whatever was active immediately before the screensaver started |
| ES-DE quit | ES-DE is shutting down | Companion screen returns to idle (no widget canvas shown) |

If nothing has happened yet since the companion app started, it stays idle and shows no widget canvas until the first event arrives.

---

## In-App Updates

ES-DE Companion checks GitHub for newer releases itself — no need to check the Releases page manually.

- **Automatic check**: a silent check runs in the background every time the app starts. If a newer version is available, an "Update available" dialog appears on its own, showing that release's notes.
- **Manual check**: Settings → Other Settings → Check for Updates (the top row of that category) runs the same check on demand, showing "Up to date," "Update available," or "Check failed" next to it once it completes.
- **Installing**: confirming "Download & Install" in the dialog requires granting Android's "Install unknown apps" permission for ES-DE Companion the first time — the dialog walks you through the permission prompt, then downloads the APK and opens the system installer automatically once it finishes.
- **"What's new"**: separately from the update dialog, the first time the app starts after actually being updated, a one-time "What's new" dialog shows that version's release notes — so you still see them even if you installed the update some other way (e.g. sideloading a new APK manually) rather than through the in-app prompt.

---

## Backup & Restore

Settings → Setup → "Backup & Restore" lets you export your entire configuration to a file and bring it back with one restore — useful before a reinstall, a factory reset, or setting up a replacement device the same way.

### What Gets Backed Up

Everything this app persists as a user setting:

- Setup folder paths (ES-DE folder, Media folder, Custom System Images/Logos/Music folders)
- UI Settings (theme, overlay opacity, Screen Behavior, Floating Action Button assignments)
- Background Music settings (Video is per-widget now, backed up as part of the widget canvases below)
- Other Settings (Close Companion App on ES-DE Quit, Launch ES-DE on Companion App Start, Debug Logging)
- App Drawer and Dock settings — hidden apps, grid columns, other-screen launch preferences, folders, and Dock configuration
- Game Launch Override — system defaults, per-game overrides, the This Screen/Other Screen display target, and the Close App on Game End toggle
- Both widget canvases (System View and Game View), including every placed widget and its per-widget configuration
- Thor Settings, on supported hardware (AYN Thor)

Not included: the onboarding-complete flag, the "what's new" last-seen-version marker, and other purely internal bookkeeping that isn't really a user setting.

### Exporting

1. Go to Settings → Setup and tap **Export Backup**
2. Pick where to save the file and what to name it — this uses Android's standard "Save As" file picker, so it works with local storage, cloud-backed providers, or anywhere else the picker offers
3. The file is a plain JSON file — safe to inspect, rename, or move around

### Restoring

1. Go to Settings → Setup and tap **Restore Backup**
2. Pick a previously exported file
3. Confirm — restoring **overwrites every current setting** with what's in the file, so review the confirmation prompt before continuing
4. Settings update immediately; no restart required

### Things to Know

- Restoring is a full overwrite of everything a backup covers, not a selective merge — you can't restore just one setting from a file
- Folder paths and package names (hidden apps, Dock apps, App Drawer folders) restore exactly as saved. If you're restoring onto a different device or after uninstalling an app that was referenced, that reference just has no effect rather than causing an error — the same as if you'd removed it normally
- A backup made by a newer version of the app is rejected if restored into an older version, with a clear error, rather than silently applying only part of it

---

## Advanced Topics

### Debug Logging

An opt-in diagnostic log (Settings → Other Settings → Debug Logging, off by default) to help track down reported issues without needing a live repro session.

- **Where it's written**: `ES-DE Companion/logs/esde_companion_log.txt`, in a top-level `ES-DE Companion` folder on the same storage root as your `ES-DE` folder — not nested inside it, and not affected by whatever custom ES-DE folder path you've configured. Requires the same "All files access" permission the app already needs to work at all.
- **What it records**: parsed app-state transitions, media resolution outcomes (found/not found, including the exact path checked), and music/video playback start and error events — each as its own timestamped line.
- **Truncated on each start**: like ES-DE's own `es_log.txt`, the file is cleared and rewritten fresh the first time something is logged after the app starts with the toggle on — it isn't an ever-growing history across app restarts.
- Turning the toggle off stops new writes immediately but doesn't delete whatever's already in the file.

Attach this file when reporting a bug that's hard to reproduce from a description alone — see [How do I report a bug?](FAQ.md#how-do-i-report-a-bug) in the FAQ.

### Version History

See the [Releases page](https://github.com/RobZombie9043/es-de-companion/releases) for release notes and version history.

---
