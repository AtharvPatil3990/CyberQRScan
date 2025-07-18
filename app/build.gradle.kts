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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.navigation.fragment) // This likely points to the non-ktx version via your libs.versions.toml
    implementation(libs.navigation.ui)       // This likely points to the non-ktx version via your libs.versions.toml
    implementation (libs.navigation.fragment.ktx) // You have this
    implementation (libs.navigation.ui.ktx)     // And this
}