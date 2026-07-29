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

plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
  // Hardcoded to match gradle/libs.versions.toml exactly (verified against that
  // file's pinned versions). This build-logic included build was failing to
  // resolve libs.* references; hardcoding removes the dependency on that
  // catalog wiring working inside the included build. If build-logic's own
  // catalog access is fixed later, this can be reverted to libs.* references.
  implementation("androidx.core:core-ktx:1.15.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
  implementation("junit:junit:4.13.2")
  implementation("androidx.test.ext:junit:1.2.1")
  implementation("androidx.test.espresso:espresso-core:3.6.1")
  implementation("com.google.dagger:hilt-android:2.52")
  implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.7")
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "kurostream.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "kurostream.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("hilt") {
            id = "kurostream.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("kotlinMultiplatform") {
            id = "kurostream.kotlin.multiplatform"
            implementationClass = "KotlinMultiplatformConventionPlugin"
        }
        register("detekt") {
            id = "kurostream.detekt"
            implementationClass = "DetektConventionPlugin"
        }
        register("spotless") {
            id = "kurostream.spotless"
            implementationClass = "SpotlessConventionPlugin"
        }
    }
}