# ES-DE Companion - User Guide

This comprehensive guide covers everything you need to know about ES-DE Companion, from initial setup to advanced customization.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Quick Setup Wizard](#quick-setup-wizard)
3. [Widget Overlay System](#widget-overlay-system)
4. [Display Configuration](#display-configuration)
5. [App Drawer](#app-drawer)
6. [Settings Reference](#settings-reference)
8. [File Structure](#file-structure)
9. [Advanced Topics](#advanced-topics)

---

## Getting Started

### What is ES-DE Companion?

ES-DE Companion is a secondary screen application that displays game artwork, videos, and customizable widgets while you browse and play games in ES-DE on your primary screen. It's designed specifically for dual-screen Android devices.

### Requirements

**Hardware:**
- Dual-screen Android device (Ayn Thor, Ayaneo Pocket DS, Anbernic RG DS etc.)
- Android 10 or higher (API level 29+)

**Software:**
- ES-DE installed and configured
- Game media downloaded (scraped) in ES-DE
- Approximately 50MB free storage for app and logs

**Permissions:**
- Storage access (for reading media and creating scripts)

### Installation

1. **Download the APK**
   - Visit the [Releases page](https://github.com/rkr87/es-de-companion/releases)
   - Download the latest APK file

2. **Install the App**
   - Enable "Install from Unknown Sources" if prompted
   - Open the downloaded APK
   - Follow Android's installation prompts

3. **Launch ES-DE Companion**
   - Open the app on your secondary screen
   - The Quick Setup wizard will start automatically

4. **Enable Scripts in ES-DE**
   - Open ES-DE on your primary screen
   - Press START to open the main menu
   - Navigate to: Other Settings
   - Toggle ON: "Custom Event Scripts"
   - Toggle ON: "Browsing Custom Events"
   - Restart ES-DE if needed

### First Launch

On first launch, ES-DE Companion will:
1. Display the Quick Setup wizard
2. Request storage permissions
3. Guide you through path configuration
4. Create ES-DE integration scripts
5. Show a tutorial on using the widget system

**Tip:** Keep both screens visible during setup to easily verify the connection between ES-DE and the companion app.

---

## Quick Setup Wizard

The Quick Setup wizard configures ES-DE Companion in five steps:

### Step 1: Storage Permissions

The app needs storage access to:
- Create integration scripts in ES-DE's scripts folder
- Read game artwork from ES-DE's media folder
- Write log files for event tracking

**Android 11+:** You'll be directed to system settings to grant "All Files Access"  
**Android 10:** Standard storage permission request will appear

**After granting permission:** The wizard automatically continues to the next step.

### Step 2: ES-DE Scripts Folder

Select where ES-DE's scripts folder is located.
This must be inside the ~/ES-DE application data directory that you have setup ES-DE to use for hte scripts to function.

**Default Path:** `/storage/emulated/0/ES-DE/scripts`

**Options:**
- **Use Default:** Uses the standard internal storage path
- **Select Folder:** Browse to custom location (if ES-DE is on SD card or custom path)

**What this does:** Tells the app where to create the integration scripts that connect ES-DE to the companion app.

### Step 3: Create Script Files

The app creates 7 integration scripts:

| Script | Purpose |
|--------|---------|
| `esdecompanion-game-end.sh` | Game exits |
| `esdecompanion-game-start.sh` | Game launches |
| `esdecompanion-game-selected.sh` | Game selected in list |
| `esdecompanion-system-selected.sh` | System selected in carousel |
| `esdecompanion-screensaver-start.sh` | Screensaver activates |
| `esdecompanion-screensaver-end.sh` | Screensaver deactivates |
| `esdecompanion-screensaver-game-select.sh` | Screensaver shows game (slideshow mode) |

**Options:**
- **Create Scripts:** Generates all 7 script files
- **Skip:** Only if scripts already exist from a previous setup

**What happens:** Scripts are written to the scripts folder and validated.

### Step 4: Downloaded Media Folder

Select where ES-DE stores downloaded game media.

**Default Path:** `/storage/emulated/0/ES-DE/downloaded_media`

**Options:**
- **Use Default:** Standard ES-DE media location
- **Select Folder:** Browse to custom location

**What this does:** Tells the app where to find game artwork (fanart, screenshots, marquees, videos, etc.)

### Step 5: Enable Scripts in ES-DE

Final step requires action in ES-DE:

1. Open ES-DE on your primary screen
2. Press START
3. Go to: Other Settings
4. Enable: "Custom Event Scripts"
5. Enable: "Browsing Custom Events"

**What this does:** Tells ES-DE to call the integration scripts when you browse games, launch games, and activate screensavers.

### Completion

After setup completes:
- A tutorial dialog explains the widget system
- You're returned to the main companion screen
- Browse a game in ES-DE to test the connection

**Tip:** If images don't appear, wait 10-20 seconds if ES-DE is on an SD card (Android needs time to mount external storage).

---

## Widget Overlay System

Widgets are customizable overlay elements that display game and system artwork on top of the background image.

### Widget Overview

**What are widgets?**
- Draggable, resizable display elements
- Show different types of game/system artwork
- Can be layered (z-index control)
- Context-aware (separate layouts for system vs game views)
- Locked by default to prevent accidental changes

**Widget Contexts:**
- **System Widgets:** Active when browsing the system carousel (SNES, Genesis, etc.)
- **Game Widgets:** Active when browsing games within a system

### Available Widget Types

#### System View Widgets

| Widget Type | Description |
|------------|-------------|
| **System Logo** | Shows current system's logo (built-in or custom) |

#### Game View Widgets

| Widget Type | Description | Media Folder |
|------------|-------------|--------------|
| **Marquee** | Arcade-style marquee artwork | `marquees/` |
| **2D Box** | Front cover of game box | `covers/` |
| **3D Box** | 3D perspective box art | `3dboxes/` |
| **Mix Image** | ES-DE composite (screenshot + metadata) | `miximages/` |
| **Screenshot** | In-game screenshot | `screenshots/` |
| **Fanart** | High-quality background artwork | `fanart/` |
| **Title Screen** | Game's title screen | `titlescreens/` |
| **Back Cover** | Back of game box | `backcovers/` |
| **Physical Media** | Disc/cartridge artwork | `physicalmedia/` |
| **Game Description** | Scrollable text description | (from gamelist.xml) |

### Widget Edit Mode

Widgets are **locked by default** to prevent accidental changes during normal use.

**To unlock widgets:**
1. Long-press anywhere on the companion screen
2. Widget menu appears
3. Toggle "Widget Edit Mode: OFF" to ON
4. Border turns purple and widgets become editable

**To lock widgets again:**
1. Long-press to open widget menu
2. Toggle "Widget Edit Mode: ON" to OFF
3. Prevents accidental dragging or resizing

### Creating Widgets

**Prerequisites:**
1. Widget Edit Mode must be ON
2. Browse to system view (for system logo) or game view (for game widgets)

**Steps:**
1. Long-press screen to open widget menu
2. Tap "Add Widget"
3. Select widget type from the list
4. Widget appears in center of screen
5. Drag to position, resize from corners

**Validation:**
- If no artwork exists for the selected type, you'll see a message
- Game Description widget works even without image files (uses gamelist.xml data)
- System Logo widget works for all ES-DE supported systems (has built-in logos)

### Editing Widgets

**Selecting Widgets:**
- Tap a widget to select it
- Selected widget shows green border
- Only one widget can be selected at a time
- Tap elsewhere to deselect

**Moving Widgets:**
1. Select a widget
2. Drag from anywhere inside the widget
3. Drop at new location
4. If snap-to-grid is ON, position snaps to grid

**Resizing Widgets:**
1. Select a widget
2. Corner handles (⌙ shape) appear
3. Drag a corner handle to resize
4. Maintains aspect ratio
5. If snap-to-grid is ON, size snaps to grid

**Deleting Widgets:**
1. Select a widget
2. Tap the 🗑 button in top-right corner
3. Confirm deletion in dialog

### Grid System

The grid system helps align widgets precisely.

**Snap to Grid:**
- When ON: Widgets automatically align to grid points
- Applies to position and size
- Makes consistent layouts easier

**Show Grid:**
- When ON: Visual grid overlay appears
- Helps visualize alignment

**How to use:**
1. Long-press screen → Widget menu
2. Toggle "Snap to Grid"
3. Toggle "Show Grid" to see grid lines
4. Tap grid size icon to change size
5. Widgets snap to nearest grid point when moved/resized

### Widget Settings

Access from widget settings menu.
1. Select a widget
2. Tap the 🔧 settings button

**Layer Control:**
1. Select a widget
2. Tap the 🔧 settings button
3. Choose layer option:
   - **Move Forward:** Move up one layer
   - **Move Backward:** Move down one layer

**Image Scaling (per widget):**
- **Fit:** Image fits inside the widget container, maintains aspect ratio but may have empty space
- **Fill:** Image fills widget container completely, may crop edges

**Background Opacity (Game Description widget only):**
- Text background transparency adjustable

**Delete Widget:**
- Deletes the currently selected widget

---

## App Drawer

The app drawer provides full access to all installed Android apps directly from the companion screen.

### Opening the App Drawer

**Method:** Swipe up anywhere on the companion screen

**Visual:** Drawer slides up from bottom, showing app grid

**Closing:** Swipe down on app drawer or tap the back button

### Using the App Drawer

**Launching Apps:**
- Single tap any app to launch it
- App opens based on saved preference (this screen or other screen)

**Searching Apps:**
- Tap search icon (magnifying glass) at top
- Type app name
- Results filter in real-time
- Shows hidden apps in search results

### App Management

**Long-Press Menu:**

Long-press any app to see options:
- **Launch on this screen:** Opens app on companion screen
- **Launch on other screen:** Opens app on primary screen
- **App Info:** Opens Android's app info screen
- **Hide App:** Hides app from drawer (still searchable)
- **Unhide App:** (for hidden apps in search) Makes app visible again

**Launch Preferences:**
- Last-used launch location is saved per-app
- Small dot indicator shows apps set to "other screen"
- Default is "this screen" for new apps

**Hidden Apps:**
- Appear in search with "H" hidden badge indicator
- Can be unhidden via long-press menu
- Manage all hidden apps in Settings

---

## Settings Menu Guide

This section describes every setting in ES-DE Companion's Settings menu, organized in the order they appear in the app.

### Accessing Settings

**From Main Screen:**
- Swipe up to open the app drawer
- Tap the menu icon (three horizontal lines) in top-right corner

### Quick Setup

**🚀 Run Quick Setup** (Button)
- **Purpose**: Re-run the complete setup wizard
- **When to use**: 
  - Scripts need to be regenerated
  - Paths have changed
  - Starting fresh configuration
- **What it does**: Walks through all setup steps (permissions, scripts, media paths)

---

### 1. MEDIA SETTINGS

This section controls how images and logos are displayed.

#### System View Background Image
- **Purpose**: Choose background type when browsing systems
- **Options** (chip selection):
  - **Fanart**: Shows random game fanart from current system
  - **Screenshot**: Shows random game screenshot from current system
  - **Custom Image**: Uses Custom Background Image if set
  - **Solid Color**: Uses selected solid color
	- **Color Picker** (appears when Solid Color selected):
		- Tap color button to select custom color
- **Default**: Fanart

#### Game View Background Image
- **Purpose**: Choose background type when browsing games
- **Options** (chip selection):
  - **Fanart**: Shows random game fanart from current system
  - **Screenshot**: Shows random game screenshot from current system
  - **Custom Image**: Uses Custom Background Image if set
  - **Solid Color**: Uses selected solid color
	- **Color Picker** (appears when Solid Color selected):
		- Tap color button to select custom color
- **Default**: Fanart

#### Video Playback
- **Purpose**: Enable/disable video playback
- **Options** (chip selection):
  - **Off**: Only show images
  - **On**: Videos play when available
- **Default**: Off

#### Video Settings Panel
(Only visible when Video Playback is On)

##### Video Delay
- **Purpose**: Time before switching from image to video
- **Control**: Slider (0-5 seconds)
- **Values**:
  - 0 = Instant (immediate video)
  - 1-5 = Delay in seconds
- **Default**: 2 seconds

##### Video Audio
- **Purpose**: Enable or disable sound during video playback
- **Options** (chip selection):
  - **Off**: No video audio
  - **On**: Video audio plays
- **Default**: Off

#### Enable Background Music
- **Purpose**: Master toggle for backgorund music system
- **Options** (chip selection):
  - **Off**: No music playback
  - **On**: Music system active
- **Default**: Off

#### Music Settings Panel
(Only visible when Enable Background Music is On)

##### System View Music
- **Purpose**: Music when browsing system view
- **Options** (chip selection):
  - **Off**: Silent in system view
  - **On**: Music plays in system view
- **Default**: On

##### Game View Music
- **Purpose**: Music when browsing games within a system
- **Options** (chip selection):
  - **On**: Music plays in game view
  - **Off**: Silent in game view
- **Default**: On

##### Screensaver Music
- **Purpose**: Music during ES-DE screensaver
- **Options** (chip selection):
  - **Off**: Music stops during screensaver
  - **On**: Music continues during screensaver
- **Default**: Off

##### Music During Companion Videos
- **Purpose**: How music behaves when ES-DE Companion videos are playing
- **Options** (chip selection):
  - **Continue**: Music stays at 100% volume
  - **Lower (20%)**: Music reduces to 20% volume
  - **Pause**: Music pauses completely
- **Default**: Lower (20%)
- **Note**: Only affects music during videos played by companion app, not ES-DE videos as they are not able to be detected 

##### Display Song Titles
- **Purpose**: Show currently playing song on screen
- **Options** (chip selection):
  - **Off**: No song title display
  - **On**: Song titles display at bottom of screen 
- **Default**: On

##### Song Title Settings Panel
(Only visible when Display Song Titles is On)

###### Display Duration
- **Purpose**: How long song titles remain visible
- **Control**: Slider
- **Values**:
  - 2-30 seconds (specific durations)
  - Infinite (song title does not hide)
- **Default**: 8 seconds

###### Background Opacity
- **Purpose**: Transparency of the song title background
- **Control**: Slider (0-100%)
- **Default**: 70%
- **Values**:
  - 0% = Fully opaque (solid black)
  - 50% = Semi-transparent
  - 100% = Fully transparent

#### Animation Style
- **Purpose**: Transition effect when images change
- **Options** (chip selection):
  - **None**: Instant change (best performance)
  - **Fade**: Cross-fade transition
  - **Scale + Fade**: Scale from small + fade in
  - **Custom**: Adjustable timing and scale
- **Default**: Fade

#### Custom Animation Settings Panel
(Only visible when Animation Style is Custom)

##### Animation Duration
- **Purpose**: How long animation takes
- **Control**: Slider (100-2000ms)
- **Default**: 500ms (0.5 seconds)
- **Values**:
  - 100ms = Very fast
  - 500ms = Balanced
  - 2000ms = Very slow
- **Display**: Shows duration in milliseconds

##### Animation Scale
- **Purpose**: Starting size for scale animation
- **Control**: Slider (80-100%)
- **Default**: 90%
- **Values**:
  - 80% = Starts very small
  - 90% = Subtle zoom
  - 100% = No scaling
- **Display**: Shows percentage
- **Note**: Only affects Scale + Fade and Custom styles

---

### 2. VISUAL EFFECTS

This section controls visual overlays.

#### Dimming
- **Purpose**: Darken background images
- **Control**: Slider (0-100%)
- **Default**: 25%
- **Values**:
  - 0% = No dimming (full brightness)
  - 50% = Moderate darkening
  - 100% = Fully Black
- **Display**: Shows percentage
- **Use case**: Help widgets stand out from background images

#### Blur (Android 12+)
- **Purpose**: Blur background images
- **Control**: Slider (0-25)
- **Default**: 0 (no blur)
- **Values**:
  - 0 = Sharp (no blur)
  - 15 = Moderate blur
  - 25 = Maximum blur
- **Display**: Shows blur amount
- **Performance**: GPU-intensive, may impact performance
- **Compatibility**: Requires Android 12 or higher

---

### 3. APPLICATION BEHAVIOR

This section controls app behavior during different ES-DE states.

#### Game Launch Display
- **Purpose**: What shows on companion screen during gameplay
- **Options** (chip selection):
  - **Black**: Plain black screen (minimal distraction)
  - **Game Image**: Launched game's artwork + game view widgets
  - **Custom Image**: Custom background + game view widgets
- **Default**: Game Image
- **Note**: Videos never play during gameplay

#### Screensaver Display
- **Purpose**: What shows during ES-DE screensavers
- **Options** (chip selection):
  - **Black**: Plain black screen
  - **Game Image**: Launched game's artwork + game view widgets
  - **Custom Image**: Custom background + game view widgets
- **Default**: Game Image
- **Note**: Videos never play during screensavers

#### Double-Tap Black Overlay
- **Purpose**: Enable double-tap gesture for manual black screen
- **Options** (chip selection):
  - **Off**: Double-tap does nothing
  - **On**: Double-tap shows/hides black overlay
- **Default**: Off
- **Gesture**: Double-tap anywhere on companion screen
- **Use case**: Quickly hide content when needed and show black screen, mimics screen off on OLED displays

---

### 4. APP DRAWER

This section configures the built-in app launcher.

#### App visibility
- **Purpose**: Choose which apps appear in the app drawer
- **Control**: **Manage Apps** button → opens list
- **What it shows**: 
  - List of all apps
  - Checkboxes to hide and unhide apps
  - **Save** button to apply changes
- **Use case**: Hide or restore hidden apps

#### Drawer Opacity
- **Purpose**: Background opacity of app drawer
- **Control**: Slider (0-100%)
- **Default**: 70%
- **Values**:
  - 0% = Fully opaque (solid background)
  - 50% = Semi-transparent
  - 100% = Fully transparent
- **Display**: Shows percentage
- **Use case**: Higher transparency lets background image show through when viewing app drawer

#### Grid Columns
- **Purpose**: Adjust number of columns in app drawer
- **Control**: Slider (2-8 columns)
- **Default**: 4 columns

---

### 5. PATHS

This section configures where ES-DE Companion finds media and creates scripts.

#### Downloaded Media Path
- **Default**: `/storage/emulated/0/ES-DE/downloaded_media`
- **Purpose**: Location of ES-DE's scraped game artwork
- **Controls**:
  - Path display showing current location
  - Green ● = Valid path with media found
  - Red ● = Invalid or empty path
  - **Select Directory** button to browse to custom location
- **Notes**: Must match ES-DE's game media folder setting

#### ES-DE Scripts
- **Default**: `/storage/emulated/0/ES-DE/scripts`
- **Purpose**: Location where integration scripts are created
- **Controls**:
  - Path display
  - Script status indicator:
    - Green ● "✓ All 7 scripts valid"
    - Yellow ● "⚠ X outdated" or "X/7 valid"
    - Red ● "✗ Missing or invalid scripts"
  - **Select Directory** button
  - **Create Scripts** button
- **Important**: Must match ES-DE's scripts folder
- **Troubleshooting**: If indicator is yellow/red, tap Create Scripts

#### System Images Path (Optional)
- **Default**: `/storage/emulated/0/ES-DE Companion/system_images`
- **Purpose**: Optional folder for custom system background images
- **Controls**:
  - Path display
  - **Select Directory** button to define your own custom path to use
- **Optional**: Only needed if you want custom system backgrounds. These will override the random game images shown when scrolling systems. 
- **Supported formats**: WEBP, PNG, JPG, JPEG, GIF

#### System Logos Path
- **Default**: `/storage/emulated/0/ES-DE Companion/system_logos`
- **Purpose**: Optional folder for custom system logos
- **Controls**:
  - Path display
  - **Select Directory** button to define your own custom path to use
- **Optional**: Only needed if you want to override built-in system logos.
- **Supported formats**: WEBP, PNG, JPG, JPEG, GIF

#### Custom Background Image (Optional)
- **Default**: None (uses built-in image)
- **Purpose**: Custom image to display when Custom Image is set. Also used as a fallback image when no system or game artwork is available
- **Controls**:
  - Path display or "No custom background set"
  - Status indicator
  - **Select Image** button (image picker)
  - **Clear** button
- **Use case**: Set custom background image
- **Supported formats**: WEBP, PNG, JPG, JPEG, GIF

##### Music Path (Optional)
- **Default**: `/storage/emulated/0/ES-DE Companion/music`
- **Purpose**: Location of music files for background music
- **Control**: 
  - Path display or "No folder selected"
  - **Select Directory** button (folder picker)
- **Supported formats**: MP3, OGG, FLAC, M4A, WAV, AAC
- **Structure**: 
  - Place MP3s in root folder for all systems
  - Or create `music/systems/{systemname}/` subfolders for per-system music

---

#### App Version
- **Purpose**: Display current app version
- **Display**: "Version X.Y.Z" (e.g., "Version 0.5.1")
- **Format**: Standard semantic versioning
- **Note**: Read-only, for reference only

---

## File Structure

Understanding where ES-DE Companion stores and accesses files.

### Default Paths

```
/storage/emulated/0/
├── ES-DE/
│   ├── downloaded_media/          # ES-DE scraped media
│   │   ├── {system}/              # Per-system folders
│   │   │   ├── marquees/
│   │   │   ├── covers/
│   │   │   ├── 3dboxes/
│   │   │   ├── miximages/
│   │   │   ├── screenshots/
│   │   │   ├── fanart/
│   │   │   ├── titlescreens/
│   │   │   ├── backcovers/
│   │   │   ├── physicalmedia/
│   │   │   └── videos/
│   │   ├── system_images/         # Custom system backgrounds
│   │   └── system_logos/          # Custom system logos
│   └── scripts/                   # ES-DE integration scripts
│       ├── esdecompanion-game-start.sh
│       ├── esdecompanion-game-end.sh
│       ├── esdecompanion-game-selected.sh
│       ├── esdecompanion-system-selected.sh
│       ├── esdecompanion-screensaver-start.sh
│       ├── esdecompanion-screensaver-end.sh
│       └── esdecompanion-screensaver-game-select.sh
└── ES-DE Companion/
    └── logs/                      # Event log files
        ├── system_scroll.log
        ├── game_scroll.log
        ├── game_launch.log
        ├── game_end.log
        ├── screensaver_start.log
        ├── screensaver_end.log
        └── screensaver_game_select.log
```

### Media Folders

Each game system has its own folder under `downloaded_media/`:

**Examples:**
- `snes/` - Super Nintendo Entertainment System
- `genesis/` - Sega Genesis / Mega Drive
- `arcade/` - Arcade (MAME, FinalBurn Neo, etc.)
- `psx/` - Sony PlayStation
- `n64/` - Nintendo 64

**Subfolder Types:**

| Subfolder | Content | Widget Type |
|-----------|---------|-------------|
| `marquees/` | Arcade-style marquee art | Marquee |
| `covers/` | Front box covers | 2D Box |
| `3dboxes/` | 3D box art | 3D Box |
| `miximages/` | ES-DE composite images | Mix Image |
| `screenshots/` | In-game screenshots | Screenshot |
| `fanart/` | High-quality backgrounds | Fanart |
| `titlescreens/` | Title screens | Title Screen |
| `backcovers/` | Back box covers | Back Cover |
| `physicalmedia/` | Disc/cartridge art | Physical Media |
| `videos/` | Game videos | (video playback) |

### Custom Media

**System Images** (`ES-DE Companion/system_images/`):
- Override random game art in system view
- Name after system: `snes.webp`, `arcade.png`, etc.
- Formats: WEBP, PNG, JPG, JPEG, GIF

**System Logos** (`ES-DE Companion/system_logos/`):
- Override built-in system logos
- Name after system: `snes.svg`, `snes.png`, etc.
- Formats: SVG, WEBP, PNG, JPG, JPEG, GIF
- Falls back to PNG/WEBP if SVG not available

### Script Files

Integration scripts connect ES-DE to the companion app:

| Script File | Triggered When |
|------------|---------------|
| `esdecompanion-game-selected.sh` | Game highlighted in list |
| `esdecompanion-system-selected.sh` | System highlighted in carousel |
| `esdecompanion-game-start.sh` | Game launch begins |
| `esdecompanion-game-end.sh` | Game exits |
| `esdecompanion-screensaver-start.sh` | Screensaver activates |
| `esdecompanion-screensaver-end.sh` | Screensaver deactivates |
| `esdecompanion-screensaver-game-select.sh` | Screensaver shows game (slideshow) |

**Script Content:**
- Bash shell scripts
- Write event data to log files

### Log Files

Log files are written by scripts and read by the companion app:

| Log File | Contains |
|----------|----------|
| `game_scroll.log` | Current game (system, name, file path) |
| `system_scroll.log` | Current system name |
| `game_launch.log` | Launched game info |
| `game_end.log` | Game exit event |
| `screensaver_start.log` | Screensaver type |
| `screensaver_end.log` | Screensaver end event |
| `screensaver_game_select.log` | Screensaver game info |

**Location:** `/storage/emulated/0/ES-DE Companion/logs/`  
**Why internal storage:** FileObserver requires internal storage for reliable monitoring (SD cards not supported)
- Log files monitored by companion app via FileObserver
- Automatic updates when companion app detects changes

---

## Advanced Topics

### Build Variants

ES-DE Companion has two build variants:

**Standard:**
- Normal Android app behavior
- Appears in recent apps menu
- Can be force-closed from recents
- Recommended for most users
- Naming convention - ES-DE-Companion-vx.x.x.apk

**Hidden:**
- Hides from recent apps menu
- Harder to accidentally close
- For users wanting a cleaner setup
- Naming convention - ES-DE-Companion-vx.x.x**h**.apk

---

### Version History

See [CHANGELOG.md](CHANGELOG.md) for detailed version history.

---
