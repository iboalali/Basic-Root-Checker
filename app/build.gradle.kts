plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.iboalali.basicrootchecker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iboalali.basicrootchecker"
        minSdk = 21
        targetSdk = 36
        versionCode = 30
        versionName = "v1.14vc$versionCode"
        vectorDrawables.useSupportLibrary = true
        @Suppress("UnstableApiUsage")
        androidResources.localeFilters += listOf("en", "ar", "de")
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

    buildFeatures {
        dataBinding = true
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
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)

    implementation(libs.google.material)

    implementation(libs.boehrsi.devicemarketingnames)
    implementation(libs.topjohnwu.libsu.core)
}
