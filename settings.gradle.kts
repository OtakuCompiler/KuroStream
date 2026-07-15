// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

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
    // Resolve the Compose Compiler Gradle plugin from the buildscript classpath
    plugins {
        id("org.jetbrains.kotlin.composer") version "2.0.21"
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
if (findProject(":torrent") != null) include(":torrent") // re-enabled: user requested max peers for P2P streaming
include(":backup")
include(":lint-checks")
include(":webosApp")
include(":tizenApp")