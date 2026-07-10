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
    id("com.diffplug.spotless") version "6.25.0"
}

spotless {
    kotlin {
        target("*.kt", "*.kts") {
            ktlint()
            // Optional: Add license header
            // licenseHeader("""KuroStream - Anime streaming app for Android TV
            // Copyright (c) 2024""".trimIndent())
        }
    }

    // Format Gradle files
    target("*.gradle", "*.gradle.kts", "settings.gradle.kts") {
        prettier()
    }

    // Format XML files (layout, manifest, etc.)
    target("*.xml") {
        prettier()
    }

    // Format JSON files
    target("*.json") {
        prettier()
    }

    // Format Markdown files
    target("*.md") {
        prettier()
    }
}

tasks.named("spotlessApply") {
    group = "formatting"
    description = "Apply code formatting"
}

tasks.named("spotlessCheck") {
    group = "verification"
    description = "Check code formatting"
}