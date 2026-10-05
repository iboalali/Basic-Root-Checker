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

        // The Android-Shared releases: a Maven repository kept in git, checked out beside this one.
        // See maven-repo/README.md. A `git pull` there is what makes a newly published version
        // visible to this build, and a clone of it is required to build this app at all.
        //
        // The group filter is not decoration. Without it Gradle would look up every dependency in a
        // local folder first, and a stale checkout could shadow a real artifact from Maven Central.
        maven {
            name = "iboalaliShared"
            url =
                uri(
                    rootDir.resolve(
                        providers
                            .gradleProperty("sharedMavenRepo.path")
                            .getOrElse("../maven-repo")
                    )
                )
            content { includeGroupByRegex("com\\.iboalali\\..*") }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Shared Android modules (Android-Shared repo) resolve as published AARs from the `maven-repo`
// checkout declared above. Which version this app sits on is the `shared` entry in
// gradle/libs.versions.toml, and it moves when this app is ready for it and not before. That
// staggering is the reason the modules are published rather than included.
//
// `-PandroidShared.composite=true` replaces that with a composite build of the local working tree,
// for developing a change across this app and the library at once. Under substitution Gradle matches
// `group:name` and throws the version away, so the catalog pin means nothing in that mode and every
// edit in the shared checkout lands here immediately. Publish a version before committing anything
// here that depends on it.
//
// The default path assumes the sibling layout under ~/Projects/private/android/. Override per machine
// with `androidShared.path` in ~/.gradle/gradle.properties (absolute paths are fine).
if (providers.gradleProperty("androidShared.composite").orNull.toBoolean()) {
    includeBuild(providers.gradleProperty("androidShared.path").getOrElse("../Android-Shared"))
}


rootProject.name = "Basic-Root-Checker"
include(":app")
include(":baselineprofile")
