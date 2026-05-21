// Kuro Stream module-level build file (intentionally minimal)
plugins {
    // Plugin versions defined in ../gradle/libs.versions.toml
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
