// Root build script for KuroStream
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless) apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}

// Relocate ALL build outputs to internal storage (fast ext4) instead of FUSE
// /sdcard - this is the single biggest build speed win on this device.
// Only apply when KURO_BUILD_DIR is explicitly set or when not running on CI.
val isCI = System.getenv("CI") == "true"
val kurostreamBuildDir = System.getenv("KURO_BUILD_DIR")?.takeIf { it.isNotBlank() }
    ?: if (isCI) null else "/root/.kurostream-build"

if (kurostreamBuildDir != null) {
    layout.buildDirectory.set(file("$kurostreamBuildDir/root"))
    subprojects {
        layout.buildDirectory.set(file("$kurostreamBuildDir/${name}"))
    }
}

tasks.register("detektAll") {
    group = "verification"
    description = "Run detekt static analysis on all modules"
    subprojects.forEach { subproject ->
        val detektTask = subproject.tasks.findByName("detekt")
        if (detektTask != null) {
            dependsOn(detektTask)
        }
    }
}

tasks.register("detektFormat") {
    group = "verification"
    description = "Auto-format Kotlin files with detekt"
    subprojects.forEach { subproject ->
        val detektFormatTask = subproject.tasks.findByName("detektFormat")
        if (detektFormatTask != null) {
            dependsOn(detektFormatTask)
        }
    }
}

// Detekt is NOT auto-applied to all subprojects: it added heavy per-module
// configuration overhead on every build. Apply it explicitly in a module
// when needed, or run the detektAll/detektFormat tasks above.
