// This file is part of KuroStream.
//
// SourceHealthManager — tracks source reliability.
// Records failures, latency, and buffer health per source.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.resolver

import com.kurostream.domain.resolver.SourceHealth
import com.kurostream.domain.resolver.StreamSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceHealthManager @Inject constructor() {

    private val records = ConcurrentHashMap<String, HealthRecord>()

    private val _stats = MutableStateFlow(emptyMap<String, HealthRecord>())
    val stats: StateFlow<Map<String, HealthRecord>> = _stats.asStateFlow()

    fun recordSuccess(sourceId: String, latencyMs: Long) {
        val rec = records.getOrPut(sourceId) { HealthRecord() }
        rec.successCount++
        rec.lastLatencyMs = latencyMs
        rec.health = when {
            rec.successCount >= 10 && rec.failCount == 0 -> SourceHealth.EXCELLENT
            rec.successCount >= 5 -> SourceHealth.GOOD
            else -> SourceHealth.DEGRADED
        }
        emit()
    }

    fun recordFailure(sourceId: String) {
        val rec = records.getOrPut(sourceId) { HealthRecord() }
        rec.failCount++
        rec.consecutiveFailures++
        rec.health = when {
            rec.consecutiveFailures >= 5 -> SourceHealth.POOR
            rec.failCount >= 3 -> SourceHealth.DEGRADED
            else -> SourceHealth.UNKNOWN
        }
        emit()
    }

    fun getHealth(sourceId: String): SourceHealth =
        records[sourceId]?.health ?: SourceHealth.UNKNOWN

    fun reset(sourceId: String) {
        records.remove(sourceId)
        emit()
    }

    private fun emit() {
        _stats.value = records.toMap()
    }

    data class HealthRecord(
        var successCount: Int = 0,
        var failCount: Int = 0,
        var consecutiveFailures: Int = 0,
        var lastLatencyMs: Long = 0,
        var health: SourceHealth = SourceHealth.UNKNOWN,
    )
}
