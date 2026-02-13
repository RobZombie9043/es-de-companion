 # ES-DE Companion - Frequently Asked Questions

## General Questions

### What is ES-DE Companion?

ES-DE Companion is a secondary screen application for Android devices that displays game artwork, videos, and customizable widgets while you browse and play games in ES-DE on your primary screen. It's designed for dual-screen devices and external display setups.

### Do I need ES-DE installed to use this app?

Yes, ES-DE Companion requires ES-DE to be installed on your Android device. The app integrates with ES-DE through custom event scripts to display synchronized content as you browse your game collection.

### Is this an official ES-DE app?

This is a semi-official companion app. While not part of the core ES-DE project, it's designed by one of the ES-DE team members as a side project to seamlessly integrate with ES-DE's custom event scripts system.

### What devices are supported?

ES-DE Companion works on:
- Dual-screen Android devices (Ayn Thor, Ayaneo Pocket DS, Anbernic RG DS etc.)
- Android 10 (API 29) or higher required

### How much does it cost?

ES-DE Companion is free and open source. The full source code is available on GitHub.

## Setup and Installation

### How do I install ES-DE Companion?

1. Download the latest APK from the [Releases page](https://github.com/RobZombie9043/es-de-companion/releases)
2. Install on your Android device
3. Launch the app and follow the Quick Setup wizard
4. Grant storage permissions when prompted
5. Enable custom scripts in ES-DE (START → Other Settings → Custom Event Scripts)

### The app says scripts are missing or invalid. What should I do?

The Quick Setup wizard should create all necessary scripts automatically. If scripts are missing:

1. Open Settings in ES-DE Companion
2. Scroll to the ES-DE Scripts section
3. Tap "Run Quick Setup" to regenerate scripts
4. Check that the scripts path matches your ES-DE installation exactly
5. Ensure you have storage permissions granted

### How do I enable custom scripts in ES-DE?

1. Open ES-DE on your primary screen
2. Press START to open the main menu
3. Navigate to Other Settings
4. Toggle ON "Custom Event Scripts"
5. Toggle ON "Browsing Custom Events"
6. Restart ES-DE if needed

### The app isn't updating images when I browse games in ES-DE. What's wrong?

Check these common issues:

1. **Scripts not enabled**: Verify custom event scripts are enabled in ES-DE (see above)
2. **Wrong paths**: Check that the media path and scripts path in Settings match your ES-DE installation. This needs to exactly match the ES-DE application folder that was setup during ES-DE installation for the scripts to be able to run.

## Widget System

### How do I create widgets?

1. Long-press anywhere on the companion screen to open the widget menu
2. Toggle "Widget Edit Mode" to ON
3. Tap "Add Widget" 
4. Choose your widget type (Marquee, Box Art, Screenshot, etc.)
5. The widget appears in the center - drag and resize as needed
6. Long-press again and toggle Edit Mode to OFF when done

### Why can't I move or resize widgets?

Widgets are locked by default to prevent accidental changes. You must enable Widget Edit Mode:

1. Long-press the screen to open the widget menu
2. Look for "Widget Edit Mode: OFF" at the top
3. Tap to toggle it to ON
4. Now you can select, move, and resize widgets
5. Remember to lock them again (toggle OFF) when finished

### What's the difference between System and Game widgets?

ES-DE Companion maintains separate widget layouts for system and game views:
- **System widgets**: Show when browsing the system view (SNES, Genesis, Arcade, etc.)
- **Game widgets**: Show when browsing individual games within a system

This lets you create different layouts optimized for each context.

### How do I delete a widget?

1. Enable Widget Edit Mode (long-press screen → toggle ON)
2. Tap the widget to select it (shows purple border)
3. Tap the 🗑 button in the top-right corner of the widget
4. Confirm deletion

### What's the difference between widget types?

ES-DE Companion supports these widget types:

**Artwork Widgets:**
- **System Logo**: Shows the current system's logo (system view only)
- **Random Fanart**: Random fanart from current system (system view only)
- **Random Screenshot**: Random screenshot from current system (system view only)
- **Marquee**: Game logo artwork
- **2D Box**: Front cover of the game box
- **3D Box**: 3D perspective box art
- **Mix Image**: Composite mix image
- **Screenshot**: In-game screenshot
- **Fanart**: Fan generated artwork
- **Title Screen**: Game's title screen
- **Back Cover**: Back of the game box
- **Physical Media**: Disc/cartridge artwork

**Special Widgets:**
- **Game Description**: Scrollable text description of the game
- **Color Background**: Solid color panel with selectable color and opacity (both views)
- **System Image** | Shows the custom system image from the System Images Path (e.g. `snes.webp`)
- **Custom Image**: User-selected image from device storage (both views)

### How do I layer widgets (control which appears on top)?

1. Select a widget (tap it in Edit Mode)
2. Tap the 🔧 settings button on the widget
3. Use "Move Forward" / "Move Backward" for fine control

### What is snap-to-grid?

Snap-to-grid helps align widgets precisely:
1. Enable it in the widget menu
2. Widgets will automatically align to the grid when moved or resized
3. Optional "Show Grid" displays the visual grid lines

## Display and Media

### Why don't I see any images?

Common causes:

1. **Media not scraped**: You need to scrape game media in ES-DE first (scraper downloads artwork)
2. **Wrong path**: Check that the "Downloaded Media Path" in Settings matches your ES-DE media folder
3. **Permissions**: Verify storage permissions are granted

### Videos aren't playing. How do I fix this?

1. **Check video settings**: Settings → Video Playback → ensure it's enabled
2. **Video delay**: There may be a configured delay (0-5 seconds) before video starts
3. **File format**: Ensure videos are in a supported format (MP4, MKV, AVI, WMV, MOV, WEBM)
4. **Path**: Verify the media path is correct
5. **Screensaver/Game Launch**: Videos don't play during screensavers or game launches by default

### Can I use custom backgrounds?

Yes! You have several options:

**System View Backgrounds:**
1. Place custom images in `/ES-DE/downloaded_media/system_images/`
2. Name them after the system: `snes.webp`, `arcade.png`, etc.
3. These override the default random game artwork

**Custom Image:**
1. Go to Settings → Paths → Custom Background Image
2. Select "Select Image"
3. Pick any image from your device
4. This shows when Custom Image is set or when no game artwork is available

### How do I add custom system logos?

1. Place logo images in `/ES-DE/downloaded_media/system_logos/`
2. Name them after the system: `snes.svg`, `snes.png`, etc.
3. SVG format recommended for best quality
4. Custom logos take priority over built-in logos

### What's the difference between Fanart and Screenshot priority?

This setting controls which image type is shown first:
- **Fanart**: Shows high-quality artwork (more artistic/promotional)
- **Screenshot**: Shows in-game screenshots (more authentic to gameplay)

If the preferred type isn't available, the app automatically falls back to the other.

## App Drawer

### How do I open the app drawer?

Swipe up anywhere on the companion screen to open the app drawer.

### Can I hide apps from the drawer?

Yes:
1. Open the app drawer
2. Long-press any app
3. Select "Hide App"
4. Hidden apps can be unhidden from Settings

### How do I launch apps on the other screen?

1. Long-press an app in the drawer
2. Select "Launch on other screen"
3. The app will open on your other display
4. This preference is saved per-app

## Background Music

### How do I enable background music?

1. Go to Settings → Background Music
2. Toggle "Enable Background Music" to ON
3. Select your music folder (should contain MP3 files)
4. Configure playback settings (shuffle, volume, etc.)

### Music isn't playing. Why?

Check these settings:
1. **Enabled**: Background Music toggle must be ON
2. **Music folder**: Must contain valid MP3 files
3. **Playback states**: Check when music should play (system browsing, game browsing)
4. **Volume**: Ensure music volume isn't set to 0%
5. **Permissions**: Storage permissions must be granted

### How do I control background music while playing?
1. Two finger tap gesture on the main screen will show the song title display if hidden
2. Play/pause and next track controls can be used to control the music

### Can music play during videos?

Yes, there are three video behavior modes:
- **Continue (1005)**: Music plays at full volume
- **Lower (20%)**: Music reduces to 20% volume during videos
- **Pause**: Music pauses during videos

Configure this in Settings → Background Music → Video Behavior.
This only works with videos played inside the Companion app and does not work for videos originated from ES-DE itself.

## Performance and Troubleshooting

### The app is laggy or slow. How can I improve performance?

Try these optimizations:

1. **Disable blur**: Background blur (Android 12+) is GPU-intensive
2. **Reduce animations**: Set animation style to "None" or "Fade" only
3. **Fewer widgets**: Too many widgets can impact performance
4. **Lower video quality**: Use smaller video files if available
5. **Clear cache**: Settings → Clear Cache

### The app crashes when I browse games. What should I do?

1. Check that you have enough free RAM
2. Clear the app cache (Settings → Clear Cache)
3. Reduce the number of active widgets
4. Disable background blur if enabled
5. Check logcat for error details and report the issue on GitHub

### The app doesn't survive device sleep. Is this normal?

Android may kill background apps to save battery. To improve retention:

1. Disable battery optimization for ES-DE Companion (Settings → Apps → Battery)
2. Note that some manufacturers aggressively kill background apps

## Advanced Usage

### How do I report a bug?

1. Check the existing [GitHub Issues](https://github.com/RobZombie9043/es-de-companion/issues)
2. If it's a new bug, create a new issue with:
   - Device model
   - ES-DE Companion version
   - Steps to reproduce
   - Screenshots if applicable

## Integration with ES-DE

### How does ES-DE Companion communicate with ES-DE?

ES-DE Companion uses the custom event scripts system:

1. Scripts are placed in `/ES-DE/scripts/`
2. ES-DE calls these scripts when you browse games, launch games, etc.
3. Scripts write events to log files
4. ES-DE Companion monitors log files using FileObserver
5. When changes are detected, the companion updates its display

### Can I use ES-DE Companion with other frontends?

Currently, ES-DE Companion is specifically designed for ES-DE and uses its custom event scripts system. 

## Getting Help

**Documentation:**
- [User Guide](USERGUIDE.md) - Comprehensive documentation
- [README](README.md) - Project overview
- This FAQ

**Issues:**
- GitHub Issues: https://github.com/RobZombie9043/es-de-companion/issues

**ES-DE Resources:**
- ES-DE Documentation: https://gitlab.com/es-de/emulationstation-de
- ES-DE Discord: https://discord.gg/kKUE8Djk