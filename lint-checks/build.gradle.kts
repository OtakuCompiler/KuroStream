plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.kurostream.lint.checks"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    compileOnly(libs.androidx.core.ktx)
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
    
    // Lint API for custom rules (matches AGP 8.7.3)
    compileOnly("com.android.tools.lint:lint-api:31.7.3")
    compileOnly("com.android.tools.lint:lint-checks:31.7.3")
    
    // For UAST analysis
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlin.get()}")
    
    // Detekt API for custom rules
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:${libs.versions.detekt.get()}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
