import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.screenshot)
}

// Release signing credentials, so `./gradlew bundleGplayRelease` produces an upload-ready AAB instead
// of requiring Studio's "Generate Signed App Bundle" wizard. Read from keystore.properties
// (gitignored) with an environment-variable fallback for CI. Both absent is not an error: the release
// build is left unsigned so a fresh clone still builds, and only the upload would fail.
// See the `release-signing` skill.
val signingProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    signingProps.getProperty(key)?.takeIf { it.isNotBlank() } ?: System.getenv(env)

val signingStoreFile = signingValue("storeFile", "BASIC_ROOT_CHECKER_STORE_FILE")
val signingStorePassword = signingValue("storePassword", "BASIC_ROOT_CHECKER_STORE_PASSWORD")
val signingKeyAlias = signingValue("keyAlias", "BASIC_ROOT_CHECKER_KEY_ALIAS")
val signingKeyPassword = signingValue("keyPassword", "BASIC_ROOT_CHECKER_KEY_PASSWORD")
val canSignRelease = listOf(
    signingStoreFile, signingStorePassword, signingKeyAlias, signingKeyPassword,
).all { it != null } && file(signingStoreFile!!).exists()

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
    // Set on the release build type, so it covers every flavor — bundleGplayRelease and
    // bundleFossRelease both sign from this one config.
    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // null when no credentials were found — see canSignRelease above.
            signingConfig = signingConfigs.findByName("release")
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

    // Coil for the remote app icons in the "Other apps" list. The catalog's own JSON parsing and its
    // OkHttp conditional GET moved to com.iboalali.appcatalog:data below, which declares them itself —
    // hence no direct kotlinx-serialization-json or okhttp here any more.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Shared "Other apps" catalog from the Android-Shared repo, resolved by the composite build wired
    // up in settings.gradle.kts. The version is ignored under dependency substitution.
    // `:ui` already exposes `:data` as an `api` dependency; both are declared because this app uses
    // both directly — the Application owns the repository, and OtherAppsCard draws the shared row.
    implementation("com.iboalali.appcatalog:data:1.0.0")
    implementation("com.iboalali.appcatalog:ui:1.0.0")

    // Shared TelemetryDeck lifecycle: the startup signal buffer, automated-test-traffic detection,
    // and the start-before-flush ordering. This app's own signal vocabulary stays in analytics/.
    // `telemetrydeck-sdk` below is still declared directly because Analytics calls TelemetryDeck
    // itself, rather than relying on this module's `api` dependency to supply it.
    implementation("com.iboalali.telemetry:core:1.0.0")

    // Shared haptics — the engine this app wrote, now shared. `RootHaptics` keeps what is genuinely
    // this app's: the checking ramp and the three outcome buzzes, plus the capability signal. Both
    // modules are declared because `RootHaptics` names `:core` types (`Haptics`, `HapticWaveform`)
    // directly, not only through `:compose`.
    implementation("com.iboalali.haptics:core:1.0.0")
    implementation("com.iboalali.haptics:compose:1.0.0")

    // Shared adaptive detail overlay: the ≥840dp container-transform card, its scene strategy, the
    // anchor state that bridges the overflow menu's Popup to it, and the leading nav icon. This app
    // wrote neither — Billboard did — but adopting it is what brings the two fixes this copy lacked:
    // the `isTraversalGroup` semantics on the overlay root, and a scene with value-based
    // equals/hashCode instead of a reference-equality anonymous object.
    implementation("com.iboalali.nav3:overlay:1.0.0")

    // The app-bar overflow menu and its items. The shared `AppBarDropdownMenuItem` wraps its own
    // `onClick` in `rememberHapticClick`, which this app's local copy did *not* — so its menu items
    // were the only silent tap targets of the three apps, and adopting this fixes that. The call
    // sites in `MainScreen` dropped their own `rememberHapticClick` wrappers to avoid a double tick.
    implementation("com.iboalali.ui:menu:1.0.0")

    // The ColorScheme cross-fade, which this app hand-wrote over 36 of Material's 48 colour roles.
    // The library's copy covers all 48 and has a test that fails when Material adds one.
    implementation("com.iboalali.ui:theme:1.0.0")

    // Shared Play Console screenshot matrices and the constrained-device stress specs. Declared on
    // `implementation`, not `screenshotTestImplementation`, because this app's constrained-device
    // preview *functions* live in `src/main` (util/ConstrainedDevicePreviews.kt) rather than in the
    // screenshotTest source set. Moving them would let this drop to screenshotTest only.
    implementation("com.iboalali.previews:matrix:1.0.0")

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

    // Compose Preview Screenshot Testing — @PreviewTest marker + the tooling that renders previews
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    // Generated Baseline Profile, produced by the :baselineprofile module
    baselineProfile(project(":baselineprofile"))
}
