plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.patrimonialima"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.patrimonialima"
        minSdk = 24
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Jetpack Compose (usa el BOM para alinear versiones automáticamente)
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Navigation para Jetpack Compose
    implementation("androidx.navigation:navigation-compose:2.8.2")
    //implementation(libs.firebase.auth.ktx) // o la versión compatible con tu Compose
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Iconos extendidos (History, Map, Logout, CameraAlt, Lightbulb, etc.)
    implementation("androidx.compose.material:material-icons-extended")

    // Solo necesario para MapaInteractivoScreen.kt
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    implementation("com.google.firebase:firebase-analytics")

// Usar una versión de BoM compatible con compilador Kotlin previo
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // Sin versiones explícitas (el BoM definirá las compatibles)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("org.osmdroid:osmdroid-android:6.1.18")

    implementation("org.mapsforge:mapsforge-map-android:0.25.0")

    implementation("org.mapsforge:mapsforge-themes:0.25.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(kotlin("test"))
}