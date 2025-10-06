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
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins.create("java") { option("lite") }
        }
    }
}