# Moody — Android

A fast, private mood journal. Check in once a day with a mood, activities and a note, then look back at your history, calendar and statistics.

Native Android app written in **Kotlin** with **Jetpack Compose** and **Material Design 3** (including Material You dynamic color). It shares its data model with the Moody web app, so one Google account shows the same journal on both.

---

## Features

| Area | What you get |
|---|---|
| **Today** | Greeting, today's check-in card, streak, weekly average, week strip, recent entries. |
| **Check-in** | Five moods, activity chips, quick note, date & time picker, discard guard, swipe-friendly full-screen form. |
| **History** | One scrolling page: search, filters & sorting, swipe to edit / delete, month calendar with day details. |
| **Statistics** | Day / week / month / year views, mood and activity charts, animated wavy progress bars. |
| **Report image** | One tap creates a polished **1080 × 1920 (9:16)** PNG saved to *Pictures/Moody*. |
| **Reminders** | Daily check-in, weekly and monthly report reminders that work with the app closed. |
| **Theming** | 19 palettes, **Material You** (follows the wallpaper), light / dark / system, text size. |
| **Languages** | English and Vietnamese, switchable in-app without a restart. |
| **Data** | Guest mode (on-device) or Google sign-in with Firestore sync; export / import everything as JSON or CSV. |
| **Navigation** | Swipe between tabs; swipe right on *Today* to open the sidebar. |

## Tech stack

- Kotlin 2.0, Jetpack Compose (BOM 2024.12), Material 3, `HorizontalPager`
- Room (guest data) · DataStore (settings) · kotlinx.serialization
- Firebase Auth + Firestore, Google sign-in through Credential Manager
- AlarmManager + BroadcastReceivers for reminders
- Coil for avatars · `sh.calvin.reorderable` for drag-and-drop lists
- Gradle Kotlin DSL with a version catalog, KSP, R8 (minify + shrink resources)

Minimum Android version: **8.0 (API 26)** · Target / compile SDK: **35**

## Getting started

### Prerequisites
- Android Studio Ladybug (2024.2) or newer
- JDK 17
- An Android device or emulator (API 26+)

### Run it
```bash
git clone <your-repo-url> && cd MoodyAndroid
./gradlew installDebug          # Windows: .\gradlew.bat installDebug
```
Without Firebase configuration the app still runs in **guest mode** (data stays on the device).

### Enable Google sign-in and sync
Firebase settings live in a git-ignored **`.env`** file — no `google-services.json`, no Google Services plugin.

1. Copy `.env.example` to `.env`.
2. Fill in the values from *Firebase console → Project settings → Your apps → Android*:

   | Key | Meaning |
   |---|---|
   | `FIREBASE_PROJECT_ID` | Project ID |
   | `FIREBASE_PROJECT_NUMBER` | Project number (sender ID) |
   | `FIREBASE_STORAGE_BUCKET` | Storage bucket (optional) |
   | `FIREBASE_APP_ID` | Android app ID (`1:…:android:…`) |
   | `FIREBASE_API_KEY` | Web API key |
   | `GOOGLE_WEB_CLIENT_ID` | OAuth **web** client ID (client type 3) |

3. Register the package `com.minhtrong67.moody` in Firebase and add your **SHA-1** (`./gradlew signingReport`). Enable the Google provider under *Authentication*.
4. Rebuild. The same keys can be provided as environment variables on CI.

> The API key ends up inside the APK (this is how Firebase works). Restrict it to your package name + SHA-1 in Google Cloud Console and protect data with Firestore rules (`users/{uid}/…` readable only by its owner).

## Build a release APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/Moody-v1.0.0-build1-release.apk`

**File naming:** `Moody-v<versionName>-build<versionCode>-<buildType>.apk`. Change `appVersionName` / `appVersionCode` at the top of `app/build.gradle.kts` to bump a release.

**Signing:** copy `keystore.properties.example` to `keystore.properties` (git-ignored) and fill in your keystore details; the release build then uses your key. Without it the APK is signed with the debug key — fine for sideloading, not for publishing.

**Smooth first run:** an APK installed outside the Play Store is compiled lazily. Force ahead-of-time compilation once:
```bash
adb shell cmd package compile -m speed-profile -f com.minhtrong67.moody
```

## Reminders

Reminders use `AlarmManager` (not WorkManager), so they fire on time even in Doze.

- **Android 13+** asks for the notification permission when you enable a reminder.
- **Android 12+**: grant *Alarms & reminders* (Settings → Reminders → *Not getting notifications?*) for exact timing; otherwise they can be a few minutes late.
- **Samsung / Xiaomi / Oppo**: set Moody to *Unrestricted* in battery settings.
- Alarms are restored after reboot, app update, and time-zone or clock changes.
- The daily reminder is skipped if you already checked in today.

## Data import / export

Settings → **Export & import data** handles everything at once.

| Format | Export | Import |
|---|---|---|
| **JSON** | One file: `{ "app": "moody", "dataset": "all", "version": 1, "data": { entries, activities, moods } }` | Same file; a bare array is also recognised |
| **CSV** | A `.zip` with `entries.csv`, `activities.csv`, `moods.csv` | The `.zip`, or a single `.csv` (the type is detected from its columns) |

Files are compatible with the Moody web app. Imports merge into existing data (matched by id, or by date + time) and never delete anything.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Google sign-in and Firestore sync |
| `POST_NOTIFICATIONS` | Reminder notifications (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | On-time reminders (Android 12+, user-granted) |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule reminders after a reboot |

## Project structure

```
app/src/main/java/com/minhtrong67/moody/
├── data/        models, repository (Room ⇄ Firestore), local/ remote/ auth/
├── transfer/    JSON / CSV import & export
├── notify/      reminders (AlarmManager, receivers, notifications)
├── ui/
│   ├── theme/       palettes, Material 3 theme, Material You
│   ├── components/  cards, dialogs, segmented control, skeletons, charts
│   ├── screens/     Login, Today, Check-in, History, Stats, Settings, Manage
│   └── MoodyRoot.kt shell: pager, drawer, top bar, check-in overlay
└── util/        dates, statistics, report renderer
```

**Architecture:** single-activity, unidirectional data flow. `MainViewModel` exposes `StateFlow`s from `DataRepository`, which reads Room (guest) or Firestore (signed in) behind one API.

## Troubleshooting

| Problem | Fix |
|---|---|
| *Google sign-in fails* | Check the SHA-1 and package name in Firebase, that the Google provider is enabled, and `GOOGLE_WEB_CLIENT_ID` in `.env`. |
| *Sign-in button missing / "not set up"* | `.env` is missing or incomplete — see **Enable Google sign-in**. |
| *No reminders* | Open Settings → Reminders → *Not getting notifications?* and fix what is flagged. |
| *App closed unexpectedly* | On the next launch Moody shows the error text — tap **Copy** and share it. |
| *Choppy animations* | Use a release build and run the `compile -m speed-profile` command above. |

## Credits

Developed by **minhtrong67**, with help from **Claude AI**.
