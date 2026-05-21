plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.kurostream.tv"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.kurostream.tv"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        // In CI release builds the workflow passes -PversionCode and -PversionName.
        // Fall back to defaults when building locally.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }

        // Memory optimization for 1GB RAM devices
        resourceConfigurations.addAll(listOf("en", "ja"))
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ABI splits — produces one APK per target + one universal fallback.
    // Requested targets: armeabi-v7a (32-bit ARM), arm64-v8a (64-bit ARM), x86_64 (emulator / Intel).
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true   // also emit a fat APK covering all three ABIs
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// ─── Per-ABI version codes ────────────────────────────────────────────────────
//
// Play Store / Fire TV Appstore require each split APK to have a unique,
// monotonically increasing version code so the store can pick the right APK.
//
// Offset map:
//   universal    → base * 1000 + 0   (e.g. versionCode 1 → 1000)
//   armeabi-v7a  → base * 1000 + 1   (32-bit ARM Fire TV Stick 1st gen)
//   arm64-v8a    → base * 1000 + 2   (64-bit ARM Fire TV Stick 4K / Cube)
//   x86_64       → base * 1000 + 8   (Intel emulator / test lab)
//
val abiVersionCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86_64" to 8)
val baseVersionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abiFilter = output.filters
                .find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }
                ?.identifier
            output.versionCode.set(
                baseVersionCode * 1000 + (abiVersionCodes[abiFilter] ?: 0)
            )
        }
    }
}

// ─── Helper tasks ─────────────────────────────────────────────────────────────
//
// Build commands:
//
//   All splits + universal (release):
//     ./gradlew assembleRelease
//
//   All splits + universal (debug):
//     ./gradlew assembleDebug
//
//   Individual ABI release APKs (no direct per-ABI task; filter after build):
//     arm64-v8a:    find app/build/outputs/apk/release -name "*arm64*"
//     armeabi-v7a:  find app/build/outputs/apk/release -name "*armeabi*"
//     x86_64:       find app/build/outputs/apk/release -name "*x86_64*"
//     universal:    find app/build/outputs/apk/release -name "*universal*"
//
// The task below runs assembleRelease and then prints the path of every
// generated APK so CI logs show all produced artefacts at a glance.

tasks.register("assembleSplitApks") {
    group = "build"
    description = "Build release APKs for armeabi-v7a, arm64-v8a, x86_64, and universal."
    dependsOn("assembleRelease")
    doLast {
        val outputDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        if (!outputDir.exists()) {
            logger.lifecycle("No APKs found — assembleRelease may have failed.")
            return@doLast
        }
        val apks = outputDir.listFiles { f -> f.extension == "apk" }
            ?.sortedBy { it.name } ?: emptyList()
        logger.lifecycle("\n── Kuro Stream split APKs ──")
        apks.forEach { apk ->
            val abi = when {
                "arm64-v8a"   in apk.name -> "arm64-v8a    (Fire TV Stick 4K / Cube)"
                "armeabi-v7a" in apk.name -> "armeabi-v7a  (Fire TV Stick 1st gen)"
                "x86_64"      in apk.name -> "x86_64       (emulator / Intel)"
                "universal"   in apk.name -> "universal    (all ABIs)"
                else                      -> "unknown"
            }
            logger.lifecycle("  [$abi]  ${apk.name}  (${apk.length() / 1024} KB)")
        }
        logger.lifecycle("Total: ${apks.size} APK(s) in ${outputDir.path}\n")
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // TV Compose
    implementation(libs.bundles.tv.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Media3 (ExoPlayer)
    implementation(libs.bundles.media3)

    // Networking
    implementation(libs.bundles.networking)

    // Local Storage
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // Image Loading
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Security
    implementation(libs.security.crypto)

    // Logging
    implementation(libs.timber)

    // VLC (fallback player)
    implementation(libs.vlc.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
