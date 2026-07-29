package com.kurostream.players.core

import com.kurostream.common.memory.MemoryGovernor
import com.kurostream.players.frame.Resolution

class PlaybackReliabilityManager(
    private val initialResolution: Resolution = Resolution.UHD_4K
) {
    private var currentResolution: Resolution = initialResolution

    fun updateResolution() {
        if (MemoryGovernor.memoryPressure > 0.7f) {
            currentResolution = when (currentResolution) {
                Resolution.UHD_4K -> Resolution.QHD_1440P
                Resolution.QHD_1440P -> Resolution.FHD_1080P
                else -> Resolution.HD_720P
            }
        } else if (MemoryGovernor.memoryPressure < 0.4f) {
            currentResolution = when (currentResolution) {
                Resolution.HD_720P -> Resolution.FHD_1080P
                Resolution.FHD_1080P -> Resolution.QHD_1440P
                else -> Resolution.UHD_4K
            }
        }
    }
    
    fun getCurrentResolution(): Resolution = currentResolution
    
    fun resetToInitial() {
        currentResolution = initialResolution
    }
}