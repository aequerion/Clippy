# Clippy - Android Clipboard Manager

A modern, privacy-focused clipboard manager for Android inspired by [Maccy](https://maccy.app/) for macOS. Clippy automatically saves your clipboard history and provides quick access via the app and a persistent notification.

## Features

### Current Version (v1.0.0)

- ✅ **Automatic Clipboard Capture** - Monitors clipboard changes in the background
- ✅ **Text History** - Stores text clipboard items with timestamps
- ✅ **Pin Favorites** - Pin important clips to keep them at the top
- ✅ **Quick Copy** - Tap any item to copy it back to clipboard
- ✅ **Persistent Notification** - Quick access to recent clips from notification
- ✅ **Configurable History Size** - Set maximum items to keep (10-500)
- ✅ **Dark Theme** - Beautiful GitHub-inspired dark theme
- ✅ **Undo Delete** - Restore accidentally deleted items
- ✅ **Boot Persistence** - Service restarts after device reboot

### Planned Features

See [planning.md](planning.md) for the full roadmap including:
- Image clipboard support
- Search functionality
- Privacy features (exclude apps, auto-delete sensitive content)
- Cloud sync
- Widgets

## Screenshots

*Coming soon*

## Requirements

- Android 8.0 (API 26) or higher
- Notification permission (Android 13+)

## Installation

### From Play Store
*Coming soon*

### Build from Source

1. Clone the repository:
```bash
git clone https://github.com/yourusername/clippy.git
cd clippy
```

2. Open in Android Studio (Hedgehog or newer recommended)

3. Build and run:
```bash
./gradlew assembleDebug
```

## Architecture

Clippy follows modern Android development best practices:

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt
- **Database**: Room
- **Preferences**: DataStore
- **Async**: Kotlin Coroutines & Flow

### Project Structure

```
app/src/main/java/com/clippy/app/
├── ClippyApplication.kt          # Application class
├── data/
│   ├── database/                 # Room database
│   │   ├── ClipEntity.kt
│   │   ├── ClipDao.kt
│   │   └── ClipDatabase.kt
│   ├── repository/
│   │   └── ClipRepository.kt
│   └── preferences/
│       └── PreferencesManager.kt
├── di/
│   └── AppModule.kt              # Hilt modules
├── service/
│   ├── ClipboardService.kt       # Foreground service
│   └── BootReceiver.kt
├── ui/
│   ├── theme/                    # Compose theme
│   ├── main/                     # Main screen
│   └── settings/                 # Settings screen
└── util/
    ├── NotificationHelper.kt
    └── CopyBroadcastReceiver.kt
```

## Theme

Clippy uses a GitHub-inspired dark theme (GitGraph):

| Element | Color |
|---------|-------|
| Background | `#0D1117` |
| Card | `#161B22` |
| Border | `#30363D` |
| Text Primary | `#FFFFFF` |
| Text Secondary | `#8B949E` |
| Accent Green | `#238636` |
| Link Blue | `#58A6FF` |
| Error Red | `#F85149` |

## Privacy

Clippy is designed with privacy in mind:

- All data is stored locally on your device
- No analytics or tracking
- No internet permission required
- No data leaves your device

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by [Maccy](https://maccy.app/) for macOS
- Theme inspired by GitHub's dark mode
- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)

---

Made with ❤️ for Android
