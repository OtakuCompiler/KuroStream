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
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.mozilla.org/maven2") }
    }
}
rootProject.name = "KuroStream"
include(":app")
// include(":baseline-profile")
// include(":benchmark")
include(":cache")
include(":common")
include(":config")
include(":data")
include(":domain")
include(":extensions")
include(":marketplace")
include(":playback")
include(":plugin-sdk")
include(":torrent")
include(":ui")
include(":desktop")
