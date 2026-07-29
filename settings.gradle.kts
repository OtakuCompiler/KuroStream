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
        // Required for the real com.frostwire:jlibtorrent* artifacts used by
        // the :torrent module — this project doesn't publish to Maven
        // Central. Not verified reachable from this environment (no network
        // access here); confirmed via web search this pass to be the
        // current, actively-maintained repo for the library.
        maven("https://dl.frostwire.com/maven")
    }
}
rootProject.name = "KuroStream"
includeBuild("build-logic")
include(":app")
include(":backup")
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
// ":torrent" intentionally excluded — 4,376 lines / 471 known compile
// errors (per code comment left in TvNavHost.kt by a prior pass), and
// confirmed via grep that no other module, including :app, declares
// project(":torrent") as a dependency. Excluding it here stops a full
// `./gradlew build` from attempting to compile it. The app's Torrents
// screen is an honest empty placeholder unrelated to this module — see
// ISSUES_LEDGER.md row 4. Re-include only once the module's own errors
// are actually fixed.
include(":ui")
