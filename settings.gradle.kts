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
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}
rootProject.name = "KuroStream"
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
// include(":torrent") // disabled: frostwire jlibtorrent 2.0.x is only on GitHub Packages (auth req'd), not Maven Central
include(":backup")
include(":lint-checks")
include(":webosApp")
include(":tizenApp")