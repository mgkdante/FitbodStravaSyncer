import java.util.Properties

val material3Version = "1.4.0-alpha15"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

val props = Properties().apply {
    val secrets = rootProject.file("local.properties")
    if (secrets.exists()) {
        secrets.inputStream().use(::load)
    } else {
        throw GradleException(
            "local.properties missing – add STRAVA_CLIENT_ID and STRAVA_CLIENT_SECRET"
        )
    }
}


android {
    namespace = "com.example.fitbodstravasyncer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fitbodstravasyncer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "STRAVA_CLIENT_ID",
            "\"${props["STRAVA_CLIENT_ID"]}\""
        )
        buildConfigField(
            "String",
            "STRAVA_CLIENT_SECRET",
            "\"${props["STRAVA_CLIENT_SECRET"]}\""
        )
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        // align with your Compose Compiler
        kotlinCompilerExtensionVersion = "1.4.6"
    }
}

dependencies {
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.connect.client)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.text.recognition)
    implementation(libs.coroutines.play.services)
    implementation(libs.androidx.connect.client)

    // Room & WorkManager
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    // Retrofit & Moshi for Strava OAuth & API calls
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    ksp(libs.moshi.kotlin.codegen)

    // EncryptedSharedPreferences for secure token storage
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.compose.material3:material3:$material3Version")
    // Window-size classes for responsive layouts
    implementation("androidx.compose.material3:material3-window-size-class:$material3Version")
    // Adaptive navigation suite for larger screens (e.g. multi-pane)
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:$material3Version")


}


