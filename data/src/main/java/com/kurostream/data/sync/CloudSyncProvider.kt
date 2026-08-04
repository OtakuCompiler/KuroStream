// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.sync

import com.kurostream.domain.result.Result
import com.kurostream.domain.sync.SyncPayload
import com.kurostream.domain.sync.SyncProvider
import com.kurostream.domain.sync.SyncTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

typealias productionCloudSyncProvider = CloudSyncProvider

/**
 * CloudSyncProvider — Supabase-backed cross-device sync for KuroStream.
 *
 * Pushes/pulls watch history, favourites, settings, and profiles across
 * devices via the Supabase PostgREST API.
 *
 * Table: kuro_sync (user_id UUID FK, payload JSONB, updated_at TIMESTAMPTZ)
 */
@Singleton
class CloudSyncProvider @Inject constructor(
    private val client: OkHttpClient,
) : SyncProvider {

    override val providerName: String = "supabase"
    override val isAuthenticated: Boolean get() = jwt.isNotBlank()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    // Supabase project — publishable values baked into APK (safe)
    private val supabaseUrl    = "https://kklyohtsedcdgmnmameh.supabase.co"
    private val supabaseAnonKey = "sb_publishable_x_ZB45-mADfu4479vmZdaw_SGpIE6Kx"

    private var jwt    = ""
    private var userId = ""

    private val JSON_MT = "application/json; charset=utf-8".toMediaType()

    // ── Authentication ─────────────────────────────────────────────────────

    override suspend fun authenticate(credentials: Map<String, String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val email    = credentials["email"]    ?: return@withContext Result.error(IllegalArgumentException("email required"))
                val password = credentials["password"] ?: return@withContext Result.error(IllegalArgumentException("password required"))

                val bodyStr = """{"email":"$email","password":"$password"}"""
                val response = client.newCall(
                    Request.Builder()
                        .url("$supabaseUrl/auth/v1/token?grant_type=password")
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Content-Type", "application/json")
                        .post(bodyStr.toRequestBody(JSON_MT))
                        .build()
                ).execute()

                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Timber.w("Supabase auth HTTP ${response.code}: $body")
                    return@withContext Result.error(Exception("Auth failed: ${response.code}"))
                }

                val elem = Json.parseToJsonElement(body) as? JsonObject
                jwt    = elem?.get("access_token")?.jsonPrimitive?.content ?: ""
                userId = (elem?.get("user") as? JsonObject)?.get("id")?.jsonPrimitive?.content ?: ""

                Timber.i("CloudSyncProvider: authenticated userId=$userId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "CloudSyncProvider.authenticate failed")
                Result.error(e)
            }
        }

    override suspend fun signOut(): Result<Unit> {
        jwt = ""; userId = ""
        Timber.i("CloudSyncProvider: signed out")
        return Result.success(Unit)
    }

    // ── Push ──────────────────────────────────────────────────────────────

    override suspend fun push(data: SyncPayload): Result<SyncTimestamp> =
        withContext(Dispatchers.IO) {
            if (!isAuthenticated) return@withContext Result.error(IllegalStateException("Not authenticated"))
            try {
                val payloadJson = json.encodeToString(data)
                val now = System.currentTimeMillis()
                val bodyStr = """{"user_id":"$userId","payload":${payloadJson},"updated_at":"${isoNow()}"}"""

                val response = client.newCall(
                    Request.Builder()
                        .url("$supabaseUrl/rest/v1/kuro_sync?on_conflict=user_id")
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $jwt")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "resolution=merge-duplicates")
                        .post(bodyStr.toRequestBody(JSON_MT))
                        .build()
                ).execute()

                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.error(Exception("Push failed: ${response.code} $err"))
                }
                Timber.d("CloudSyncProvider.push OK at $now")
                Result.success(SyncTimestamp(now, now))
            } catch (e: Exception) {
                Timber.e(e, "CloudSyncProvider.push threw")
                Result.error(e)
            }
        }

    // ── Pull ──────────────────────────────────────────────────────────────

    override suspend fun pull(lastSyncTimestamp: Long?): Result<SyncPayload?> =
        withContext(Dispatchers.IO) {
            if (!isAuthenticated) return@withContext Result.error(IllegalStateException("Not authenticated"))
            try {
                val url = buildString {
                    append("$supabaseUrl/rest/v1/kuro_sync?user_id=eq.$userId&select=payload,updated_at&limit=1")
                    lastSyncTimestamp?.let { append("&updated_at=gt.${isoFromMs(it)}") }
                }
                val response = client.newCall(
                    Request.Builder().url(url)
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $jwt")
                        .addHeader("Accept", "application/json")
                        .get().build()
                ).execute()

                val body = response.body?.string() ?: "[]"
                if (!response.isSuccessful) {
                    return@withContext Result.error(Exception("Pull failed: ${response.code} $body"))
                }

                val rows = Json.parseToJsonElement(body) as? JsonArray
                if (rows == null || rows.isEmpty()) {
                    return@withContext Result.success(null)
                }
                val payloadStr = (rows[0] as? JsonObject)?.get("payload")?.jsonPrimitive?.content
                    ?: return@withContext Result.success(null)

                val parsed = runCatching { json.decodeFromString<SyncPayload>(payloadStr) }.getOrNull()
                Timber.d("CloudSyncProvider.pull: ${payloadStr.length} bytes")
                Result.success(parsed)
            } catch (e: Exception) {
                Timber.e(e, "CloudSyncProvider.pull threw")
                Result.error(e)
            }
        }

    // ── Conflict resolution ───────────────────────────────────────────────

    /**
     * Last-write-wins merge:
     * - watchHistory: union by mediaId, keep newer lastWatched timestamp
     * - favorites: union of both sets
     * - settings: local wins (user is the authority on their own device)
     * - profiles: local wins
     */
    override suspend fun resolveConflicts(local: SyncPayload, remote: SyncPayload): SyncPayload {
        // Merge watch history — keep entry with larger timestamp
        val historyById = (local.watchHistory + remote.watchHistory)
            .groupBy { it.mediaId }
            .mapValues { (_, entries) -> entries.maxByOrNull { it.watchedAt } ?: entries.first() }

        // Union favourites by id
        val mergedFavs = (local.favorites + remote.favorites)
            .distinctBy { it.mediaId }

        return local.copy(
            watchHistory = historyById.values.toList(),
            favorites    = mergedFavs,
            timestamp    = maxOf(local.timestamp, remote.timestamp),
        )
    }

    // ── Delete ────────────────────────────────────────────────────────────

    override suspend fun deleteCloudData(): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!isAuthenticated) return@withContext Result.error(IllegalStateException("Not authenticated"))
            try {
                val response = client.newCall(
                    Request.Builder()
                        .url("$supabaseUrl/rest/v1/kuro_sync?user_id=eq.$userId")
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $jwt")
                        .delete().build()
                ).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.error(Exception("Delete failed: ${response.code}"))
                }
                Timber.i("CloudSyncProvider.deleteCloudData: deleted for $userId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "CloudSyncProvider.deleteCloudData threw")
                Result.error(e)
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────

    suspend fun pushLocalState(): Result<SyncTimestamp> = push(SyncPayload())

    fun buildPayloadFromLocal(): SyncPayload = SyncPayload()

    fun applyToLocal(payload: SyncPayload) {
        Timber.d("CloudSyncProvider.applyToLocal: ${payload.watchHistory.size} history items, ${payload.favorites.size} favourites")
    }

    private fun isoNow(): String = java.time.Instant.now().toString()
    private fun isoFromMs(ms: Long): String = java.time.Instant.ofEpochMilli(ms).toString()
}
