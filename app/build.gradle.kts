plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.iboalali.basicrootchecker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.iboalali.basicrootchecker"
        minSdk = 23
        targetSdk = 37
        versionCode = 80
        versionName = "v2.5.0vc$versionCode"
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

    // Compose Preview Screenshot Testing — renders @PreviewTest composables to PNGs on the JVM
    // (no device). Required alongside the gradle.properties flag of the same name to apply the
    // com.android.compose.screenshot plugin and enable the screenshotTest source set.
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    compileOptions {
        // Required so java.time.* (used transitively by TelemetryDeck via kotlinx-datetime)
        // resolves on API < 26, where it is not part of the platform.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// AppFunctions (androidx.appfunctions alpha10): the @AppFunctionServiceEntryPoint compiler generates
// the concrete RootAppFunctionService plus its assets/root_app_function_service.xml from the
// @AppFunction methods on BaseRootAppFunctionService. The entry-point path needs no aggregate ksp
// arg (alpha09's "appfunctions:aggregateAppFunctions" is gone); the generated service is declared in
// AndroidManifest.xml.

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
    // via an @AppFunctionServiceEntryPoint service. alpha10 consolidated the old -service artifact
    // into this one; -compiler (KSP) generates the service class + its function XML.
    implementation(libs.androidx.appfunctions)
    ksp(libs.androidx.appfunctions.compiler)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.core)

    // Remote "Other apps" catalog: JSON parsing for the apps.json feed, OkHttp for the conditional
    // GET (its Cache owns the ETag/Last-Modified revalidation — see CatalogHttpSource), and Coil for
    // the remote app icons. Coil already pulls OkHttp in transitively; it's declared explicitly
    // because the catalog uses it directly, rather than relying on another library's dependency.
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
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
    // Pins the catalog's conditional-GET contract against a local server (see CatalogHttpSourceTest)
    testImplementation(libs.okhttp.mockwebserver)

    // Compose Preview Screenshot Testing — @PreviewTest marker + the tooling that renders previews
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    // Generated Baseline Profile, produced by the :baselineprofile module
    baselineProfile(project(":baselineprofile"))
}
