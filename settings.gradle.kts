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
    }
}
rootProject.name = "KuroStream"
include(":app")
// include(":benchmark")  // removed — zero source files, only generated BuildConfig
include(":cache")
include(":common")
include(":config")
include(":data")
include(":domain")
include(":extensions")
include(":marketplace")
include(":playback")
include(":plugin-sdk")
// include(":torrent") // DISABLED: 471 pre-existing compilation errors
include(":ui")
