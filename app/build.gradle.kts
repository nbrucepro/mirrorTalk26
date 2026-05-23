plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.mirrortalk26"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mirrortalk26"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    // ── Core Android ───────────────────────────────────────
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ── Navigation Component ────────────────────────────────
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")

    // ── ViewModel + LiveData ────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")

    // ── Room Database ───────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // ── CameraX ─────────────────────────────────────────────
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // ── ML Kit Face Detection ────────────────────────────────
    implementation("com.google.mlkit:face-detection:16.1.5")

    // ── MPAndroidChart ───────────────────────────────────────
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // - RecyclerView dependency
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ── Testing ──────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    // ── CameraX Video ───────────────────────────────────────
    implementation("androidx.camera:camera-video:1.3.1")
}