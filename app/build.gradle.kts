plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.iboalali.basicrootchecker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iboalali.basicrootchecker"
        minSdk = 23
        targetSdk = 36
        versionCode = 42
        versionName = "v2.0vc$versionCode"
        @Suppress("UnstableApiUsage")
        androidResources.localeFilters += listOf("en", "ar", "de")
        buildConfigField("String", "TELEMETRY_DECK_APP_ID", "\"613251CD-B223-443A-9583-3A18586FAB55\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            versionNameSuffix = "-debug"
            applicationIdSuffix = ".debug"
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("gplay") {
            dimension = "distribution"
            isDefault = true
        }
        create("foss") {
            dimension = "distribution"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.annotation)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.core)

    // Material (for DynamicColors)
    implementation(libs.google.material)

    // Third-party
    implementation(libs.boehrsi.devicemarketingnames)
    implementation(libs.telemetrydeck.sdk)
    implementation(libs.topjohnwu.libsu.core)

    // In-app updates (gplay flavor only)
    "gplayImplementation"(libs.google.play.app.update.ktx)
}
