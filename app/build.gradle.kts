plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baseline.profile)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.kurostream.app"
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = "28.0.13004108"

    defaultConfig {
        applicationId = "com.kurostream.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
        manifestPlaceholders["appAuthRedirectScheme"] = "kurostream"
        resourceConfigurations += setOf("en")

        // KuroCloud API configuration (publishable - safe in APK)
        buildConfigField("String", "KURO_API_BASE", "\"https://kuro-stream-tv.lovable.app\"")
        buildConfigField("String", "KURO_AUTH_URL", "\"https://kklyohtsedcdgmnmameh.supabase.co\"")
        buildConfigField("String", "KURO_ANON_KEY", "\"sb_publishable_x_ZB45-mADfu4479vmZdaw_SGpIE6Kx\"")
    }


    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("UPLOAD_KEYSTORE_PATH") ?: "upload-keystore.jks")
            storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("UPLOAD_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("UPLOAD_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            isCrunchPngs = true
            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = true
                isOptimizeCode = true
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            ndk {
                abiFilters.clear()
                abiFilters.add("arm64-v8a")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/*.md"
            excludes += "**/ISSUES_LEDGER.md"
            excludes += "**/PRODUCTION_IMPLEMENTATION_REPORT.md"
            excludes += "**/DebugProbesKt.bin"
        }
        jniLibs {
            useLegacyPackaging = false
            pickFirsts += listOf("lib/arm64-v8a/libc++_shared.so")
        }
    }

    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = false
        disable += "ObsoleteLintCustomCheck"
        disable += "LockedOrientationActivity"
    }

    baselineProfile {
        baselineProfileOutputDir = "src/main/baseline-prof"
        saveInSrc = true
        mergeIntoMain = true
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":extensions"))
    implementation(project(":plugin-sdk"))
    implementation(project(":cache"))
    implementation(project(":ui"))
    implementation(project(":marketplace"))
    implementation(project(":torrent"))
    implementation(project(":playback"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.tv.material3)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tvprovider)
    implementation(libs.play.review)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coil)
    implementation(libs.coil.video)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.timber)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.android)

    // Play Integrity + App Check
    implementation("com.google.android.play:integrity:1.4.0")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-firestore") {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    implementation("com.google.firebase:firebase-messaging")

    // Resolve protobuf duplicate-class conflict: redirect any protobuf-javalite
    // request to the full protobuf-java runtime (already present via protobuf-kotlin
    // in the data module). Also exclude protolite-well-known-types which bundles
    // duplicate protobuf classes. This eliminates the duplicate AbstractMessageLite etc.
    configurations.all {
        exclude(group = "com.google.firebase", module = "protolite-well-known-types")
        resolutionStrategy.dependencySubstitution {
            substitute(module("com.google.protobuf:protobuf-javalite"))
                .using(module("com.google.protobuf:protobuf-java:${libs.versions.protobuf.get()}"))
        }
    }

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    // Cast
    implementation("com.google.android.gms:play-services-cast-framework:21.5.0")
    implementation("com.google.android.gms:play-services-cast-tv:21.0.0")

    // WebRTC - temporarily disabled, requires custom repo
    // implementation("org.webrtc:google-webrtc:1.0.32006")

    coreLibraryDesugaring(libs.android.desugarJdkLibs)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.coroutines)
    testImplementation(libs.test.turbine)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation(libs.test.androidx.junit)

    androidTestImplementation(libs.test.androidx.junit)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
