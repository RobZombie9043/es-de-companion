# ES-DE Second Screen Companion

![Version](https://img.shields.io/badge/version-0.1.2-blue)
![Android](https://img.shields.io/badge/Android-10%2B-green)

A companion app for [ES-DE](https://es-de.org/) that displays beautiful game artwork and marquees on a secondary display, transforming your dual-screen device into an immersive retro gaming interface.

> **Note**: This app is **not officially affiliated with ES-DE**. It is an independent, community-created companion application.

> **Note**: This app was developed using AI assistance, the full source code has been made available.

## 🎮 Features

### Dynamic Display
- **Real-time artwork display** - Shows game fanart, screenshots, and marquees as you browse in ES-DE
- **Video playback support** - Play game videos (MP4, MKV, AVI, WMV, MOV, WebM) when browsing games
  - Configurable delay (instant to 5 seconds) before video starts
  - Optional audio control (muted by default)
  - Respects animation settings (fade, scale+fade, custom)
- **System view support** - Displays built-in system logos, custom images, or random game artwork when browsing systems
- **Smooth animations** - Configurable fade and scale effects with custom timing options
- **Background customization** - Adjustable dimming and blur effects

### App Drawer
- **Full Android app launcher** - Access all your installed apps from one place
- **Smart search** - Quickly find apps with the built-in search bar
- **Customizable grid** - Adjust column count to your preference
- **App visibility control** - Hide apps you don't want to see in the drawer
- **Long-press menu** - Configure launch behavior for each app
  - Open app info/settings
  - Choose display (top/bottom screen) for launching apps
  - Per-app preferences are saved

### Easy Setup
- **Quick Setup Wizard** - Step-by-step configuration on first launch
- **Auto-script creation** - Automatically generates ES-DE integration scripts

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

All settings can be customized in the Settings screen.

### File Paths

The app uses these default paths (configurable in settings):

| Path | Default Location | Purpose |
|------|-----------------|---------|
| **Downloaded Media** | `/storage/emulated/0/ES-DE/downloaded_media` | Game artwork - fanart, screenshots, marquees |
| **Videos** | `/storage/emulated/0/ES-DE/downloaded_media/{system}/videos` | Game videos (optional) |
| **System Images** | `/storage/emulated/0/ES-DE/downloaded_media/systems` | Custom system images (optional override) |
| **Scripts** | `/storage/emulated/0/ES-DE/scripts` | Integration scripts |

### Custom System Images (Optional Override)

To override random game artwork in system view with your own images:
1. Place custom images in the system images folder
2. Use filenames matching ES-DE system shortnames: `snes.webp`, `arcade.png`, `psx.jpg`, etc.
3. These will take priority over built-in logos

### Game Videos (Optional)

To add video playback support:
1. Place video files in the videos folder within each system's directory:
   - `/downloaded_media/{system}/videos/{gamename}.mp4`
   - Example: `/downloaded_media/snes/videos/Super Mario World.mp4`
2. Supported formats: MP4, MKV, AVI, WMV, MOV, WebM
3. Enable video playback in Settings → Media Settings → Video Playback
4. Configure delay and audio preferences as desired

**Note**: Videos are resource-intensive. Use moderate resolutions (1080p or lower) for best performance.

## 🎨 How It Works

1. **ES-DE Integration**: The app creates custom event scripts that ES-DE calls when you browse games/systems
2. **Real-time Updates**: Scripts write to log files that the app monitors using FileObserver
3. **Image Loading**: The app reads game metadata and displays corresponding artwork

### Script Files

The Quick Setup creates these scripts automatically:

```
ES-DE/scripts/
├── game-select/
│   └── game-select.sh       # Triggered when browsing games
└── system-select/
    └── system-select.sh     # Triggered when browsing systems
```

## 🤝 Contributing

Contributions are welcome! Please feel free to open an Issue or submit a Pull Request.

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 🙏 Credits

- Built for [ES-DE](https://es-de.org/) by Leon Styhre
- Works best in a dual home sceen set up using [Mjolnir](https://github.com/blacksheepmvp/mjolnir) home screen manager
- Uses [Glide](https://github.com/bumptech/glide) for efficient image loading
- Uses [AndroidSVG](https://github.com/BigBadaboom/androidsvg) for SVG rendering
- Uses [ExoPlayer](https://github.com/google/ExoPlayer) for video playback

## 📞 Support

If you encounter any issues or have questions:
1. Check the [Issues](../../issues) page
2. Create a new issue with details about your problem

## 🔄 Changelog

### [0.1.2] - 2026-01-03 - Various Presentation and Bug Fixes

### [0.1.1] - 2026-01-03 - Video Playback Release
- 🎬 **Video playback support** - Play game videos (MP4, MKV, AVI, WMV, MOV, WebM)
  - Configurable video delay (instant to 5 seconds)
  - Optional audio control (muted by default)
  - Videos respect animation settings
  - Smart video/image transitions
- 🎨 **UI improvements**
  - Renamed "Game Logo" to "Game Marquee" for clarity
  - Simplified custom animation settings (removed redundant Effect Type selector)
  - Added helpful descriptions for animation settings
  - Cleaned up settings layout with grouped video controls
  - Logo Size slider now hidden when both logos are disabled
- 🔧 **App launch improvements**
  - Changed default app launch to "This Screen" instead of secondary display
  - Apps now launch on primary screen by default (configurable per-app)
- ⚡ **Performance optimizations**
  - Video only reloads when video settings change
  - Smooth resume when returning from settings with no changes
  - Proper video timer cancellation when opening settings
  - Marquee content pre-loads for smooth video transitions

### [0.1.0] - 2026-01-02 - Initial Release
- ✨ Added built-in system logos for all supported ES-DE systems (SVG format)
- 🔧 Separate on/off controls for system and game logos
- 📏 Shared logo size control (Small/Medium/Large)
- 🎨 Text fallback display for systems without logos
- ✨ Advanced animation system with 4 styles (None, Fade, Scale + Fade, Custom)
- ⚙️ Custom animation controls (adjustable duration 100-500ms, scale amount 85-100%)
- ✨ Added long-press menu in app drawer
- 🎯 Per-app display launch preferences (top/bottom screen)
- 🚀 App drawer auto-closes after launching apps
- 🔧 Quick Setup wizard with ES-DE script configuration
- 🎨 Customizable visual effects (dimming, blur, animations)
- 📱 App drawer with search and visibility controls
- 🖼️ Real-time game artwork and marquee display
- 🔍 Smart path detection and configuration

---

**Enjoy your enhanced ES-DE dual screen experience!** 🎮✨
