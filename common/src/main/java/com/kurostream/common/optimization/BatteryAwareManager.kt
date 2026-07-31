package com.kurostream.common.optimization

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryAwareManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val _batteryState = MutableStateFlow(readBatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    data class BatteryState(
        val isCharging: Boolean = false,
        val levelPercent: Int = -1,
        val isPowerSaveMode: Boolean = false
    )

    private fun readBatteryState(): BatteryState {
        val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

        val powerSave = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            (appContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager)?.isPowerSaveMode == true
        } else false

        return BatteryState(
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            levelPercent = pct,
            isPowerSaveMode = powerSave
        )
    }

    fun shouldReduceImageQuality(): Boolean {
        val state = _batteryState.value
        return state.isPowerSaveMode || state.levelPercent in 0..19
    }
}
