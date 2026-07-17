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

val excludedDetektModules = setOf("tizenApp", "webosApp", "benchmark", "cache", "common", "data", "extensions")

subprojects {
    // Apply detekt plugin conditionally
    if (name !in excludedDetektModules) {
        apply(plugin = "io.gitlab.arturbosch.detekt")
    }
}

// Configure detekt after all projects are evaluated, only for projects with the plugin
gradle.projectsEvaluated {
    subprojects { project ->
        if (project.name !in excludedDetektModules) {
            project.plugins.withType<io.gitlab.arturbosch.detekt.extensions.DetektExtension>().configureEach { ext ->
                ext.toolVersion = libs.versions.detekt.get()
                ext.config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
                ext.buildUponDefaultConfig = true
                ext.allRules = false
                ext.parallel = true
            }
            project.tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
                exclude("**/TvRepositoryAdapters.kt")
                exclude("**/BenchmarkRunner.kt")
                exclude("**/BackupSettingsScreen.kt")
                reports {
                    html.required.set(true)
                    xml.required.set(true)
                    txt.required.set(false)
                }
            }
        }
    }
}
