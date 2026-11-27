<div align="center">

# 📱 my-kotlin-apps

**Pure native, super lightweight Android apps built with Kotlin and Material Design 3 Expressive.**

[![Kotlin](https://img.shields.io/badge/Kotlin-Native-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Material Design 3](https://img.shields.io/badge/Material%20Design%203-Expressive-6750A4?logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Android Studio](https://img.shields.io/badge/IDE-Android%20Studio-4285F4?logo=androidstudio&logoColor=white)](https://developer.android.com/studio)
[![Download APK](https://img.shields.io/badge/Download-APK%20on%20MEGA-D9272E?logo=mega&logoColor=white)](https://mega.nz/folder/BEx3TKBC#NNAWeixkXGiB4E6AUvZnWQ)

</div>

---

## 📖 Overview

**my-kotlin-apps** is a monorepo that collects a set of small, focused Android applications. Every app follows the same principles:

- **Pure native**: written in Kotlin with the Android SDK, with no cross-platform runtime.
- **Super lightweight**: minimal dependencies, small APK size, fast startup.
- **Modern design**: built around the [Material Design 3 (Expressive)](https://m3.material.io/) design language.

Each app lives in its own folder and is an independent Android Studio project, so you can open, build and run them separately.

## 🗂️ Apps

| App | Folder | Description |
| --- | ------ | ----------- |
| 📅 **CalendarPro** | [`CalendarPro`](./CalendarPro) | A calendar app for viewing dates and managing your schedule. |
| 💸 **Expenser** | [`ExpenserAndroid`](./ExpenserAndroid) | An expense tracker for recording and reviewing personal spending. |
| 🙂 **Moody** | [`MoodyAndroid`](./MoodyAndroid) | A mood tracker for logging how you feel day by day. |
| 🎨 **ThemeWall** | [`ThemeWall`](./ThemeWall) | A theme and wallpaper app for personalizing your device. |

## 📥 Download

Prebuilt APK files for all apps are available on MEGA:

<div align="center">

[![Download from MEGA](https://img.shields.io/badge/⬇%20Download%20all%20apps-MEGA-D9272E?style=for-the-badge&logo=mega&logoColor=white)](https://mega.nz/folder/BEx3TKBC#NNAWeixkXGiB4E6AUvZnWQ)

</div>

**How to install**

1. Download the APK of the app you want from the MEGA folder above.
2. On your Android device, allow installation from unknown sources when prompted (**Settings → Security / Apps → Install unknown apps**).
3. Open the downloaded APK and tap **Install**.

> ⚠️ These APKs are installed outside of Google Play. Only download them from the link in this repository.

## ✨ Highlights

- 🎯 100% Kotlin, native Android
- 🪶 Lightweight and fast, with minimal third-party dependencies
- 🎨 Material Design 3 Expressive UI with dynamic color support
- 🌗 Light and dark theme friendly
- 📦 Independent projects: build only what you need

## 🧰 Tech Stack

| Category | Technology |
| -------- | ---------- |
| Language | [Kotlin](https://kotlinlang.org/) |
| Platform | Android (native) |
| UI / Design | [Material Design 3 Expressive](https://m3.material.io/) |
| Build system | Gradle |
| IDE | [Android Studio](https://developer.android.com/studio) |

## 🚀 Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable version recommended)
- JDK 17 or newer
- Android SDK installed through Android Studio
- An Android device or emulator

### Clone the repository

```bash
git clone https://github.com/minhtrong67/my-kotlin-apps.git
cd my-kotlin-apps
```

### Run an app

1. Open **Android Studio** and choose **File → Open**.
2. Select one of the app folders (for example `CalendarPro`).
3. Wait for Gradle sync to finish.
4. Pick a device or emulator and click **Run ▶**.

### Build from the command line

```bash
cd CalendarPro        # or ExpenserAndroid, MoodyAndroid, ThemeWall
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/`.

## 📁 Repository Structure

```text
my-kotlin-apps/
├── CalendarPro/        # Calendar app
├── ExpenserAndroid/    # Expense tracker app
├── MoodyAndroid/       # Mood tracker app
├── ThemeWall/          # Theme & wallpaper app
└── README.md
```

## 🤝 Contributing

Contributions, issues and feature requests are welcome.

1. Fork this repository
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m "Add amazing feature"`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## 📄 License

No license has been specified yet. Consider adding a [LICENSE](https://choosealicense.com/) file (for example MIT or Apache 2.0) to define how others may use this code.

## 👤 Author

**minhtrong67**

- GitHub: [@minhtrong67](https://github.com/minhtrong67)

---

<div align="center">

⭐ If you find this project useful, consider giving it a star!

</div>
