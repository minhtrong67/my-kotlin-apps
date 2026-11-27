plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val appVersionName = "1.0.0"
base.archivesName.set("CalendarPro-v$appVersionName")

android {
    namespace = "com.gnort67.calendarpro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gnort67.calendarpro"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = appVersionName
        vectorDrawables.useSupportLibrary = true
        // Chỉ giữ tài nguyên vi/en để APK gọn nhẹ
        resourceConfigurations += listOf("vi", "en")
    }

    buildTypes {
        release {
            // Ký bằng khoá debug để APK release cài được ngay trên máy thật.
            // Khi phát hành lên Google Play, hãy thay bằng keystore riêng của bạn.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = false // icon được tra theo tên (getIdentifier) nên không được shrink
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation("junit:junit:4.13.2")
}
