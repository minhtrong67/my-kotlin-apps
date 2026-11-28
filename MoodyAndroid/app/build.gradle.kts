import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val appVersionName = "1.0.0"
val appVersionCode = 1

// APK file name: <App>-v<versionName>-build<versionCode>-<buildType>.apk  e.g. Moody-v1.0.0-build1-release.apk
base { archivesName.set("Moody-v$appVersionName-build$appVersionCode") }

// ---- Firebase config comes from ".env" (git-ignored) or from real environment variables (CI) ----
// No google-services.json and no google-services plugin: the values become the same string resources the plugin would
// generate (google_app_id, google_api_key, ...), which the Firebase SDK reads at startup.
// Without them the app still builds and runs in guest mode.
val envProps = Properties().apply {
    val f = rootProject.file(".env")
    if (f.exists()) f.reader(Charsets.UTF_8).use { load(it) }
}
fun envOf(key: String): String? =
    (System.getenv(key) ?: envProps.getProperty(key))?.trim()
        ?.removeSurrounding("\"")?.removeSurrounding("'")?.takeIf { it.isNotEmpty() }

// ---- Release signing: keystore.properties (git-ignored). Without it the release build is signed with the debug key. ----
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.reader(Charsets.UTF_8).use { load(it) }
}

val firebaseResources = mapOf(
    "google_app_id" to "FIREBASE_APP_ID",
    "google_api_key" to "FIREBASE_API_KEY",
    "gcm_defaultSenderId" to "FIREBASE_PROJECT_NUMBER",
    "project_id" to "FIREBASE_PROJECT_ID",
    "google_storage_bucket" to "FIREBASE_STORAGE_BUCKET",
    "default_web_client_id" to "GOOGLE_WEB_CLIENT_ID",
)
val firebaseConfigured = listOf("FIREBASE_APP_ID", "FIREBASE_API_KEY", "FIREBASE_PROJECT_ID", "GOOGLE_WEB_CLIENT_ID").all { envOf(it) != null }
if (!firebaseConfigured) logger.warn("Moody: .env is missing or incomplete -> building WITHOUT Firebase (guest mode only). See .env.example")

android {
    namespace = "com.minhtrong67.moody"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.minhtrong67.moody"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        // Keep only the languages the app ships (drops every other translation bundled by Material / Firebase / Play services).
        resourceConfigurations += listOf("en", "vi")

        if (firebaseConfigured) {
            firebaseResources.forEach { (resName, envKey) -> envOf(envKey)?.let { resValue("string", resName, it) } }
        }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // keystore.properties present -> your release key; otherwise the debug key, so the APK can still be sideloaded.
            signingConfig = if (keystoreProps.isNotEmpty()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.reorderable)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}
