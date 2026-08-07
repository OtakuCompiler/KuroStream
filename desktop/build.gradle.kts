import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                // Compose Multiplatform Desktop (native Kotlin UI on JVM)
                implementation(compose.desktop.currentOs)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)
                implementation(compose.ui)

                // Lifecycle for ViewModel-style state holders
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

                // Coroutines + serialization (shared with Android)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)

                // Networking (shared with Android)
                implementation(libs.retrofit)
                implementation(libs.retrofit.serialization.converter)
                implementation(libs.okhttp)
                implementation(libs.okhttp.brotli)

                // Image loading on JVM
                implementation("io.coil-kt:coil-compose:2.7.0")
                implementation("io.coil-kt:coil-network-okhttp:2.7.0")
                implementation("io.coil-kt:coil-swing:2.7.0")

                // Video playback on desktop via libVLC / JavaFX / Compose
                // (libVLC ships its native bindings for Linux/Windows/macOS)
                implementation("net.java.dev.jna:jna:5.14.0")

                // Logging
                implementation("com.github.tony19:logback-android:3.0.1")
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.test.junit)
            }
        }
    }
}

application {
    mainClass.set("com.kurostream.desktop.MainKt")

    // Native packaging via jpackage — produces .exe (Windows), .deb / .rpm /
    // .AppImage (Linux), .dmg / .pkg (macOS) installers.
    val jpackageJdkHome = System.getenv("JPACKAGE_JDK_HOME") ?: System.getProperty("java.home")
    jpackage {
        jpackageHome = jpackageJdkHome
        appName = "KuroStream"
        appVersion = "1.0.0"
        vendor = "KuroStream"
        description = "Privacy-focused streaming for anime, movies, and TV shows"
        copyright = "GPL-3.0-only"
        // Output format is auto-detected from host OS:
        //  - Windows host → .exe (MSI via --type msi)
        //  - Linux host   → .deb (--type deb), .rpm (--type rpm), or .AppImage (--type app-image)
        //  - macOS host   → .dmg / .pkg
        // Use `bash gradlew :desktop:packageDistribution` to build.
        // Cross-platform builds require running on each target OS.
        // For CI we run one job per OS (see .github/workflows/desktop-build.yml).
        outputDir = layout.buildDirectory.dir("distributions").get().asFile.absolutePath

        // Bundle a JRE so users don't need Java pre-installed.
        // Set to false to reduce installer size if you assume users have Java 17+.
        bundledJre = true
    }
}

// Disable unsupported source set Kotlin targets for the desktop-only module.
// (We don't need androidTarget here — that's handled by the :domain module.)
