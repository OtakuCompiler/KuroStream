/*
 * KuroStream Desktop Module
 *
 * Compose Multiplatform desktop application that shares the domain + data
 * layers with the Android app. Produces native installers:
 *   - Windows: msi / exe via jpackage + launch4j
 *   - macOS:   .dmg
 *   - Linux:   .deb / .AppImage
 *
 * Build commands:
 *   bash gradlew :desktop:packageExe           # Windows installer
 *   bash gradlew :desktop:packageDmg           # macOS
 *   bash gradlew :desktop:packageDeb           # Linux
 *
 * Output: desktop/build/compose/binaries/main/{exe,dmg,deb}/
 */

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.6.11"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":common"))
    implementation(project(":playback"))
    implementation(project(":extensions"))
    implementation(project(":ui"))

    // Compose Multiplatform
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Logging
    implementation(libs.timber)

    // Crypto / DRM
    implementation(libs.sqlite.jdbc)

    // Torrent (libtorrent4j works on JVM desktop)
    implementation(libs.jlibtorrent)
}

application {
    mainClass.set("com.kurostream.desktop.MainKt")
    nativeDistributions {
        appName = "KuroStream"
        packageName = "kurostream"
        version = "1.0.0"
        vendor = "KuroStream"
        licenseFile.set(project.file("../LICENSE"))

        modules("jdk.unsupported", "java.naming", "java.management")

        // Windows .exe / .msi
        windows {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            iconFile.set(project.file("src/main/resources/icons/kurostream.ico"))
            menu = true
            shortcut = true
            perUserInstall = false
            // Enable UPX compression for smaller installer
            upgradeUuid = "f4e9c8a1-5b7d-4e3a-9c2e-7b8a1d3f5e6c"
        }

        // macOS .dmg
        macOS {
            targetFormats(TargetFormat.Dmg)
            iconFile.set(project.file("src/main/resources/icons/kurostream.icns"))
            bundleID = "app.kurostream.desktop"
            jpackageArgs += listOf("--java-options", "-Xmx2g")
        }

        // Linux .deb / .AppImage
        linux {
            targetFormats(TargetFormat.Deb, TargetFormat.AppImage)
            iconFile.set(project.file("src/main/resources/icons/kurostream.png"))
            debMaintainer = "KuroStream <dev@kurostream.app>"
            debDescription = "Privacy-focused streaming for anime, movies, and TV shows."
        }
    }
}

// Maven Central repo (not in default set for desktop plugins)
repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

compose.desktop {
    application {
        mainClass = "com.kurostream.desktop.MainKt"
    }
}
