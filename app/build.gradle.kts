configurations.all {
    exclude(group = "com.google.protobuf", module = "protobuf-lite")
}


plugins {
    id("com.android.application")
    id("androidx.navigation.safeargs")
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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }
    }

    buildFeatures { viewBinding = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // AndroidX UI
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.espresso.contrib)
    implementation(libs.firebase.database)

    // Unit tests
    testImplementation(libs.junit4)
    testImplementation(libs.mockito.core)
    testImplementation(libs.hamcrest)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit.jupiter)

    // FragmentScenario (debug only)
    debugImplementation(libs.androidx.fragment.testing)

    // Instrumented tests — aligned to Espresso 3.5.x matrix
    androidTestImplementation(libs.androidx.test.core)        // 1.5.0
    androidTestImplementation(libs.androidx.test.runner)      // 1.5.2
    androidTestImplementation(libs.androidx.test.rules)       // 1.5.0
    androidTestImplementation(libs.androidx.test.ext.junit)   // 1.1.5
    androidTestImplementation(libs.espresso.core)             // 3.5.1
    implementation(libs.protobuf.javalite)
    implementation(libs.okhttp3.okhttp)
}
