// Root build script for KuroStream
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless) apply false
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

subprojects {
    // Detekt is applied via convention plugins where needed.
    // Auto-apply to Kotlin modules if the plugin class is available.
    afterEvaluate {
        try {
            pluginManager.apply("io.gitlab.arturbosch.detekt")
        } catch (_: Exception) {
            // ignore modules that cannot apply detekt
        }
    }
}
