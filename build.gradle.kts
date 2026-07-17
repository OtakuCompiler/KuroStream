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
        
        afterEvaluate {
            configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
                toolVersion = libs.versions.detekt.get()
                config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
                buildUponDefaultConfig = true
                allRules = false
                parallel = true
            }
            tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
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
