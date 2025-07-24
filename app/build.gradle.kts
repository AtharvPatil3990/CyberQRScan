plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.cyberqrscan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cyberqrscan"
        minSdk = 26
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.scenecore)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.play.services.code.scanner)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.navigation.fragment) // This likely points to the non-ktx version via your libs.versions.toml
    implementation(libs.navigation.ui)       // This likely points to the non-ktx version via your libs.versions.toml
    implementation (libs.navigation.fragment.ktx) // You have this
    implementation (libs.navigation.ui.ktx)     // And this
    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
// ML Kit Barcode Scanning (Google Play Services version)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
// Material Design Components
    implementation("com.google.android.material:material:1.11.0")
// CardView
    implementation("androidx.cardview:cardview:1.0.0")
// Lifecycle (used with CameraX)
    implementation("androidx.lifecycle:lifecycle-runtime:2.6.2")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
// Optional: For backward compatibility
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.guava:guava:31.1-android")

    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
}