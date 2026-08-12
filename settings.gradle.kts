pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Shared Android modules (Android-Shared repo), consumed as a composite build — no publishing, no
// submodule. Gradle substitutes the local project for the `com.iboalali.appcatalog:*` coordinates by
// matching group:name, so the version declared in app/build.gradle.kts is ignored.
//
// The default assumes the sibling layout under ~/Projects/private/android/. Override per machine with
// `androidShared.path` in ~/.gradle/gradle.properties if the checkout lives somewhere else.
val androidSharedPath =
    providers.gradleProperty("androidShared.path").getOrElse("../Android-Shared")

includeBuild(androidSharedPath)


rootProject.name = "Basic-Root-Checker"
include(":app")
include(":baselineprofile")
