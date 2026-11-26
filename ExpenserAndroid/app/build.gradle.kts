import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------------------------------------------------------------------------------------------------------------
// Secrets live in <project>/.env (git-ignored). Real environment variables win over the file. See .env.example.
// ---------------------------------------------------------------------------------------------------------------
val dotEnv: Map<String, String> = buildMap {
    val f = rootProject.file(".env")
    if (f.exists()) f.readLines(Charsets.UTF_8).forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val i = line.indexOf('=')
        if (i > 0) put(line.substring(0, i).trim(), line.substring(i + 1).trim().removeSurrounding("\"").removeSurrounding("'"))
    }
}
fun envVar(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() } ?: dotEnv[name]?.takeIf { it.isNotBlank() }

// Firebase config: GOOGLE_SERVICES_JSON_BASE64 in .env is decoded into app/google-services.json (git-ignored) before the
// Google Services plugin runs. Without it, a hand-placed app/google-services.json still works.
val googleServicesFile = file("google-services.json")
envVar("GOOGLE_SERVICES_JSON_BASE64")?.let { b64 ->
    val bytes = Base64.getMimeDecoder().decode(b64)
    if (!googleServicesFile.exists() || !googleServicesFile.readBytes().contentEquals(bytes)) googleServicesFile.writeBytes(bytes)
}
if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val appVersionName = "1.8.2"
// APK files are named Expenser-<version>-<buildType>.apk
base.archivesName.set("Expenser-$appVersionName")

android {
    namespace = "com.expenser.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.expenser.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = appVersionName
        buildConfigField("String", "IMGBB_API_KEY", "\"${envVar("IMGBB_API_KEY") ?: ""}\"")
    }

    // Release signing comes from .env (KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD). An unsigned APK cannot be
    // installed, so without a keystore the release build is signed with the debug key instead (fine for side-loading).
    val releaseKeystore = envVar("KEYSTORE_FILE")?.let { rootProject.file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) create("release") {
            storeFile = releaseKeystore
            storePassword = envVar("KEYSTORE_PASSWORD")
            keyAlias = envVar("KEY_ALIAS")
            keyPassword = envVar("KEY_PASSWORD") ?: envVar("KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
}
