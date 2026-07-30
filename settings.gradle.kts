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
        maven("https://jitpack.io")
        maven("https://dl.frostwire.com/maven")
    }
}
rootProject.name = "KuroStream"
includeBuild("build-logic")
include(":app")
include(":benchmark")
include(":cache")
include(":common")
include(":config")
include(":core-common")
include(":core-platform")
include(":data")
include(":domain")
include(":extensions")
include(":launcher")
include(":marketplace")
include(":playback")
include(":plugin-sdk")
include(":tizenApp")
// include(":torrent")  // excluded — dl.frostwire.com/maven is dead (404), jlibtorrent cannot be resolved
include(":ui")
