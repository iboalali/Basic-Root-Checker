plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.iboalali.basicrootchecker.baselineprofile"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // Macrobenchmark / Baseline Profile capture needs API 28+; the test device is API 36.
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The app under test.
    targetProjectPath = ":app"

    testOptions {
        managedDevices {
            localDevices {
                // Guarantees large-screen coverage without depending on what happens to be plugged
                // in. Pixel C is 900dp wide in portrait and 1280dp in landscape, so it clears the
                // 840dp breakpoint either way — most tablet profiles (Pixel Tablet, Medium Tablet,
                // Nexus 10) are only 800dp in portrait, just under it, and would silently profile
                // the phone path instead.
                create("tabletApi35") {
                    device = "Pixel C"
                    apiLevel = 35
                    // Profile capture needs a rootable image; google_apis/playstore are not.
                    systemImageSource = "aosp"
                }
            }
        }
    }

    // Mirror the app's flavor dimension so generate/measure can target gplay and foss. Both
    // flavors ship a profile (Play Store + F-Droid).
    flavorDimensions += "distribution"
    productFlavors {
        create("gplay") {
            dimension = "distribution"
        }
        create("foss") {
            dimension = "distribution"
        }
    }
}

// Generate and measure on whatever is physically connected, plus the tablet emulator above. The
// plugin unions the rules from every device into the same baseline-prof.txt, so the connected phone
// still contributes the single-pane push flow while the tablet contributes the overlay path.
baselineProfile {
    useConnectedDevices = true
    managedDevices += "tabletApi35"
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
