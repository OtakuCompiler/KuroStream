// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

// Reconciliation notes (see MERGE_REPORT.md):
// - Converted from Groovy (build.gradle) to Kotlin DSL for consistency with the rest of the project.
// - namespace changed from "com.mediaplayer.playback" -> "com.kurostream.playback" to match
//   the project's package convention (source files were renamed to match).
// - externalNativeBuild/cmake + NDK abiFilters removed: the zip did not include
//   src/main/cpp/CMakeLists.txt, so this would fail configuration as-is. Re-enable once the
//   MPV native build step (see README "Native Libraries Setup") has been completed.
// Reconciliation notes (see MERGE_REPORT.md and MERGE_REPORT_2.md):
// - Converted from Groovy (build.gradle) to Kotlin DSL for consistency with the rest of the project.
// - namespace changed from "com.mediaplayer.playback" -> "com.kurostream.playback" to match
//   the project's package convention (source files were renamed to match).
// - Phase 71-80 merge added real native sources (MpvRendererJNI.cpp, native_dsp.cpp) plus a
//   combined CMakeLists.txt under src/main/cpp/ — externalNativeBuild is now re-enabled.
//   Building still requires libmpv-for-Android and Oboe to be resolvable (see
//   src/main/cpp/CMakeLists.txt comments and MERGE_REPORT_2.md "Native Libraries Setup").
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kurostream.players"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // externalNativeBuild {
        //     cmake {
        //         cppFlags += ""
        //         abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        //         arguments += listOf("-DANDROID_STL=c++_shared")
        //     }
        // }
    }

    // externalNativeBuild temporarily disabled: requires prebuilt libmpv-for-Android.
    // See CMakeLists.txt comments and MERGE_REPORT_2.md "Native Libraries Setup".
    // externalNativeBuild {
    //     cmake {
    //         path = file("src/main/cpp/CMakeLists.txt")
    //         version = "3.22.1"
    //     }
    // }

    buildFeatures {
        compose = true
        // prefab re-enabled once externalNativeBuild is uncommented (needed for Oboe AAR)
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            consumerProguardFiles("consumer-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.pickFirsts.add("lib/arm64-v8a/libc++_shared.so")
        resources.pickFirsts.add("lib/armeabi-v7a/libc++_shared.so")
        resources.pickFirsts.add("lib/x86_64/libc++_shared.so")
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.smoothstreaming)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.common)
    implementation(libs.media3.decoder)

    implementation(libs.libvlc.all)
    // MPV requires the manually-built AAR described in the README's "Native Libraries Setup"
    // step; uncomment once playback/libs/app-release.aar has been produced and added.
    // implementation(libs.mpv.android)

    implementation(libs.okhttp)
    implementation(libs.timber)
    implementation(libs.gson)

    // --- Phase 71-80 additions (advanced rendering/AI/DRM/sandbox/watch-party/audio) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    implementation(libs.webrtc.android)
    implementation(libs.nanohttpd)
    implementation(libs.nanohttpd.websocket)
    implementation(libs.pytorch.android.lite)
    implementation(libs.oboe)

    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
