# 🔄 Changelog

## [0.3.4] - TBC
- Updated psp logo to blue version for better visibility


## [0.3.3] - 2026-01-10
- Added additional selectable game overlay image types (None, Marquee, 2D Box, 3D Box, Mix Image)

## [0.3.2] - 2026-01-09
- More fixes for custom image ImagePicker to work with MediaStore and other file picker scenarios

## [0.3.1] - 2026-01-09
- Fix for custom image ImagePicker
- Added ability to show hidden apps in search and unhide app from long press menu

## [0.3.0] - 2026-01-08
- Overhauled app long press menu
    - New `Hide App` button to more easily hide apps from app drawer
    - Choosing launch app on `this screen`/`other screen` directly launches the app on the chosen screen instead of needing to go back to app drawer to do this. Last used selection is remembered for future app launches.
    - Added a small indicator dot on app icons that are set for launch on other screen for a quick visual indication of current setting
- Added a `Custom Background Image` option that can override the default fallback image for when media is not found and can be set as the Game Launch or Screensaver Display image.
- Updated some app default settings (wont affect existing settings if already changed)
    - `Video Delay` is set to 2 seconds (if Video Playback is enabled)
    - `Game Launch Display` and `Screensaver Display` settings default to `Game Image`
- Added missing system logos for All Games and Last Played Auto Collections and grouped Custom Collections
- Changed default directories for custom `System Images and Logos` to /storage/emualted/0/ES-DE Companion/system_images or system_logos (this is to keep all ES-DE Companion custom media separate from ES-DEs downloaded_media by default)
- Added separate sliders for System Logo and Game Marquee size (off - small - medium - large)

## [0.2.9] - 2026-01-07
- Fix for Game Launch Display setting not sticking after game launch
  
## [0.2.8] - 2026-01-07
- Fix for media not displaying for games in folders, updating script detection to assist with migration 
- Update image loading to handle cache invalidation 

## [0.2.7] - 2026-01-07
- Volume control fix that respects per screen volume controls

## [0.2.6] - 2026-01-07
- Added an optional double tap shortcut to show/hide black overlay
- Volume control fixes

## [0.2.5] - 2026-01-06
- Added enhanced script setup safety checks to avoid incorrect setups leading to black screen
- Back Button Fix to prevent cycling through recent apps instead of staying on the home screen

## [0.2.4] - 2026-01-06
- Updated to use Glide's built-in cross-fade to handle the fade animations which results in smoother transitions with no black screens

## [0.2.3] - 2026-01-05
- Improved screensaver transitions for all screensaver event types
- Fix for https://github.com/RobZombie9043/es-de-companion/issues/5 - App crash with custom images
- Fix for png/jpg custom system images not loading
- Fix for https://github.com/RobZombie9043/es-de-companion/issues/4 - SD card not mounting on boot triggering quick setup

## [0.2.2] - 2026-01-03 - Game launch & Screensaver Controls release

### Game Launch Display Control
- **New Setting**: "Game Launch Display" in Application Behavior settings
  - **Black**: Plain black screen during gameplay (minimal distraction)
  - **Default Image**: Fallback background with current game marquee
  - **Game Image**: Shows launched game's artwork and marquee
- **Dual video blocking system** prevents videos during gameplay:
  - Window focus detection (when ES-DE uses 'launch games on the other screen')
  - Game state tracking (when game launches on top of ES-DE)
- **Smart duplicate filtering** prevents event spam from running games
- **Live setting updates** - Changes apply immediately if game is already running

### Screensaver Display Control
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

## [0.2.1] - 2026-01-02 - SD Card Compatibility Fix
- Fixed compatibility with ES-DE installed to SD card
- Moved logs to internal storage for reliable FileObserver monitoring
- **Note**: Re-create scripts in Settings after updating if game images were not loading

## [0.2.0] - 2026-01-02 - UX Improvements - First Beta Release
- **Enhanced onboarding** - Comprehensive tutorial dialog at end of setup wizard
- **Settings discoverability** - Visual pulse animation on settings button
- **Performance optimizations** - Separate debouncing for systems and games

## [0.1.2] - 2026-01-03 - Various Presentation and Bug Fixes

## [0.1.1] - 2026-01-03 - Video Playback enhancement
- **Video playback support** - Play game videos with configurable settings
- **UI improvements** - Renamed "Game Logo" to "Game Marquee" for clarity
- **App launch improvements** - Changed default to "This Screen"
- **Performance optimizations** - Smart video and marquee loading

## [0.1.0] - 2026-01-02 - Test build
- Built-in system logos for all supported ES-DE systems
- Separate on/off controls for system and game logos
- Shared logo size control (Small/Medium/Large)
- Advanced animation system with 4 styles
- Quick Setup wizard with ES-DE script configuration
- App drawer with search and visibility controls
- Real-time game artwork and marquee display
