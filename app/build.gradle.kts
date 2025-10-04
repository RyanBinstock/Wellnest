plugins {
    id("com.android.application")
    id("androidx.navigation.safeargs")   // Java Safe Args
    id("com.google.gms.google-services")
}

android {
    namespace = "com.code.wlu.cp470.wellnest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.code.wlu.cp470.wellnest"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { viewBinding = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout.v214)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
}