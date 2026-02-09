# Changelog

## [0.6.0] - TBC

### Added
- **Immersive Mode**: Enhanced fullscreen experience when Android is set to 3-button navigation mode
- **New Widget Types**: Four new widget types available for enhanced customization
    - **Color Background**: Solid color panels with adjustable opacity (0-100%)
        - Choose from 12 preset colors or enter custom hex colors (#RRGGBB)
        - Perfect for creating colored sections, backgrounds, or design accents
        - Available in both system and game views
    - **Custom Image**: Display any image from your device storage
        - Select PNG, JPG, WEBP, GIF, or SVG images from your device
        - Ideal for custom artwork, logos, or personal images
        - Available in both system and game views
    - **Random Fanart**: Display random fanart from current system (system view only)
        - Shows a different random fanart each time you switch systems
        - Automatically selects from all available fanart in the system's media folder
        - Adds visual variety to system browsing
    - **Random Screenshot**: Display random screenshot from current system (system view only)
        - Shows a different random screenshot each time you switch systems
        - Automatically selects from all available screenshots in the system's media folder
        - Great for previewing game collections

### Changed
- **Performance Improvements**:
    - Significantly improved system scrolling responsiveness by reducing debounce delays
- **Widgets fallbacks**: Added fallback logic to fanart and screenshot widgets
- **Minimum SDK Requirement**: Updated from Android 10 (API 29) to Android 13 (API 33)
    - Replaced deprecated RenderScript blur with RenderEffect (modern blur API)
    - Updated blur implementation to use Android 13+ native RenderEffect
- **Media3 Library**: Updated ExoPlayer (Media3) from 1.2.0 to 1.4.1
- **Code Organization**: Major package restructure for better maintainability
    - Created `ui` package for UI components (AppAdapter, ResizableWidgetContainer, WidgetView, GridOverlayView)
    - Created `data` package for data models (AppState, OverlayWidget)
    - Renamed `Constants.kt` to `AppConstants.kt` for clarity
    - Refactored `MediaFileLocator` to `MediaManager` (better naming for manager pattern)
    - Refactored `AppLaunchPreferences` to `AppLaunchManager` (consistent manager naming)

### Refactored
- **Manager Classes**: Comprehensive code cleanup and optimization across all managers
    - `VideoManager.kt`: Cleaned up video playback logic, removed unused code
    - `MusicManager.kt`: Streamlined music implementation, improved code organization
    - `WidgetManager.kt`: Code cleanup and consistency improvements
    - `ScriptManager.kt`: Code cleanup and removed redundant logic
    - `PreferencesManager.kt`: Added KTX extensions for cleaner preference access
- **Constants Management**: Extracted hardcoded values to centralized `AppConstants.kt`
    - Timing constants (debounce delays, animation durations)
    - UI constants (grid sizes, gesture thresholds)
    - File path constants
- **General Code Cleanup**: Removed unused code, imports and deprecated functions throughout codebase

### Fixed
- **Image Cache Invalidation**: Fixed cache invalidation for changed background images

## [0.5.3] - 2026-02-06

### Added
- Video playback looping
- KSP processor for Glide
- Error handling added to video player and image display

### Changed
- Removed animations when transition has same image
- Background music continues to play in app settings menu
- Remove Game End Debounce
- Refactor: Preferences Manager
- Refactor: ImageManager
- Refactor: VideoManager
- Refactor: ScriptManager
- Refactor: moved widgetManager to managers
- Cleanup of imports

- ### Fixed
- Media lookup to remove ROMs hardcoding affecting image display
- systemName race condition causing images to not display in rare instances

## [0.5.2] - 2026-01-30

### Added
- Two-finger tap gesture to display/hide song title 
- Play/pause/next controls added to song title display

### Changed
- Modified when Custom Image is set as the Background Image type to not apply fade and scale transition animations as its not changing images

### Fixed
- Double tap black overlay pauses background music

## [0.5.1] - 2026-01-25

### Fixed
- Game description widget not working for some game names with special characters
- System logo override SVG logos not working

## [0.5.0] - 2026-01-25

### Added
- **Background Music System**: Play background music during game/system browsing
    - Master On/Off control
    - Separate On/Off controls for System View/Game View/Screensaver
    - Automatically turns off during gameplay or when app loses focus
    - Volume control during ES-DE Companion videos: Continue (100%), Lower (20%), or Pause
    - Display song titles with customizable duration and background opacity
    - Customizable music path (default: `/ES-DE Companion/music`)
    - Per-system music overrides by placing music in `~music/systems/{systemname}/`
- **Animated Media Support**: Support for animated logos and background images (GIF/animated WEBP)
- **Custom Image Backgrounds**: Added Custom Image selection to System and Game View backgrounds
    - Uses Custom Background Image if set, otherwise falls back to built-in default
- **Build Variants**: Two build variants available
    - Standard version (shows in recent apps)
    - Persistent version with 'h' suffix (hidden from recent apps menu)

### Changed
- **State Management**: Refactored from boolean-based state tracking to centralized state machine
- **Code Cleanup**: Extracted MediaFileLocator to eliminate duplicate code
- **Refactored Long Functions**: Improved code organization and maintainability

### Fixed
- Cache invalidation now applies to widget images for proper refresh of changed artwork
- Removed focus window being applied to widgets when navigating with built-in controls
- Implemented Android standard gesture controls for more consistent navigation
- Cancel long press timer when widget interaction starts to avoid accidentally opening widget menu

## [0.4.5] - 2026-01-19

### Fixed
- Incorrect default for screensaver image type
- Removed focus from widgets when using built-in controls

## [0.4.4] - 2026-01-19

### Changed
- Modified widget onboarding dialog presentation at end of Quick Setup
- Added 100ms minimum double-tap time for black screen toggle to avoid accidental activations

## [0.4.3] - 2026-01-19

### Added
- Widget onboarding help dialog

## [0.4.2] - 2026-01-19

### Fixed
- Some text being unreadable with Android Light mode enabled
- System logos being hidden when turning on widget edit mode

## [0.4.1] - 2026-01-19

### Fixed
- Videos not playing on Ayaneo Pocket DS
- Some system logos not scaling correctly

## [0.4.0] - 2026-01-18

### Added
- **Widget Overlay System**: Create customizable overlay widgets
    - Long-press screen to access widget menu
    - **System View Widget**: System Logo
    - **Game View Widgets**: Marquee, 2D Box, 3D Box, Mix Image, Back Cover, Physical Media, Screenshot, Fanart, Title Screen, Game Description
    - Widget Edit Mode toggle (On/Off)
    - Snap to Grid toggle (On/Off)
    - Show Grid toggle (On/Off)
    - Layer control to adjust widget stacking order
    - Adjustable transparency for game description widget
    - Image scaling options: Fit to container or Fill container with cropping
- **Split Backgrounds**: Independent background images for system browsing vs game browsing
- **Solid Color Backgrounds**: Option to use solid colors instead of images for either view
- **Improved Fallback Text**: System logo and game marquee widgets show fallback text with truncation for long names

### Changed
- Added back button in settings menu
- Updated PSP default logo to blue version for better visibility

## [0.3.3] - 2026-01-10

### Added
- Additional selectable game overlay image types (None, Marquee, 2D Box, 3D Box, Mix Image)

## [0.3.2] - 2026-01-09

### Fixed
- Additional fixes for custom image ImagePicker to work with MediaStore and other file picker scenarios

## [0.3.1] - 2026-01-09

### Added
- Ability to show hidden apps in search and unhide app from long press menu

### Fixed
- Custom image ImagePicker functionality

## [0.3.0] - 2026-01-08

### Added
- **Custom Background Image**: Can override default fallback image and be used as Game Launch or Screensaver Display image
- **App Long Press Menu Overhaul**:
    - New "Hide App" button for easier app drawer management
    - Choosing launch location (this screen/other screen) now launches app immediately
    - Last used launch selection remembered for future launches
    - Small indicator dot on app icons set to launch on other screen
- **Missing System Logos**: Added logos for All Games, Last Played Auto Collections, and Custom Collections
- **Separate Logo Size Controls**: Independent sliders for System Logo and Game Marquee size

### Changed
- Default directories for custom System Images and Logos changed to `/ES-DE Companion/system_images` and `/system_logos`
- Updated default settings (won't affect existing customized settings):
    - Video Delay set to 2 seconds (if Video Playback enabled)
    - Game Launch Display and Screensaver Display default to "Game Image"

## [0.2.9] - 2026-01-07

### Fixed
- Game Launch Display setting not persisting after game launch

## [0.2.8] - 2026-01-07

### Fixed
- Media not displaying for games in subfolders
- Updated script detection to assist with migration
- Image loading cache invalidation

## [0.2.7] - 2026-01-07

### Fixed
- Volume control now respects per-screen volume controls

## [0.2.6] - 2026-01-07

### Added
- Optional double-tap shortcut to show/hide black overlay

### Fixed
- Volume control issues

## [0.2.5] - 2026-01-06

### Added
- Enhanced script setup safety checks to avoid incorrect setups leading to black screen

### Fixed
- Back button cycling through recent apps instead of staying on home screen

## [0.2.4] - 2026-01-06

### Changed
- Updated to use Glide's built-in cross-fade for smoother transitions with no black screens

## [0.2.3] - 2026-01-05

### Added
- Improved screensaver transitions for all screensaver event types

### Fixed
- App crash with custom images ([Issue #5](https://github.com/RobZombie9043/es-de-companion/issues/5))
- PNG/JPG custom system images not loading
- SD card not mounting on boot triggering Quick Setup ([Issue #4](https://github.com/RobZombie9043/es-de-companion/issues/4))

## [0.2.2] - 2026-01-03

### Added
- **Game Launch Display Control**: Configure what displays during gameplay
    - Black: Plain black screen (minimal distraction)
    - Default Image: Fallback background with current marquee
    - Game Image: Shows launched game's artwork and marquee
- **Screensaver Display Control**: Configure what displays during ES-DE screensavers
    - Black: Plain black screen
    - Default Image: Fallback background with current marquee
    - Game Image: Shows screensaver game artwork (slideshow/video modes)
- **Dual Video Blocking System**: Prevents videos during gameplay
    - Window focus detection (when ES-DE uses "launch games on other screen")
    - Game state tracking (when game launches on top of ES-DE)
- **Smart Duplicate Filtering**: Prevents event spam from running games
- **Live Setting Updates**: Changes apply immediately if game is already running
- **Intelligent Screensaver Handling**:
    - Automatically detects ES-DE screensaver type (dim/black vs slideshow/video)
    - Blocks videos during screensaver regardless of mode
    - Ignores browse events during screensaver to prevent interference

### Changed
- **Path Changes** (Important - Re-run Quick Setup):
    - Logs location: Moved to `/storage/emulated/0/ES-DE Companion/logs`
    - Script naming: Prefix changed to `esdecompanion-` for clarity
    - Migration system: Automatic validation and migration from old paths/names
    - Reason: Improves compatibility with ES-DE on SD cards (FileObserver requires internal storage)

## [0.2.1] - 2026-01-02

### Fixed
- Compatibility with ES-DE installed to SD card
- Moved logs to internal storage for reliable FileObserver monitoring

**Note**: Re-create scripts in Settings after updating if game images were not loading

## [0.2.0] - 2026-01-02

### Added
- Enhanced onboarding with comprehensive tutorial dialog at end of setup wizard
- Visual pulse animation on settings button for improved discoverability

### Changed
- Performance optimizations with separate debouncing for systems and games

## [0.1.2] - 2026-01-03

### Fixed
- Various presentation and bug fixes

## [0.1.1] - 2026-01-03

### Added
- **Video Playback Support**: Play game videos with configurable settings

### Changed
- Renamed "Game Logo" to "Game Marquee" for clarity
- Changed app launch default to "This Screen"
- Performance optimizations for video and marquee loading

## [0.1.0] - 2026-01-02

### Added
- **Initial Release**: Test build
- Built-in system logos for all supported ES-DE systems
- Separate On/Off controls for system and game logos
- Shared logo size control (Small/Medium/Large)
- Advanced animation system with 4 styles
- Quick Setup wizard with ES-DE script configuration
- App drawer with search and visibility controls
- Real-time game artwork and marquee display

---

## Version History Summary

| Version | Release Date | Major Features |
|---------|--------------|----------------|
| 0.5.1   | 2026-01-25  | Bug fixes for widget descriptions and SVG logos |
| 0.5.0   | 2026-01-25  | Background music system, animated media support, build variants |
| 0.4.5   | 2026-01-19  | Screensaver defaults and widget focus fixes |
| 0.4.4   | 2026-01-19  | Widget onboarding improvements |
| 0.4.3   | 2026-01-19  | Widget help dialog |
| 0.4.2   | 2026-01-19  | Light mode text fixes, system logo visibility |
| 0.4.1   | 2026-01-19  | Video playback and logo scaling fixes |
| 0.4.0   | 2026-01-18  | Widget overlay system, split backgrounds |
| 0.3.3   | 2026-01-10  | Additional game overlay image types |
| 0.3.2   | 2026-01-09  | Custom image picker fixes |
| 0.3.1   | 2026-01-09  | Hidden app management improvements |
| 0.3.0   | 2026-01-08  | Custom backgrounds, app drawer overhaul |
| 0.2.9   | 2026-01-07  | Game launch display persistence fix |
| 0.2.8   | 2026-01-07  | Subfolder support, cache invalidation |
| 0.2.7   | 2026-01-07  | Per-screen volume control fix |
| 0.2.6   | 2026-01-07  | Double-tap black overlay toggle |
| 0.2.5   | 2026-01-06  | Script setup safety checks |
| 0.2.4   | 2026-01-06  | Glide cross-fade transitions |
| 0.2.3   | 2026-01-05  | Screensaver improvements, bug fixes |
| 0.2.2   | 2026-01-03  | Game launch and screensaver controls |
| 0.2.1   | 2026-01-02  | SD card compatibility fix |
| 0.2.0   | 2026-01-02  | UX improvements, first beta |
| 0.1.2   | 2026-01-03  | Various fixes |
| 0.1.1   | 2026-01-03  | Video playback enhancement |
| 0.1.0   | 2026-01-02  | Initial test build |

---

## Future Roadmap

### Planned for Future Releases

**TBC**

See [GitHub Issues](https://github.com/RobZombie9043/es-de-companion/issues) for detailed feature requests and bug tracking.

---