# ES-DE Companion - Frequently Asked Questions

## General Questions

### What is ES-DE Companion?

ES-DE Companion is a secondary screen application for Android devices that displays game artwork, videos, and customizable widgets while you browse and play games in ES-DE on your primary screen. It's designed for dual-screen devices and external display setups.

### Do I need ES-DE installed to use this app?

Yes, ES-DE Companion requires ES-DE to be installed on your Android device. The app reads ES-DE's own activity log to display synchronized content as you browse your game collection — see [How does ES-DE Companion communicate with ES-DE?](#how-does-es-de-companion-communicate-with-es-de)

### Is this an official ES-DE app?

This is a semi-official companion app. While not part of the core ES-DE project, it's designed by one of the ES-DE team members as a side project to seamlessly integrate with ES-DE.

### What devices are supported?

ES-DE Companion works on:
- Dual-screen Android devices (Ayn Thor, Ayaneo Pocket DS, Anbernic RG DS etc.)
- Android 10 (API 29) or higher required

### How much does it cost?

ES-DE Companion is free and open source. The full source code is available on GitHub.

### Is this app finished?

The app is a ground-up rebuild and is currently at v0.7.0-RC1 — a release candidate. Core features (log sync, widgets, app drawer/dock, video, music) all work, but expect some rough edges.

## Setup and Installation

### How do I install ES-DE Companion?

1. Download the latest APK from the [Releases page](https://github.com/RobZombie9043/es-de-companion/releases)
2. Install on your Android device
3. Launch the app and follow the onboarding wizard
4. Grant "All files access" when prompted
5. Enable the required settings in ES-DE (see below)

### How do I enable the required settings in ES-DE?

1. Open ES-DE on your primary screen
2. Press START to open the main menu
3. Navigate to Other Settings
4. Toggle ON "Custom Event Scripts"
5. Toggle ON "Browsing Custom Event Scripts"
6. Toggle ON "Debug Mode"
7. Back out of the settings menu — ES-DE only saves these to disk once you leave the menu, so the companion app won't see the change until you do

### The onboarding wizard is stuck on the "ES-DE settings" step

That step watches ES-DE's own `settings/es_settings.xml` file and only advances once all three of Custom Event Scripts, Browsing Custom Event Scripts, and Debug Mode are enabled. Common causes:
- You toggled the settings but haven't backed out of ES-DE's settings menu yet — ES-DE only writes the file on exit, not while the menu is open
- The ES-DE folder configured in the wizard doesn't match the one ES-DE is actually using

### Onboarding is warning me about "DebugSkipInputLogging" — is that a problem?

Not a blocker — it's advisory only, and it doesn't stop onboarding from finishing. If that debug flag is set to `true` in ES-DE's `settings/es_settings.xml`, the companion app can't tell which direction you're navigating, so widget slide animations won't play correctly (other functionality is unaffected). That flag isn't in ES-DE's own settings menu, so fixing it means editing `es_settings.xml` directly and setting it back to `false`.

### How do I redo setup / change my folders later?

Folder paths can be changed any time from Settings → Setup — there's no need to redo the whole wizard. There's no manual "redo setup" button; the onboarding wizard only reappears automatically if "All files access" gets revoked, in which case it restarts with your previous folder choices already filled in.

### The app isn't updating when I browse games in ES-DE. What's wrong?

Check these in order:
1. **ES-DE settings not enabled**: Verify Custom Event Scripts, Browsing Custom Event Scripts, and Debug Mode are all on in ES-DE, and that you backed out of ES-DE's settings menu after enabling them
2. **Wrong ES-DE folder**: Settings → Setup → ES-DE folder must be the exact folder ES-DE itself is configured to use — the app looks for `settings/es_settings.xml` and `logs/es_log.txt` inside it
3. **Wrong media folder**: Settings → Setup → Media folder must match ES-DE's downloaded media location

### How do I hide the Settings gear icon?

ES-DE Companion's own Settings → Other Settings category (not ES-DE's own "Other Settings" menu — see above) has a "Show Settings Button" toggle, on by default. Turning it off just hides the gear icon on the main screen — Settings stays reachable via long-press regardless.

### Can ES-DE Companion close itself when I quit ES-DE?

Yes: Settings → Other Settings → "Close Companion App on ES-DE Quit," off by default. Turn it on and the companion app closes itself when it sees ES-DE's quit event.

### Can Companion launch ES-DE for me automatically?

Yes: Settings → Other Settings → "Launch ES-DE on Companion App Start," off by default. Turn it on and Companion launches ES-DE on the other display as soon as it starts up — combined with Companion's own boot auto-start, this brings both apps up together with no manual step.

### Can I quit Companion manually?

Yes: the Settings popup has a "Quit Companion App" row below the category list that closes the app immediately — no need to wait for "Close Companion App on ES-DE Quit" to trigger.

## Widget System

### How do I create widgets?

1. Long-press anywhere on the companion screen (or tap the Settings gear, if shown) to open the Settings popup
2. Tap "Widgets" at the top of the list — this jumps straight into the widget editor
3. Tap the ⋮ menu button in the corner
4. Tap "Add Widget"
5. Choose a widget type from the list — it's placed centered on the grid and auto-selected
6. Drag to move it, or use the edge handles to resize it
7. Tap "Done" in the menu when finished

### Why can't I move or resize widgets?

There's no lock setting anymore — widget editing is always reachable. If long-press/the gear icon isn't opening the Settings popup at all, or the popup opens but tapping "Widgets" doesn't land you in the editor, that's worth reporting as a bug (see [How do I report a bug?](#how-do-i-report-a-bug)).

### What's the difference between System View and Game View widgets?

ES-DE Companion has two independent widget canvases:
- **System View**: shown while browsing the system carousel
- **Game View**: shown while browsing a game, playing a game, or during the screensaver

Each has its own separate layout, so you can design each context differently.

### How do I delete a widget?

1. Enter edit mode (long-press the screen, then tap "Widgets")
2. Tap the widget to select it
3. Open the ⋮ menu and tap "Remove Widget" — this happens immediately, with no confirmation prompt

### What widget types are available?

**System View:** System Logo, System Image, Random Game Fanart, Random Game Screenshot, Custom Image, Color Background.

**Game View:** Marquee, Description, Box Cover, 3D Box, Mix Image, Screenshot, Fan Art, Title Screen, Box Back Cover, Physical Media, Custom Image, Color Background.

See the [User Guide](USERGUIDE.md#widget-overlay-system) for what each one does and its configuration options.

### How do I layer widgets (control which appears on top)?

1. Select a widget in edit mode
2. Open the ⋮ menu
3. Use "Move Forwards" / "Move Backwards" — each tap moves it one layer at a time

### Is there a snap-to-grid option?

All widget placement is grid-snapped by design — there's no separate toggle, and no free/pixel-level placement. A faint grid line overlay is always visible in edit mode.

### What are Logo Glint and Pan & Zoom?

Two optional per-widget animation effects, each an independent on/off toggle in that widget's Configure Widget dialog:
- **Logo Glint**: a periodic light sweep across the widget. Available on logo-style widgets only (System Logo, Marquee), and it runs alongside whichever transition is selected — not instead of it.
- **Pan & Zoom**: slowly zooms and pans across the image while it's displayed. Available on eligible Cover-scaled backdrop widgets (System Image, Custom Image, Fan Art, Screenshot, Title Screen).

Image/logo transitions (None/Fade for backdrops, None/Slide/Scale for logos) are also configured this way — per widget, not as a global Settings toggle. See [Per-Widget Configuration](USERGUIDE.md#per-widget-configuration) in the User Guide for the full breakdown.

## Display and Media

### Why don't I see any images?

Common causes:
1. **Media not scraped**: You need to scrape game media in ES-DE first
2. **Wrong media folder**: Check Settings → Setup → Media folder matches ES-DE's downloaded media location
3. **Permissions**: Verify "All files access" is granted (Settings → Setup shows a warning banner if it's missing)

### How do I quickly blank the screen?

Double-tap anywhere on the main companion screen — this manually blanks it to black, independent of the Game Playing/Screensaver Screen Behavior settings. Double-tap again to restore. It uses the same black cover that a Screen Behavior set to "Off" triggers automatically, so it also works to restore the screen in that case.

### Can I use custom backgrounds?

Yes:
1. Set a Custom System Images folder in Settings → Setup
2. Place images named after the system there: `snes.webp`, `arcade.png`, etc.
3. Use a System Image widget in System View to display it — it falls back to random fanart, then a random screenshot, then a generic background if no custom image exists for that system

### How do I add custom system logos?

1. Set a Custom Logos folder in Settings → Setup
2. Place logo images named after the system there: `snes.svg`, `snes.png`, etc.
3. Custom logos take priority over the 207 built-in logos bundled with the app

## App Drawer & App Dock

### How do I open the app drawer?

Swipe up anywhere on the companion screen.

### Can I hide apps from the drawer?

Yes: Settings → App Drawer and Dock → Manage Apps shows every installed app with a checkbox; unchecking one hides it from both the App Drawer and the App Dock's add-app picker.

### How do I launch apps on the other screen?

- **Double-tap** an app in the drawer or dock to launch it on the other screen (and remember that as the new default for that app), or
- **Long-press** and choose "Launch on other screen" from the menu

If no secondary display is connected, this just launches on the current screen instead.

### What's the App Dock, and how is it different from the App Drawer?

The App Dock is an optional, persistent row of pinned apps at the bottom of the screen — it's always visible (while the drawer is closed) rather than needing a swipe to open. It's off by default; turn it on in Settings → App Drawer and Dock. Long-press an empty slot to pin an app, or a filled slot to reorder/remove it or change its launch screen. Unlike the drawer, it has no "Hide App" option in its own menu — visibility is managed from the drawer's Manage Apps screen.

### Can I put a shortcut to the App Drawer in the Dock?

Yes — the Dock's add-app picker offers a special "App Drawer" entry at the top of the list. Pin it into a slot like any other app; tapping or double-tapping it opens the App Drawer instead of launching anything. Its long-press menu only offers Move Left/Move Right/Remove from Dock, since "Launch on this/other screen" and "App Info" don't apply to it.

### Can I group apps into folders in the App Drawer?

Yes: long-press an app → **Add to Folder** → either pick an existing folder or create a new one. A folder collapses to a single mosaic tile in the grid; tap it to open a popup with its contents. Long-press an app inside the popup to remove it from the folder, and long-press the folder's own tile (in the grid) or its title bar (inside the open popup) to rename it. There's no manual reordering — folders and apps both sort alphabetically by name, and the **Sort folders on top of apps** setting (Settings → App Drawer and Dock, on by default) controls whether folders are grouped ahead of ungrouped apps or interleaved with them.

## Background Music

### Why isn't background music playing even though I turned it on?

The most common cause: Background Music also requires a **Custom Music Folder** to be set in Settings → Setup. With the toggle on but no folder configured, nothing plays. Also check:
1. **Music folder has valid files**: supported formats are MP3, OGG, FLAC, WAV, M4A, AAC, OPUS
2. **Context toggles**: "Play while browsing systems"/"games"/"during screensaver" each control when music plays — check the one for what you're currently doing
3. Music **never** plays during actual gameplay — this is by design, not a bug

### How do I control background music while playing?

There's a floating music-controls panel that appears briefly when a track changes. There's no dedicated volume slider in the app — the only volume control is the "During Video Playback" ducking setting, which lowers or pauses music while a video plays.

### Can music play during videos?

There are three modes, set via Settings → Background Music → "During Video Playback":
- **Unchanged** — music plays at full volume regardless of video
- **Lower volume** (default) — music drops to 20% while a video plays
- **Pause** — music pauses entirely while a video plays

### Is there a shuffle option?

Track selection is always random — there's no separate shuffle toggle because random is the only playback mode, and there's no sequential/ordered mode either.

## Video Playback

### Videos aren't playing. How do I fix this?

1. **Check the setting**: Settings → Video Playback → "Background Video" must be on (it's off by default)
2. **Check the delay**: there's a configurable 0-10 second delay (default 3s) before playback starts — until then, the widget canvas underneath stays visible
3. **File format**: supported formats are MP4, MKV, AVI, WMV, MOV, WEBM, placed in each system's `videos/` media folder
4. **Media folder**: verify Settings → Setup → Media folder is correct

### Why don't videos play during gameplay or the screensaver?

By design — video only plays while you're actively browsing a specific game, never during actual gameplay, the screensaver, or while browsing systems.

## Performance and Troubleshooting

### The app is laggy or slow. How can I improve performance?

Try these:
1. **Reduce blur/darken effects**: per-widget blur is more GPU-intensive than a plain image
2. **Fewer widgets**: too many active widgets can impact performance
3. **Turn off video/music** if you don't need them
4. **Smaller video files**: use lower-resolution videos if available

### The app doesn't survive device sleep. Is this normal?

Android may kill background apps to save battery. To improve retention, disable battery optimization for ES-DE Companion in Android's app settings. Some manufacturers are more aggressive about killing background apps than others.

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

ES-DE Companion reads ES-DE's own log file — it does not write anything to ES-DE's installation:

1. ES-DE writes every browsing/launch/screensaver event to `es_log.txt` in its own logs folder, once Custom Event Scripts, Browsing Custom Event Scripts, and Debug Mode are all enabled
2. ES-DE Companion tails that file in real time
3. Events are parsed and reduced into a single current state (idle, browsing a system, browsing a game, playing a game, or in the screensaver)
4. The companion screen updates its widgets and media accordingly

There is no scripts folder and no script generation — that mechanism from older versions of this app has been removed entirely.

### Can I use ES-DE Companion with other frontends?

No — ES-DE Companion is specifically built around ES-DE's own log file format and settings.

## Getting Help

**Documentation:**
- [User Guide](USERGUIDE.md) - Comprehensive documentation
- [README](README.md) - Project overview
- This FAQ

**Issues:**
- GitHub Issues: https://github.com/RobZombie9043/es-de-companion/issues

**ES-DE Resources:**
- ES-DE Documentation: https://gitlab.com/es-de/emulationstation-de
- ES-DE Discord: https://discord.gg/r2uubuxp
