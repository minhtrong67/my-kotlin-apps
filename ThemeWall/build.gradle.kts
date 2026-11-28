plugins {
    id("com.android.application") version "9.5.1" apply false
    // AGP 9 has built-in Kotlin (KGP 2.2.10). The Compose plugin pins the Kotlin version.
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
