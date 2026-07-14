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

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.libs
import org.gradle.kotlin.dsl.provider
import org.gradle.kotlin.dsl.providers

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")
            // Apply Compose Compiler Gradle plugin for Kotlin 2.0+
            pluginManager.apply("org.jetbrains.kotlin.composer")

            extensions.configure<AppExtension> {
                compileSdk = providers.provider { libs.versions.compileSdk.get().toInt() }

                defaultConfig {
                    minSdk = providers.provider { libs.versions.minSdk.get().toInt() }
                    targetSdk = providers.provider { libs.versions.compileSdk.get().toInt() }
                    versionCode = 100
                    versionName = "1.0.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    vectorDrawables.useSupportLibrary = true

                    ndk {
                        abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
                    }

                    bundle {
                        language {
                            enableSplit = true
                        }
                        density {
                            enableSplit = true
                        }
                        abi {
                            enableSplit = true
                        }
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
                    }
                    debug {
                        isMinifyEnabled = false
                        isDebuggable = true
                        applicationIdSuffix = ".debug"
                        versionNameSuffix = "-debug"
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                kotlinOptions {
                    jvmTarget = "17"
                }

                buildFeatures {
                    compose = true
                    viewBinding = false
                }

                composeOptions {
                    kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
                }

                packaging {
                    resources.excludes += "/META-INF/*.kotlin_module"
                    jniLibs {
                        useLegacyPackaging = true
                    }
                }
            }
        }
    }
}