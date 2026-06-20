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

// Generate and measure on the physically connected device.
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
