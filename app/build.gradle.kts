plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.sanaker.stromapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sanaker.stromapp"
        minSdk = 24
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
    kotlinOptions {
        jvmTarget = "11"
    }
    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // --- Legg til disse avhengighetene ---
    // Sjekk om okhttp, gson og coil er riktig definert i libs.versions.toml.
    // Hvis ikke, bruk direkte strenger som vist nedenfor:
    implementation(libs.okhttp) // For HTTP-kall, erstatt med riktig versjon
    implementation(libs.gson.v2101)     // For JSON-parsing, erstatt med riktig versjon
    implementation(libs.coil)                 // For bildevisning fra URL, erstatt med riktig versjon

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx) // For lifecycleScope

    // Ktor Client for HTTP requests
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio) // For CIO engine (async I/O)
    implementation(libs.ktor.client.content.negotiation) // For JSON parsing
    implementation(libs.ktor.serialization.kotlinx.json) // Kotlinx Serialization for Ktor

    // Kotlinx Coroutines for asynchronous operations
    implementation(libs.kotlinx.coroutines.android)

    // Optional: Logging for Ktor (useful for debugging network requests)
    implementation(libs.ktor.client.logging)
    // implementation(libs.logback.classic) // or another logging implementation
    implementation(libs.kotlin.reflect) // Bruk din Kotlin-versjon
    implementation (libs.androidx.swiperefreshlayout)// Or a newer stable version if available
    implementation (libs.mpandroidchart)
    implementation(libs.androidx.constraintlayout)
}