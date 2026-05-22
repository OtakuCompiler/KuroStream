import com.android.build.api.variant.FilterConfiguration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// Defined at top level so both android{} and androidComponents{} can see it.
val abiVersionCodes = mapOf(
    "armeabi-v7a" to 1,
    "arm64-v8a"   to 2,
    "x86_64"      to 3
)

android {
    namespace = "com.kurostream"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kurostream"
        minSdk = 22
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── ABI splits ────────────────────────────────────────────────────────────
    // Produces 4 APKs: armeabi-v7a (ARM 32-bit), arm64-v8a (ARM 64-bit),
    // x86_64, and a universal fallback APK.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    // ── Release signing config ────────────────────────────────────────────────
    // Reads four environment variables set by the CI workflow (via GitHub
    // Secrets).  When any variable is absent (local dev, forked PRs) the
    // signingConfig is left unset and Gradle produces an unsigned APK instead
    // of failing the build.
    //
    // Required GitHub Secrets:
    //   KEYSTORE_BASE64   — base64-encoded .jks file
    //   STORE_PASSWORD    — keystore password
    //   KEY_ALIAS         — key alias inside the keystore
    //   KEY_PASSWORD      — key password
    signingConfigs {
        create("release") {
            val keystoreEnv = System.getenv("KEYSTORE_FILE")
            if (!keystoreEnv.isNullOrBlank()) {
                storeFile     = file(keystoreEnv)
                storePassword = System.getenv("STORE_PASSWORD") ?: ""
                keyAlias      = System.getenv("KEY_ALIAS") ?: ""
                keyPassword   = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
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
            // Only apply the signing config when the keystore file actually
            // exists — falls back to unsigned when secrets are unavailable.
            signingConfig = signingConfigs.getByName("release").takeIf {
                it.storeFile?.exists() == true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ── Per-ABI versionCode via the stable AGP 8 Variant API ─────────────────────
// versionCodeOverride was removed in AGP 8; use output.versionCode.set() instead.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters
                .firstOrNull { it.filterType == FilterConfiguration.FilterType.ABI }
                ?.identifier
                ?: "universal"
            val abiCode = abiVersionCodes[abi] ?: 0
            // Multiply base versionCode by 10, add the ABI offset.
            output.versionCode.set(
                output.versionCode.map { base -> (base ?: 1) * 10 + abiCode }
            )
            output.outputFileName.set(
                "kuro-stream-${abi}-v${variant.name}.apk"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)

    implementation(libs.coroutines.android)
    implementation(libs.coroutines.core)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.timber)
    implementation(libs.jsoup)

    debugImplementation(libs.androidx.ui.tooling)
}
