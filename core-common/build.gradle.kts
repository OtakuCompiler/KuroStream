plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.spotless)
}

kotlin {
    androidTarget()
    js {
        browser()
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.javax.inject)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.javax.inject)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

android {
    namespace = "com.kurostream.core.common"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    kotlin {
        target("*.kt", "*.kts")
        ktlint()
    }
}
