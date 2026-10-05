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
        minSdk = 24
        targetSdk = 37
        versionCode = 82
        versionName = "v2.6.0-alpha1vc$versionCode"
        @Suppress("UnstableApiUsage")
        androidResources.localeFilters +=
            listOf("en", "ar", "de", "es", "ru", "nl", "zh", "ms", "ta")
        buildConfigField("String", "TELEMETRY_DECK_APP_ID", "\"613251CD-B223-443A-9583-3A18586FAB55\"")
    }
    // Set on the release build type, so it covers every flavor: bundleGplayRelease and
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
            // null when no credentials were found (see canSignRelease above).
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

    // Required alongside the gradle.properties flag of the same name to apply the
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

// The screenshot tasks fork their own JVM, which inherits nothing from org.gradle.jvmargs and
// defaults to a heap far too small for this matrix. 175 previews at native store resolution (the
// 10-inch slot alone is 2560x1600) exhaust it, and the task dies with "Java heap space" rather than
// with anything naming the real cause. Raised on the render tasks only, so the unit tests stay on
// the default.
tasks.withType<Test>().matching { it.name.endsWith("ScreenshotTest") }.configureEach {
    maxHeapSize = "4g"
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
    // Window size class (currentWindowAdaptiveInfoV2), which gates the large-screen detail overlay.
    implementation(libs.androidx.compose.material3.adaptive)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // AppFunctions, in both flavors. The KSP compiler generates the concrete RootAppFunctionService
    // and assets/root_app_function_service.xml from the @AppFunction methods on
    // BaseRootAppFunctionService. The generated service is declared in AndroidManifest.xml.
    implementation(libs.androidx.appfunctions)
    ksp(libs.androidx.appfunctions.compiler)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.core)

    // Coil for the remote app icons in the "Other apps" list. The catalog's JSON parsing and HTTP
    // come from com.iboalali.appcatalog:data, which declares its own dependencies.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Shared "Other apps" catalog from the Android-Shared repo. Every `libs.shared.*` coordinate
    // below resolves from the `maven-repo` checkout at the single `shared` version in
    // libs.versions.toml. See settings.gradle.kts for that and for the composite-build switch.
    // `:ui` already exposes `:data` as an `api` dependency. Both are declared because this app uses
    // both directly: the Application owns the repository, and OtherAppsCard draws the shared row.
    implementation(libs.shared.appcatalog.data)
    implementation(libs.shared.appcatalog.ui)

    // Shared TelemetryDeck lifecycle: the startup signal buffer, automated-test-traffic detection,
    // and the start-before-flush ordering. This app's own signal vocabulary stays in analytics/.
    // `telemetrydeck-sdk` below is still declared directly because Analytics calls TelemetryDeck
    // itself, rather than relying on this module's `api` dependency to supply it.
    implementation(libs.shared.telemetry.core)

    // Shared haptics engine. `RootHaptics` holds this app's own patterns: the checking ramp, the
    // three outcome buzzes and the capability signal. Both modules are declared because
    // `RootHaptics` names `:core` types (`Haptics`, `HapticWaveform`) directly, not only through
    // `:compose`.
    implementation(libs.shared.haptics.core)
    implementation(libs.shared.haptics.compose)

    // Shared adaptive detail overlay: the ≥840dp container-transform card, its scene strategy, the
    // anchor state that bridges the overflow menu's Popup to it, and the leading nav icon.
    implementation(libs.shared.nav3.overlay)

    // The app-bar overflow menu and its items. The shared `AppBarDropdownMenuItem` wraps its own
    // `onClick` in `rememberHapticClick`, so call sites must not wrap it again or the tap ticks
    // twice.
    implementation(libs.shared.ui.menu)

    // The ColorScheme cross-fade over every Material color role.
    implementation(libs.shared.ui.theme)

    // The open-source credits list and the verbatim license texts, including
    // `AndroidSharedAttributions`, which credits every library this app ships. The collapsing
    // LargeTopAppBar and the `license_list` testTag stay in this app.
    implementation(libs.shared.ui.licences)

    // Shared Play Console screenshot matrices and the constrained-device stress specs. Declared on
    // `implementation`, not `screenshotTestImplementation`, because this app's constrained-device
    // preview *functions* live in `src/main` (util/ConstrainedDevicePreviews.kt) rather than in the
    // screenshotTest source set. Moving them would let this drop to screenshotTest only.
    implementation(libs.shared.previews.matrix)

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

    // Compose Preview Screenshot Testing: the @PreviewTest marker and the tooling that renders it
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    // Generated Baseline Profile, produced by the :baselineprofile module
    baselineProfile(project(":baselineprofile"))
}
