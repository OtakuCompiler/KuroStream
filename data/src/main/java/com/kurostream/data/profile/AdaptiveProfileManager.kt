// This file is part of KuroStream.
//
// AdaptiveProfileManager — selects visual profile based on device class.
// TV Profile: disables expensive blur, uses static gradients, reduces shadows
// Mobile Profile: enables stronger glass blur, dynamic backgrounds
// Desktop Profile: maximum glass effects, HDR-style gradients
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.profile

import com.kurostream.playback.kurovision.DeviceClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveProfileManager @Inject constructor(
    private val deviceProfile: com.kurostream.playback.kurovision.KuroVisionDeviceProfile? = null,
) {

    val visualProfile: VisualProfile
        get() = when (deviceProfile?.deviceClass) {
            DeviceClass.LOW_POWER_TV, DeviceClass.MID_TV -> VisualProfile.TV
            DeviceClass.MOBILE_LOW, DeviceClass.MOBILE_HIGH -> VisualProfile.MOBILE
            DeviceClass.DESKTOP_LOW, DeviceClass.DESKTOP_HIGH -> VisualProfile.DESKTOP
            else -> VisualProfile.TV
        }

    fun isBlurEnabled(): Boolean = when (visualProfile) {
        VisualProfile.TV -> false
        VisualProfile.MOBILE -> true
        VisualProfile.DESKTOP -> true
    }

    fun maxGlowAlpha(): Float = when (visualProfile) {
        VisualProfile.TV -> 0.2f
        VisualProfile.MOBILE -> 0.4f
        VisualProfile.DESKTOP -> 0.5f
    }

    fun maxShadowElevation(): Float = when (visualProfile) {
        VisualProfile.TV -> 4f
        VisualProfile.MOBILE -> 8f
        VisualProfile.DESKTOP -> 16f
    }
}

enum class VisualProfile { TV, MOBILE, DESKTOP }
