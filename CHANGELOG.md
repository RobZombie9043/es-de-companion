# 🔄 Changelog

## [0.2.2] - 2026-01-05 - Gameplay & Screensaver Enhancement Release

### 🎮 Game Launch Display Control
- **New Setting**: "Game Launch Display" in Application Behavior settings
  - **Black**: Plain black screen during gameplay (minimal distraction)
  - **Default Image**: Fallback background with current game marquee
  - **Game Image**: Shows launched game's artwork and marquee
- **Dual video blocking system** prevents videos during gameplay:
  - Window focus detection (when ES-DE uses 'launch games on the other screen')
  - Game state tracking (when game launches on top of ES-DE)
- **Smart duplicate filtering** prevents event spam from running games
- **Live setting updates** - Changes apply immediately if game is already running

### 🌙 Screensaver Display Control
- **New Setting**: "Screensaver Display" in Application Behavior settings
  - **Black**: Plain black screen during screensaver
  - **Default Image**: Fallback background with current marquee
  - **Game Image**: Shows screensaver game artwork (slideshow/video modes)
- **Intelligent screensaver handling**:
  - Automatically detects ES-DE screensaver type (dim/black vs. slideshow/video)
  - Blocks videos during screensaver regardless of mode
  - Ignores browse events during screensaver to prevent interference
- **Live setting updates** - Changes apply immediately if screensaver is active

### 📁 Path Changes (Important!)
- **Logs location**: Moved to `/storage/emulated/0/ES-DE Companion/logs`
- **Script naming**: Prefix changed to `esdecompanion-` for clarity
- **Migration system**: Automatic validation and migration from old paths/names
- **Why**: Improves compatibility with ES-DE installed on SD cards (FileObserver requires internal storage) and provides clean future updating
- **Action Required**: Re-run Quick Setup after updating to create new scripts

## [0.2.1] - 2026-01-04 - SD Card Compatibility Fix
- Fixed compatibility with ES-DE installed to SD card
- Moved logs to internal storage for reliable FileObserver monitoring
- **Note**: Re-create scripts in Settings after updating if game images were not loading

## [0.2.0] - 2026-01-04 - UX Improvements Release
- 🎓 **Enhanced onboarding** - Comprehensive tutorial dialog at end of setup wizard
- 💡 **Settings discoverability** - Visual pulse animation on settings button
- ⚡ **Performance optimizations** - Separate debouncing for systems and games

## [0.1.2] - 2026-01-03 - Various Presentation and Bug Fixes

## [0.1.1] - 2026-01-03 - Video Playback Release
- 🎬 **Video playback support** - Play game videos with configurable settings
- 🎨 **UI improvements** - Renamed "Game Logo" to "Game Marquee" for clarity
- 🔧 **App launch improvements** - Changed default to "This Screen"
- ⚡ **Performance optimizations** - Smart video and marquee loading

## [0.1.0] - 2026-01-02 - Initial Release
- ✨ Built-in system logos for all supported ES-DE systems
- 🔧 Separate on/off controls for system and game logos
- 📏 Shared logo size control (Small/Medium/Large)
- ✨ Advanced animation system with 4 styles
- 🚀 Quick Setup wizard with ES-DE script configuration
- 📱 App drawer with search and visibility controls
- 🖼️ Real-time game artwork and marquee display
