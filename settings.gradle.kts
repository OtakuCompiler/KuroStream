pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://dl.frostwire.com/maven")
        maven("https://jitpack.io")
    }
}
rootProject.name = "KuroStream"
includeBuild("build-logic")
include(":app")
// include(":benchmark")  // removed — zero source files, only generated BuildConfig
include(":cache")
include(":common")
include(":config")
include(":core-common")
include(":core-platform")
include(":data")
include(":domain")
include(":extensions")
// include(":launcher")  // removed — zero source files, only generated BuildConfig
include(":marketplace")
include(":playback")
include(":plugin-sdk")
// include(":tizenApp")  // removed — directory missing from workspace; no remaining source/runtime references found
include(":torrent")  // re-enabled — dl.frostwire.com/maven is live (verified 2026-07-31); jlibtorrent 2.0.12.9 resolves with android natives
include(":ui")
