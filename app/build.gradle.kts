plugins {
    id("com.android.application")
    id("androidx.navigation.safeargs")
    id("com.google.gms.google-services")

    alias(libs.plugins.protobuf)
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

    // Room Backend
    implementation(libs.room.runtime)
    testImplementation(libs.junit.jupiter)
    annotationProcessor(libs.room.compiler)

    // --- DataStore (Proto) for Java via Rx wrappers ---
    implementation(libs.datastore)
    implementation(libs.datastore.rxjava3)
    implementation(libs.protobuf.javalite)

    // Unit tests
    testImplementation(libs.junit4)
    testImplementation(libs.mockito.core)
    testImplementation(libs.hamcrest)
    testImplementation(libs.robolectric)

    // Instrumented tests
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins.create("java") { option("lite") }
        }
    }
}