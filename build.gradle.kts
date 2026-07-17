plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false

    alias(libs.plugins.spotless)
    alias(libs.plugins.dependencycheck) apply false
}

// The detekt plugin is NOT applied globally to avoid spotless check failures on root project.
// Modules that want detekt should apply the plugin and configure it in their own build.gradle.kts.