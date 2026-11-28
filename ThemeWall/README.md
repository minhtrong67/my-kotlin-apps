# ThemeWall

**Wallpapers that follow your theme.** ThemeWall is a lightweight native Android app that switches your home and lock screen wallpaper when the system (or your own schedule) changes between light and dark. Keep multiple wallpaper sets, change images by time of day, and switch sets in one tap.

Built with Kotlin, Jetpack Compose and **Material 3 Expressive** (Compose Material 3 1.5.0).

---

## Features

- **Automatic switching** – one light and one dark image per set, applied the moment the mode changes.
- **Custom light/dark times** – optionally ignore the system theme and switch by clock (for example light from 06:00, dark from 18:00).
- **Wallpaper sets** – create, rename, delete and select multiple sets; each holds its own light/dark pair and time-of-day images.
- **Time-of-day images** – optional Morning / Afternoon / Evening / Night slots per mode; empty slots fall back to the set's main image.
- **Built-in cropper** – pan and zoom (pinch or double-tap) in a frame that matches your screen; EXIF rotation is respected; images are stored as screen-sized JPEGs.
- **Apply target** – home screen, lock screen or both.
- **Dim dark image** – optional 0–60 % darkening of the dark wallpaper for better icon/text contrast.
- **Appearance** – System / Light / Dark app theme, dynamic color (Android 12+), English and Vietnamese with per-app language support.
- **Quiet by design** – a single minimum-priority service notification; in-app messages appear only when something goes wrong.
- **Resilient** – survives reboot and app updates, detects when the system stopped the background service and offers a one-tap fix.

## Design (Material 3 Expressive)

- Expressive theme with spring-based motion (`MotionScheme.expressive()`) and a vibrant tonal palette (dynamic color when available).
- Large flexible top app bar with subtitle, short navigation bar, shape-morphing buttons, shape-library accents (`MaterialShapes`), loading indicator, larger corner radii and grouped settings cards.
- Swipe between tabs; predictive back; haptics on toggles and selection.

## How it works

Android does not deliver the "dark mode changed" broadcast to manifest-declared receivers, so ThemeWall runs a small foreground service (`specialUse`) that registers a receiver at runtime.

1. The service listens for configuration, time and time-zone changes plus its own alarm.
2. Events are debounced (300 ms); OEM builds often fire several in a row.
3. The state key (mode + time slot) is compared with the last applied one; the wallpaper is written only when it differs.
4. Images are streamed from disk with `WallpaperManager.setStream`. Only when dimming is on is a bitmap composed.
5. One inexact `setAndAllowWhileIdle` alarm at a time covers custom light/dark times and time-of-day edges, so no exact-alarm permission is required.

> Apps cannot change the **system** theme without a special permission. ThemeWall's custom times switch the *wallpaper mode*. If you also want the system UI to follow a schedule, use your device's own scheduled dark mode (for example One UI: Settings → Display → Dark mode → Turn on as scheduled); ThemeWall follows it automatically.

## Requirements

| Item | Version |
|------|---------|
| Android | 8.0+ (API 26), targets API 35 |
| Android Gradle Plugin | 9.5.1 (built-in Kotlin, KGP 2.2.10) |
| Gradle | 9.6.0 |
| JDK | 17 or newer (tested with OpenJDK 21) |
| Compose BOM | 2026.11.01 (Material 3 1.5.0) |
| compileSdk | 37.1 (install *SDK Platform 37.1* from the SDK Manager) |
| Android Studio | a recent stable release that supports AGP 9.5 |

## Build

```bash
# Android Studio: open the project folder and press Run.
# Command line (generate the wrapper once if it is missing):
gradle wrapper --gradle-version 9.6.0
./gradlew assembleDebug      # Windows: .\gradlew.bat assembleDebug
./gradlew assembleRelease
```

Judge performance on the **release** build; debug builds of Compose apps are noticeably slower.

### APK naming

Artifacts follow `<AppName>-v<semver>-<buildType>.apk`:

```
app/build/outputs/apk/release/ThemeWall-v1.5.0-release.apk
app/build/outputs/apk/debug/ThemeWall-v1.5.0-debug.apk
```

`versionName` comes from `appVersion` in `app/build.gradle.kts`; `versionCode = MAJOR*10000 + MINOR*100 + PATCH` (1.5.0 → 10500), so bumping the version updates both.
The release build is signed with the debug keystore so it installs without setup. Use your own `signingConfig` for any wider distribution.

## Permissions

| Permission | Why |
|------------|-----|
| `SET_WALLPAPER` | Change home / lock screen wallpaper |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the theme watcher alive |
| `POST_NOTIFICATIONS` | Show the minimal service notification on Android 13+ |
| `RECEIVE_BOOT_COMPLETED` | Resume watching after reboot or app update |

No network access, analytics or ads. Images stay in private app storage; backups are disabled. Source/profile links open in your browser.

## Samsung / One UI notes

1. **Settings → Apps → ThemeWall → Battery → Unrestricted**.
2. Remove ThemeWall from **Settings → Battery → Background usage limits → Sleeping apps**.
3. Use a static lock screen; Samsung dynamic lock screen services may override the lock wallpaper.

To hide the notification entirely, long-press it and turn off the "Theme watcher" channel; the service keeps running.

## Project structure

```
app/src/main/java/com/gnort67/wallpaperswitcher/
├── MainActivity.kt          Entry point, service start, kill detection
├── Prefs.kt                 Settings + wallpaper sets (Compose-observable)
├── WallpaperApplier.kt      File layout, slots, custom times, applying wallpapers
├── ThemeWatcherService.kt   Foreground service, alarms, boot/update receiver
├── ImageTools.kt            Decoding, EXIF rotation, crop + JPEG export
├── Screens.kt               Expressive theme, navigation, Home, Settings, shared components
├── Sets.kt                  Wallpaper sets list and dialogs
├── Schedule.kt              Custom light/dark times and time-of-day slots
└── Crop.kt                  Pan/zoom crop screen
```

Image files live in `filesDir` as `wp_<setId>_<l|d>_<slot>.jpg` (`slot` 0 = main, 1–4 = morning to night).

## Performance notes

- R8 + resource shrinking in release, `profileinstaller` and a hand-written baseline profile (`app/src/main/baseline-prof.txt`; delete it if a release build ever fails on it).
- Previews use power-of-two subsampling in `RGB_565`; all file and bitmap work is off the main thread.
- The service does no polling: it wakes only for system events and its own alarm.

## Roadmap

- Quick Settings tile for toggling auto switch
- Separate images for home and lock screen
- Editable time-of-day boundaries

## Author

Created by **minhtrong67** · [GitHub profile](https://github.com/minhtrong67) · [Source code](https://github.com/minhtrong67/my-kotlin-apps)
