plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.iboalali.basicrootchecker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.iboalali.basicrootchecker"
        minSdk = 23
        targetSdk = 37
        versionCode = 70
        versionName = "v2.4.0vc$versionCode"
        @Suppress("UnstableApiUsage")
        androidResources.localeFilters += listOf("en", "ar", "de", "es", "ru")
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
        // Required so java.time.* (used transitively by TelemetryDeck via kotlinx-datetime)
        // resolves on API < 26, where it is not part of the platform.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ksp {
    // Aggregate all @AppFunction metadata into a single schema so the system can discover them.
    arg("appfunctions:aggregateAppFunctions", "true")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.annotation)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Adaptive: window size class (currentWindowAdaptiveInfoV2; brings androidx.window:window-core)
    // used to switch the secondary screens to a dialog on large screens.
    implementation(libs.androidx.compose.material3.adaptive)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // AppFunctions — expose root-check workflows to the system / on-device agents (both flavors)
    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.appfunctions.service)
    ksp(libs.androidx.appfunctions.compiler)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.core)

    // Remote "Other apps" catalog: JSON parsing for the apps.json feed + Coil image loading for
    // the remote app icons (coil-network-okhttp pulls OkHttp transitively, used only by Coil).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Material (for DynamicColors)
    implementation(libs.google.material)

    // Third-party
    implementation(libs.boehrsi.devicemarketingnames)
    implementation(libs.telemetrydeck.sdk)
    implementation(libs.topjohnwu.libsu.core)

    // In-app updates (gplay flavor only)
    "gplayImplementation"(libs.google.play.app.update.ktx)

    // Tip jar / in-app billing (gplay flavor only)
    "gplayImplementation"(libs.google.play.billing.ktx)

    // In-app review / rating (gplay flavor only)
    "gplayImplementation"(libs.google.play.review.ktx)

    // Installs the shipped baseline profile (assets/dexopt/baseline.prof) at runtime
    implementation(libs.androidx.profileinstaller)

    // Unit tests
    testImplementation(libs.junit)

    // Generated Baseline Profile, produced by the :baselineprofile module
    baselineProfile(project(":baselineprofile"))
}
