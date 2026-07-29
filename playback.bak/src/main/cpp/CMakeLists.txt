cmake_minimum_required(VERSION 3.22.1)
project(kurostream_native)

set(CMAKE_CXX_STANDARD 17)

# --- Phase 71: libmpv hardware-accelerated renderer ---
# Requires a prebuilt libmpv for Android on the NDK search path (see
# MERGE_REPORT_2.md "Native Libraries Setup" — same manual prerequisite noted for
# the mpv-android AAR in the phases 21-30 merge).
find_package(mpv REQUIRED)

add_library(mpvrenderer SHARED
    MpvRendererJNI.cpp
)

target_link_libraries(mpvrenderer
    mpv
    EGL
    GLESv3
    android
    log
    pthread
)

target_compile_options(mpvrenderer PRIVATE
    -O3
    -ffast-math
    -fvisibility=hidden
    -fno-exceptions
    -fno-rtti
)

# --- Phase 80: audio DSP (10-band EQ, loudness normalization) ---
# Requires Google's Oboe library. Simplest path: add Oboe as a dependency via the
# prefab/AAR mechanism (implementation("com.google.oboe:oboe:1.9.0") in playback/build.gradle.kts
# with `android.buildFeatures.prefab = true`) rather than vendoring the source tree.
find_package(oboe REQUIRED CONFIG)

add_library(kurostream_dsp SHARED
    native_dsp.cpp
)

target_link_libraries(kurostream_dsp
    oboe::oboe
    android
    log
)

target_compile_options(kurostream_dsp PRIVATE
    -O3
    -ffast-math
    -fvisibility=hidden
)
