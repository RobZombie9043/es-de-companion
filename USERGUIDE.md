# ES-DE Companion - User Guide

This comprehensive guide covers everything you need to know about ES-DE Companion, from initial setup to advanced customization.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Onboarding Wizard](#onboarding-wizard)
3. [Screen Gestures](#screen-gestures)
4. [Widget Overlay System](#widget-overlay-system)
5. [App Drawer](#app-drawer)
6. [App Dock](#app-dock)
7. [Video & Music](#video--music)
8. [Settings Reference](#settings-reference)
9. [File Structure](#file-structure)
10. [How Log Events Work](#how-log-events-work)
11. [Advanced Topics](#advanced-topics)

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

**Skipped entirely if:** all three are already enabled when this step is reached.

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
| **Long-press** (or tap the Settings gear, if shown) | Opens the Settings/Widgets popup menu — see [Widget Edit Mode](#widget-edit-mode) and [Settings Reference](#settings-reference) |
| **Double-tap** | Manually blanks the screen to black; double-tap again to restore |

The double-tap blank gesture is always available, independent of the automatic Game Playing/Screensaver Screen Behavior settings (see [Settings Reference](#settings-reference)) — it uses the same black cover, so it also restores with a double-tap when triggered automatically by a Screen Behavior set to "Off."

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
| **Description** | Scrollable text description, from `gamelist.xml` | (none — text only) |
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

All of the above is set per widget instance, from that widget's own Configure Widget dialog — there's no global transitions/glint/pan-and-zoom setting in Settings. A freshly-added System Logo or Marquee widget starts with the Slide transition and Logo Glint on; a freshly-added System Image or Fan Art background starts with Pan & Zoom on and 10% darken.

### Widget Edit Mode

**To enter edit mode:**
1. Long-press anywhere on the companion screen (or tap the Settings gear, if shown) to open the Settings popup
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

**Launching apps:**
- **Single tap** launches the app at whichever screen it last launched on (defaults to "this screen" the first time).
- **Double tap** launches it on the *other* screen than the single-tap default, and remembers that as the new preference. If no secondary display is available, this just launches on the current screen instead, without changing the saved preference.
- **Long-press** opens a menu: "Launch on this screen," "Launch on other screen," "App Info," and "Hide App."

Launching any app automatically closes the drawer.

There is no search box in the drawer — it lists every visible installed app in the grid.

### Managing App Visibility

Go to **Settings → App Drawer and Dock → Manage Apps** to see every installed app with a checkbox — unchecking an app hides it from the drawer (and from the dock's add-app picker). There's no in-drawer "unhide" option; that's done from this same Settings screen.

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

## Video & Music

### Video Playback

Optional video playback for the game you're currently browsing.

- **Off by default.**
- Plays **only while browsing a specific game** — never during actual gameplay, never during the screensaver, and never while browsing systems.
- **Video Start Delay**: 0-10 seconds before playback starts (default 3s). Until playback actually begins, the widget canvas underneath remains visible.
- **Video Audio**: on/off toggle for whether the video plays with sound (default on).

Configured in **Settings → Video Playback**.

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

Settings isn't a dedicated screen — it's a popup reached by long-pressing anywhere on the main screen (or tapping the Settings gear, if shown; see [Screen Gestures](#screen-gestures)). This section describes every setting, organized by the category it appears under in that popup, in the order they're listed there.

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
| Game Playing Screen Behavior | Segmented control | On / Dimmed / Off / Manual | On |
| Screensaver Screen Behavior | Segmented control | On / Dimmed / Off | On |

`Screen Behavior` options: **On** leaves the screen as normal; **Dimmed** overlays a translucent black scrim (touches still pass through); **Off** shows an opaque black cover and blocks touches except a double-tap to restore (the same cover the manual double-tap-to-blank gesture uses — see [Screen Gestures](#screen-gestures)); **Manual** (Game Playing only) shows the game's manual PDF instead.

Image/logo transitions, Logo Glint, and Pan & Zoom are no longer set here — they're configured per widget from the Configure Widget dialog. See [Per-Widget Configuration](#per-widget-configuration).

### App Drawer and Dock

| Setting | Control | Options | Default |
|---|---|---|---|
| Manage Apps | Opens a checklist of every installed app | Checked = visible in App Drawer | All visible |
| Grid columns | Slider | 3-6 | 5 |
| Enable Dock | Toggle | On/Off | Off |
| Maximum dock apps | Slider (shown only if dock enabled) | 2-5 | 5 |
| Dock size | Segmented control (shown only if dock enabled) | Small / Medium / Large | Medium |

### Video Playback

| Setting | Control | Options | Default |
|---|---|---|---|
| Background Video | Toggle | On/Off | Off |
| Video Start Delay | Slider (shown only if enabled) | 0-10 seconds | 3 seconds |
| Video Audio | Toggle (shown only if enabled) | On/Off | On |

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
| Close Companion App on ES-DE Quit | Toggle | Off |
| Show Settings Button | Toggle | On |

**Close Companion App on ES-DE Quit** closes ES-DE Companion when ES-DE fires its quit event. **Show Settings Button** controls whether the Settings gear icon appears on the main screen — turning it off doesn't reduce what's reachable, since Settings stays available via long-press regardless (see [Screen Gestures](#screen-gestures)).

### Setup

| Setting | Control | Default |
|---|---|---|
| ES-DE folder | Required folder picker | `/storage/emulated/0/ES-DE` |
| Media folder | Required folder picker | `/storage/emulated/0/ES-DE/downloaded_media` |
| Custom System Images Folder | Optional folder picker | Not set |
| Custom Logos Folder | Optional folder picker | Not set |
| Custom Music Folder | Optional folder picker | Not set |

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
| `manuals/` | Game manual PDFs | Used by the "Manual" Game Playing Screen Behavior option, not a widget |

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

## Advanced Topics

### Version History

See the [Releases page](https://github.com/RobZombie9043/es-de-companion/releases) for release notes and version history.

---
