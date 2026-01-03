# ES-DE Second Screen Companion

![Version](https://img.shields.io/badge/version-0.2.2-blue)
![Android](https://img.shields.io/badge/Android-10%2B-green)

A companion app for [ES-DE](https://es-de.org/) that displays beautiful game artwork and marquees on a secondary display, transforming your dual-screen device into an immersive retro gaming interface.

> **Note**: This is a semi-official companion app designed to enhance your ES-DE experience on dual-screen devices.

> **Note**: This app was developed using AI assistance, the full source code has been made available.

## 🎮 Features

### Dynamic Display
- **Real-time artwork display** - Shows game fanart, screenshots, and marquees as you browse in ES-DE
- **Video playback support** - Play game videos when browsing games
  - Configurable delay (instant to 5 seconds) before video starts
  - Optional audio control (muted by default)
  - Respects animation settings (fade, scale+fade, custom)
  - Videos automatically stop during gameplay and screensavers
- **System view support** - Displays built-in system logos, custom images, or random game artwork when browsing systems
- **Smooth animations** - Configurable fade and scale effects with custom timing options
- **Background customization** - Adjustable dimming and blur effects

### Application Behavior
- **Game Launch Display** - Choose what displays while games are running
  - **Black**: Plain black screen (minimal distraction)
  - **Default Image**: Fallback background with current marquee
  - **Game Image**: Shows the launched game's artwork and marquee
- **Screensaver Display** - Choose what displays during ES-DE screensavers
  - **Black**: Plain black screen
  - **Default Image**: Fallback background with current marquee
  - **Game Image**: Shows screensaver game artwork (slideshow/video modes only)

### App Drawer
- **Full Android app launcher** - Access all your installed apps from one place
- **Smart search** - Quickly find apps with the built-in search bar
- **Customizable grid** - Adjust column count to your preference
- **App visibility control** - Hide apps you don't want to see in the drawer
- **Long-press menu** - Configure launch behavior for each app
  - Open app info/settings
  - Choose display (this screen/other screen) for launching apps
  - Per-app preferences are saved

### Easy Setup
- **Quick Setup Wizard** - Step-by-step configuration on first launch
- **Auto-script creation** - Automatically generates ES-DE integration scripts
- **Comprehensive onboarding** - Tutorial dialog explaining key features and gestures

### Visual Customization
- **Background priority** - Choose between Fanart or Screenshot priority
- **Animation styles** - None, Fade, Scale + Fade, or Custom with adjustable duration and scale
- **Logo controls** - Independent on/off for system/game logos with shared size control
- **Dimming control** - 0-100% background darkening
- **Blur effects** - Optional background blur (Android 12+)
- **Drawer opacity** - Customize app drawer transparency

## 📱 Requirements

- **Android 10+** (API 29 or higher)
- **Dual-screen device** or external display support
- **ES-DE** installed with downloaded media
- **Storage permissions** for accessing media and creating scripts

## 🚀 Installation

1. Download the latest APK from [Releases](../../releases)
2. Install on your device
3. Launch the app and follow the Quick Setup wizard
4. Grant storage permissions when prompted
5. Enable scripts in ES-DE:
   - Open ES-DE
   - Press START → Other Settings
   - Toggle ON "Custom Event Scripts"
   - Toggle ON "Browsing Custom Events"

## 🏠 Recommended Setup

For the best experience, use [Mjolnir](https://github.com/blacksheepmvp/mjolnir) to run this companion app together with ES-DE as your home screens on dual-display devices.

## ⚙️ Configuration

### Default Settings
- **Animation Style**: Scale + Fade
- **Background Dimming**: 25%
- **Drawer Opacity**: 70%
- **Background Priority**: Fanart
- **Grid Columns**: 4
- **System Logo**: On
- **Game Marquee**: On
- **Logo Size**: Medium
- **Video Playback**: Off
- **Video Audio**: Off (when video enabled)
- **App Launch Display**: This Screen
- **Game Launch Display**: Default Image
- **Screensaver Display**: Default Image

All settings can be customized in the Settings screen.

### File Paths

The app uses these default paths (configurable in settings):

| Path | Default Location | Purpose |
|------|-----------------|---------|
| **Downloaded Media** | `/storage/emulated/0/ES-DE/downloaded_media` | Game artwork - fanart, screenshots, marquees |
| **Videos** | `/storage/emulated/0/ES-DE/downloaded_media/{system}/videos` | Game videos (optional) |
| **System Images** | `/storage/emulated/0/ES-DE/downloaded_media/system_images` | Custom system images (optional override) |
| **System Logos** | `/storage/emulated/0/ES-DE/downloaded_media/system_logos` | Custom system logos (optional override) |
| **Scripts** | `/storage/emulated/0/ES-DE Companion/scripts` | Integration scripts |
| **Logs** | `/storage/emulated/0/ES-DE Companion/logs` | Event log files |

**Note**: Scripts and logs have moved to internal storage (`/ES-DE Companion/`) for better compatibility with SD card installations.

### Custom System Images (Optional Override)

To override random game artwork in system view with your own images:
1. Place custom images in the system images folder
2. Use filenames matching ES-DE system shortnames: `snes.webp`, `arcade.png`, `psx.jpg`, etc.
3. These will override the random game art displayed

### Custom System Logos (Optional Override)

To add additional system logos or override built-in system logos in system view with your own images:
1. Place custom images in the system logos folder
2. Use filenames matching ES-DE system shortnames: `snes.svg`, `snes.png`, `arcade.webp`, etc.
3. These will take priority over built-in logos

## 🎨 How It Works

1. **ES-DE Integration**: The app creates custom event scripts that ES-DE calls when you browse games/systems, launch games, and activate screensavers
2. **Real-time Updates**: Scripts write to log files that the app monitors using FileObserver
3. **Image Loading**: The app reads game metadata and displays corresponding artwork
4. **Smart State Management**: Automatically handles gameplay, screensavers, and browsing states

### Script Files

The Quick Setup creates these scripts automatically:

```
ES-DE Companion/scripts/
├── esdecompanion-game-select/
│   └── esdecompanion-game-select.sh        # Browsing games
├── esdecompanion-system-select/
│   └── esdecompanion-system-select.sh      # Browsing systems
├── esdecompanion-game-start/
│   └── esdecompanion-game-start.sh         # Game launched
├── esdecompanion-game-end/
│   └── esdecompanion-game-end.sh           # Game exited
├── esdecompanion-screensaver-start/
│   └── esdecompanion-screensaver-start.sh  # Screensaver started
├── esdecompanion-screensaver-end/
│   └── esdecompanion-screensaver-end.sh    # Screensaver ended
└── esdecompanion-screensavergameselect/
    └── esdecompanion-screensavergameselect.sh  # Screensaver game
```

## 🤝 Contributing

Contributions are welcome! Please feel free to open an Issue or submit a Pull Request.

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 🙏 Credits

- Built for [ES-DE](https://es-de.org/) by Leon Styhre
- Works best in a dual home screen set up using [Mjolnir](https://github.com/blacksheepmvp/mjolnir) home screen manager by Blacksheep
- Uses [Glide](https://github.com/bumptech/glide) for efficient image loading
- Uses [AndroidSVG](https://github.com/BigBadaboom/androidsvg) for SVG rendering
- Uses [ExoPlayer](https://github.com/google/ExoPlayer) for video playback

## 📞 Support

If you encounter any issues or have questions:
1. Check the [Issues](../../issues) page
2. Create a new issue with details about your problem

## 🔄 Changelog

### [0.2.2] - 2026-01-05 - Gameplay & Screensaver Enhancement Release

#### 🎮 Game Launch Display Control
- **New Setting**: "Game Launch Display" in Application Behavior settings
  - **Black**: Plain black screen during gameplay (minimal distraction)
  - **Default Image**: Fallback background with current game marquee
  - **Game Image**: Shows launched game's artwork and marquee
- **Dual video blocking system** prevents videos during gameplay:
  - Window focus detection (when ES-DE uses 'launch games on the other screen')
  - Game state tracking (when game launches on top of ES-DE)
- **Smart duplicate filtering** prevents event spam from running games
- **Live setting updates** - Changes apply immediately if game is already running

#### 🌙 Screensaver Display Control
- **New Setting**: "Screensaver Display" in Application Behavior settings
  - **Black**: Plain black screen during screensaver
  - **Default Image**: Fallback background with current marquee
  - **Game Image**: Shows screensaver game artwork (slideshow/video modes)
- **Intelligent screensaver handling**:
  - Automatically detects ES-DE screensaver type (dim/black vs. slideshow/video)
  - Blocks videos during screensaver regardless of mode
  - Ignores browse events during screensaver to prevent interference
- **Live setting updates** - Changes apply immediately if screensaver is active

#### 📁 Path Changes (Important!)
- **Logs location**: Moved to `/storage/emulated/0/ES-DE Companion/logs`
- **Script naming**: Prefix changed to `esdecompanion-` for clarity
- **Migration system**: Automatic validation and migration from old paths/names
- **Why**: Improves compatibility with ES-DE installed on SD cards (FileObserver requires internal storage) and provides clean future updating
- **Action Required**: Re-run Quick Setup after updating to create new scripts

### [0.2.1] - 2026-01-04 - SD Card Compatibility Fix
- Fixed compatibility with ES-DE installed to SD card
- Moved logs to internal storage for reliable FileObserver monitoring
- **Note**: Re-create scripts in Settings after updating if game images were not loading

### [0.2.0] - 2026-01-04 - UX Improvements Release
- 🎓 **Enhanced onboarding** - Comprehensive tutorial dialog at end of setup wizard
- 💡 **Settings discoverability** - Visual pulse animation on settings button
- ⚡ **Performance optimizations** - Separate debouncing for systems and games

### [0.1.2] - 2026-01-03 - Various Presentation and Bug Fixes

### [0.1.1] - 2026-01-03 - Video Playback Release
- 🎬 **Video playback support** - Play game videos with configurable settings
- 🎨 **UI improvements** - Renamed "Game Logo" to "Game Marquee" for clarity
- 🔧 **App launch improvements** - Changed default to "This Screen"
- ⚡ **Performance optimizations** - Smart video and marquee loading

### [0.1.0] - 2026-01-02 - Initial Release
- ✨ Built-in system logos for all supported ES-DE systems
- 🔧 Separate on/off controls for system and game logos
- 📏 Shared logo size control (Small/Medium/Large)
- ✨ Advanced animation system with 4 styles
- 🚀 Quick Setup wizard with ES-DE script configuration
- 📱 App drawer with search and visibility controls
- 🖼️ Real-time game artwork and marquee display

---

**Enjoy your enhanced ES-DE dual screen experience!** 🎮✨
