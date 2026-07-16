pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin/")
    }
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "KuroStream"
includeBuild("build-logic")
include(":app")
include(":domain")
include(":data")
include(":playback")
include(":plugin-sdk")
include(":cache")
include(":common")
include(":extensions")
include(":launcher")
include(":benchmark")
include(":core-common")
include(":core-platform")
include(":ui")
include(":backup")
include(":lint-checks")
include(":webosApp")
include(":tizenApp")
