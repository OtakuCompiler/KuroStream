// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.playback.kurovision

enum class ColorProfile(
    val displayName: String,
    val description: String,
    val colorTemperatureK: Int,
    val saturation: Float,
    val contrast: Float,
    val fakeHdrIntensity: Float,
    val oledBlackIntensity: Float,
    val sharpness: Float,
    val gamma: Float,
) {
    NATURAL(
        displayName = "Natural",
        description = "Reference-accurate colors, no processing.",
        colorTemperatureK = 6500,
        saturation = 1.0f,
        contrast = 1.0f,
        fakeHdrIntensity = 0.0f,
        oledBlackIntensity = 0.0f,
        sharpness = 1.0f,
        gamma = 1.0f,
    ),
    VIVID(
        displayName = "Vivid",
        description = "Punchy colors and contrast for HDR-like pop.",
        colorTemperatureK = 6500,
        saturation = 1.3f,
        contrast = 1.15f,
        fakeHdrIntensity = 0.0f,
        oledBlackIntensity = 0.0f,
        sharpness = 1.2f,
        gamma = 1.0f,
    ),
    WARM(
        displayName = "Warm",
        description = "Warm tungsten tone, gentle on eyes for night viewing.",
        colorTemperatureK = 5800,
        saturation = 1.05f,
        contrast = 1.0f,
        fakeHdrIntensity = 0.0f,
        oledBlackIntensity = 0.0f,
        sharpness = 1.0f,
        gamma = 0.95f,
    ),
    COOL(
        displayName = "Cool",
        description = "Cool daylight tone, crisp clinical look.",
        colorTemperatureK = 7500,
        saturation = 1.0f,
        contrast = 1.05f,
        fakeHdrIntensity = 0.0f,
        oledBlackIntensity = 0.0f,
        sharpness = 1.0f,
        gamma = 1.0f,
    ),
    CINEMA(
        displayName = "Cinema",
        description = "Filmmaker Intent — warm shadows, lifted blacks.",
        colorTemperatureK = 6200,
        saturation = 1.1f,
        contrast = 1.1f,
        fakeHdrIntensity = 0.0f,
        oledBlackIntensity = 0.0f,
        sharpness = 1.0f,
        gamma = 0.97f,
    ),
    ANIME(
        displayName = "Anime",
        description = "Vibrant cel-shaded look with edge sharpening.",
        colorTemperatureK = 6500,
        saturation = 1.25f,
        contrast = 1.1f,
        fakeHdrIntensity = 0.4f,
        oledBlackIntensity = 0.0f,
        sharpness = 1.3f,
        gamma = 1.0f,
    ),
    HDR_VISION(
        displayName = "HDR Vision",
        description = "Fake HDR tone mapping with shadow recovery.",
        colorTemperatureK = 6500,
        saturation = 1.15f,
        contrast = 1.2f,
        fakeHdrIntensity = 0.9f,
        oledBlackIntensity = 0.0f,
        sharpness = 1.1f,
        gamma = 1.05f,
    ),
    ;

    companion object {
        private val DEFAULT = NATURAL

        fun fromName(name: String?): ColorProfile =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
