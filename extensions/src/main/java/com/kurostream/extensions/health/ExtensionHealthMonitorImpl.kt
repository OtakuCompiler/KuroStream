package com.kurostream.extensions.health

import com.kurostream.domain.extension.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionHealthMonitorImpl @Inject constructor(
    private val extensionRepository: com.kurostream.domain.extension.ExtensionRepository,
) : ExtensionHealthMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val healthMap = mutableMapOf<String, MutableStateFlow<ExtensionHealth>>()
    private val history = mutableMapOf<String, MutableList<HealthSnapshot>>()
    private val disabledByHealth = mutableSetOf<String>()

    override fun observeHealth(extensionId: String): Flow<ExtensionHealth> {
        return healthMap.getOrPut(extensionId) {
            MutableStateFlow(ExtensionHealth(extensionId, true, System.currentTimeMillis(), 1f, 0))
        }.asStateFlow()
    }

    override fun observeGlobalHealth(): Flow<Map<String, ExtensionHealth>> {
        return combine(*healthMap.values.toTypedArray()) { states ->
            states.associateBy { it.extensionId }
        }
    }

    override suspend fun checkHealth(extensionId: String): ExtensionHealth {
        val start = System.currentTimeMillis()
        return try {
            val ext = extensionRepository.getExtension(extensionId)
            val isHealthy = ext != null && ext.isEnabled
            val latency = System.currentTimeMillis() - start
            val snapshot = HealthSnapshot(isHealthy, latency, System.currentTimeMillis())
            recordSnapshot(extensionId, snapshot)
            val health = computeHealth(extensionId)
            healthMap.getOrPut(extensionId) { MutableStateFlow(health) }.update { health }
            health
        } catch (e: Exception) {
            Timber.e(e, "Health check failed for $extensionId")
            val snapshot = HealthSnapshot(false, System.currentTimeMillis() - start, System.currentTimeMillis(), e.message)
            recordSnapshot(extensionId, snapshot)
            val health = computeHealth(extensionId)
            healthMap.getOrPut(extensionId) { MutableStateFlow(health) }.update { health }
            health
        }
    }

    override suspend fun runDiagnostics(): List<HealthIssue> {
        val issues = mutableListOf<HealthIssue>()
        healthMap.values.forEach { state ->
            val health = state.value
            if (!health.isHealthy && health.consecutiveFailures > 10) {
                issues.add(HealthIssue(health.extensionId, IssueSeverity.CRITICAL, "Extension is broken and has ${health.consecutiveFailures} consecutive failures", System.currentTimeMillis()))
            } else if (!health.isHealthy) {
                issues.add(HealthIssue(health.extensionId, IssueSeverity.MEDIUM, health.lastError ?: "Extension is unhealthy", System.currentTimeMillis()))
            } else if (health.successRate < 0.5f) {
                issues.add(HealthIssue(health.extensionId, IssueSeverity.LOW, "Low success rate: ${(health.successRate * 100).toInt()}%", System.currentTimeMillis()))
            }
        }
        return issues
    }

    override suspend fun autoDisableBroken() {
        healthMap.values.forEach { state ->
            val health = state.value
            if (health.consecutiveFailures > 10 && !disabledByHealth.contains(health.extensionId)) {
                try {
                    extensionRepository.disable(health.extensionId)
                    disabledByHealth.add(health.extensionId)
                    Timber.w("Auto-disabled broken extension: ${health.extensionId}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to auto-disable extension: ${health.extensionId}")
                }
            }
        }
    }

    override suspend fun autoEnableFixed() {
        disabledByHealth.toList().forEach { extensionId ->
            val health = healthMap[extensionId]?.value ?: return@forEach
            if (health.isHealthy && health.consecutiveFailures == 0) {
                try {
                    extensionRepository.enable(extensionId)
                    disabledByHealth.remove(extensionId)
                    Timber.i("Auto-enabled fixed extension: $extensionId")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to auto-enable extension: $extensionId")
                }
            }
        }
    }

    init {
        scope.launch {
            while (isActive) {
                delay(300000)
                healthMap.keys.forEach { extensionId ->
                    launch { checkHealth(extensionId) }
                }
                delay(5000)
                launch { autoDisableBroken() }
                launch { autoEnableFixed() }
            }
        }
    }

    private fun recordSnapshot(extensionId: String, snapshot: HealthSnapshot) {
        history.getOrPut(extensionId) { mutableListOf() }.add(snapshot)
        if (history[extensionId]!!.size > 100) {
            history[extensionId]!!.removeAt(0)
        }
    }

    private fun computeHealth(extensionId: String): ExtensionHealth {
        val snapshots = history[extensionId] ?: return ExtensionHealth(extensionId, true, System.currentTimeMillis(), 1f, 0)
        val recent = snapshots.takeLast(100)
        val successRate = recent.count { it.isHealthy }.toFloat() / recent.size.coerceAtLeast(1)
        val consecutiveFailures = recent.takeLastWhile { !it.isHealthy }.size
        val isHealthy = consecutiveFailures <= 10 && successRate >= 0.1f
        val lastError = recent.lastOrNull { !it.isHealthy }?.error
        return ExtensionHealth(
            extensionId = extensionId,
            isHealthy = isHealthy,
            lastCheck = System.currentTimeMillis(),
            successRate = successRate,
            consecutiveFailures = consecutiveFailures,
            lastError = lastError,
            latencyMs = recent.lastOrNull()?.latencyMs ?: 0,
        )
    }
}

data class HealthSnapshot(
    val isHealthy: Boolean,
    val latencyMs: Long,
    val timestamp: Long,
    val error: String? = null,
)
