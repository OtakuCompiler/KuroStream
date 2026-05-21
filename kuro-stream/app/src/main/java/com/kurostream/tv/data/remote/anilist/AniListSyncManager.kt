package com.kurostream.tv.data.remote.anilist

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AniList OAuth and sync manager.
 * Handles authentication, list sync, and progress updates with AniList.
 */
@Singleton
class AniListSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val okHttpClient: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher
) {
    
    companion object {
        private const val ANILIST_OAUTH_URL = "https://anilist.co/api/v2/oauth/authorize"
        private const val ANILIST_TOKEN_URL = "https://anilist.co/api/v2/oauth/token"
        private const val ANILIST_GRAPHQL_URL = "https://graphql.anilist.co"
        
        // These should be stored securely and retrieved from BuildConfig or encrypted storage
        private const val CLIENT_ID = "YOUR_CLIENT_ID"
        private const val CLIENT_SECRET = "YOUR_CLIENT_SECRET"
        private const val REDIRECT_URI = "kurostream://anilist-callback"
        
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("anilist_access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("anilist_refresh_token")
        private val KEY_TOKEN_EXPIRY = longPreferencesKey("anilist_token_expiry")
        private val KEY_USER_ID = stringPreferencesKey("anilist_user_id")
        private val KEY_USERNAME = stringPreferencesKey("anilist_username")
        private val KEY_AVATAR_URL = stringPreferencesKey("anilist_avatar_url")
    }
    
    /**
     * Check if user is logged in to AniList.
     */
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ACCESS_TOKEN] != null
    }
    
    /**
     * Get current user info.
     */
    val currentUser: Flow<AniListUser?> = dataStore.data.map { prefs ->
        val userId = prefs[KEY_USER_ID] ?: return@map null
        val username = prefs[KEY_USERNAME] ?: return@map null
        AniListUser(
            id = userId,
            name = username,
            avatarUrl = prefs[KEY_AVATAR_URL]
        )
    }
    
    /**
     * Build OAuth authorization URL.
     */
    fun buildAuthorizationUrl(): String {
        return Uri.parse(ANILIST_OAUTH_URL)
            .buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .build()
            .toString()
    }
    
    /**
     * Open AniList authorization page in browser.
     */
    fun startOAuthFlow(): Intent {
        val authUrl = buildAuthorizationUrl()
        return Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    /**
     * Handle OAuth callback and exchange code for tokens.
     */
    suspend fun handleOAuthCallback(code: String): Result<AniListUser> = withContext(ioDispatcher) {
        try {
            // Exchange authorization code for access token
            val tokenJson = JSONObject().apply {
                put("grant_type", "authorization_code")
                put("client_id", CLIENT_ID)
                put("client_secret", CLIENT_SECRET)
                put("redirect_uri", REDIRECT_URI)
                put("code", code)
            }
            
            val request = Request.Builder()
                .url(ANILIST_TOKEN_URL)
                .post(tokenJson.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to exchange code for token"))
            }
            
            val responseBody = response.body?.string() ?: return@withContext Result.failure(
                Exception("Empty response")
            )
            val tokenResponse = JSONObject(responseBody)
            
            val accessToken = tokenResponse.getString("access_token")
            val refreshToken = tokenResponse.optString("refresh_token")
            val expiresIn = tokenResponse.getLong("expires_in")
            val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
            
            // Save tokens
            dataStore.edit { prefs ->
                prefs[KEY_ACCESS_TOKEN] = accessToken
                if (refreshToken.isNotEmpty()) {
                    prefs[KEY_REFRESH_TOKEN] = refreshToken
                }
                prefs[KEY_TOKEN_EXPIRY] = expiryTime
            }
            
            // Fetch user info
            val user = fetchCurrentUser(accessToken)
            if (user != null) {
                dataStore.edit { prefs ->
                    prefs[KEY_USER_ID] = user.id
                    prefs[KEY_USERNAME] = user.name
                    user.avatarUrl?.let { prefs[KEY_AVATAR_URL] = it }
                }
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to fetch user info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Logout from AniList.
     */
    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_TOKEN_EXPIRY)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_AVATAR_URL)
        }
    }
    
    /**
     * Get access token, refreshing if necessary.
     */
    private suspend fun getValidAccessToken(): String? {
        val prefs = dataStore.data.first()
        val accessToken = prefs[KEY_ACCESS_TOKEN] ?: return null
        val expiry = prefs[KEY_TOKEN_EXPIRY] ?: 0L
        
        // Check if token is expired or will expire soon (within 5 minutes)
        if (System.currentTimeMillis() > expiry - (5 * 60 * 1000)) {
            val refreshToken = prefs[KEY_REFRESH_TOKEN]
            if (refreshToken != null) {
                return refreshAccessToken(refreshToken)
            }
            return null
        }
        
        return accessToken
    }
    
    /**
     * Refresh access token.
     */
    private suspend fun refreshAccessToken(refreshToken: String): String? {
        return try {
            val tokenJson = JSONObject().apply {
                put("grant_type", "refresh_token")
                put("client_id", CLIENT_ID)
                put("client_secret", CLIENT_SECRET)
                put("refresh_token", refreshToken)
            }
            
            val request = Request.Builder()
                .url(ANILIST_TOKEN_URL)
                .post(tokenJson.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            
            val responseBody = response.body?.string() ?: return null
            val tokenResponse = JSONObject(responseBody)
            
            val newAccessToken = tokenResponse.getString("access_token")
            val newRefreshToken = tokenResponse.optString("refresh_token", refreshToken)
            val expiresIn = tokenResponse.getLong("expires_in")
            val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
            
            dataStore.edit { prefs ->
                prefs[KEY_ACCESS_TOKEN] = newAccessToken
                prefs[KEY_REFRESH_TOKEN] = newRefreshToken
                prefs[KEY_TOKEN_EXPIRY] = expiryTime
            }
            
            newAccessToken
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Execute GraphQL query.
     */
    private suspend fun executeGraphQL(
        query: String,
        variables: JSONObject? = null,
        accessToken: String? = null
    ): JSONObject? {
        return try {
            val body = JSONObject().apply {
                put("query", query)
                variables?.let { put("variables", it) }
            }
            
            val requestBuilder = Request.Builder()
                .url(ANILIST_GRAPHQL_URL)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
            
            accessToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: return null
            JSONObject(responseBody)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Fetch current authenticated user info.
     */
    private suspend fun fetchCurrentUser(accessToken: String): AniListUser? {
        val query = """
            query {
                Viewer {
                    id
                    name
                    avatar {
                        large
                        medium
                    }
                }
            }
        """.trimIndent()
        
        val response = executeGraphQL(query, accessToken = accessToken)
        val viewer = response?.optJSONObject("data")?.optJSONObject("Viewer") ?: return null
        
        return AniListUser(
            id = viewer.getString("id"),
            name = viewer.getString("name"),
            avatarUrl = viewer.optJSONObject("avatar")?.optString("large")
                ?: viewer.optJSONObject("avatar")?.optString("medium")
        )
    }
    
    /**
     * Get user's anime list.
     */
    fun getAnimeList(status: AniListMediaStatus? = null): Flow<Result<List<AniListMediaEntry>>> = flow {
        val accessToken = getValidAccessToken()
        if (accessToken == null) {
            emit(Result.failure(Exception("Not logged in")))
            return@flow
        }
        
        val query = """
            query (${"$"}userId: Int, ${"$"}status: MediaListStatus) {
                MediaListCollection(userId: ${"$"}userId, type: ANIME, status: ${"$"}status) {
                    lists {
                        entries {
                            id
                            mediaId
                            status
                            score
                            progress
                            repeat
                            startedAt {
                                year
                                month
                                day
                            }
                            completedAt {
                                year
                                month
                                day
                            }
                            media {
                                id
                                idMal
                                title {
                                    romaji
                                    english
                                    native
                                }
                                coverImage {
                                    large
                                    medium
                                }
                                episodes
                                status
                                averageScore
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val prefs = dataStore.data.first()
        val userId = prefs[KEY_USER_ID]?.toIntOrNull()
        
        val variables = JSONObject().apply {
            userId?.let { put("userId", it) }
            status?.let { put("status", it.name) }
        }
        
        val response = executeGraphQL(query, variables, accessToken)
        val lists = response?.optJSONObject("data")
            ?.optJSONObject("MediaListCollection")
            ?.optJSONArray("lists")
        
        if (lists == null) {
            emit(Result.failure(Exception("Failed to fetch anime list")))
            return@flow
        }
        
        val entries = mutableListOf<AniListMediaEntry>()
        for (i in 0 until lists.length()) {
            val list = lists.getJSONObject(i)
            val listEntries = list.optJSONArray("entries") ?: continue
            
            for (j in 0 until listEntries.length()) {
                val entry = listEntries.getJSONObject(j)
                entries.add(parseMediaEntry(entry))
            }
        }
        
        emit(Result.success(entries))
    }.flowOn(ioDispatcher)
    
    /**
     * Update anime progress.
     */
    suspend fun updateProgress(
        mediaId: Int,
        progress: Int,
        status: AniListMediaStatus? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        val accessToken = getValidAccessToken()
            ?: return@withContext Result.failure(Exception("Not logged in"))
        
        val mutation = """
            mutation (${"$"}mediaId: Int, ${"$"}progress: Int, ${"$"}status: MediaListStatus) {
                SaveMediaListEntry(mediaId: ${"$"}mediaId, progress: ${"$"}progress, status: ${"$"}status) {
                    id
                    progress
                    status
                }
            }
        """.trimIndent()
        
        val variables = JSONObject().apply {
            put("mediaId", mediaId)
            put("progress", progress)
            status?.let { put("status", it.name) }
        }
        
        val response = executeGraphQL(mutation, variables, accessToken)
        val savedEntry = response?.optJSONObject("data")?.optJSONObject("SaveMediaListEntry")
        
        if (savedEntry != null) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to update progress"))
        }
    }
    
    /**
     * Update anime score.
     */
    suspend fun updateScore(mediaId: Int, score: Float): Result<Unit> = withContext(ioDispatcher) {
        val accessToken = getValidAccessToken()
            ?: return@withContext Result.failure(Exception("Not logged in"))
        
        val mutation = """
            mutation (${"$"}mediaId: Int, ${"$"}score: Float) {
                SaveMediaListEntry(mediaId: ${"$"}mediaId, score: ${"$"}score) {
                    id
                    score
                }
            }
        """.trimIndent()
        
        val variables = JSONObject().apply {
            put("mediaId", mediaId)
            put("score", score)
        }
        
        val response = executeGraphQL(mutation, variables, accessToken)
        val savedEntry = response?.optJSONObject("data")?.optJSONObject("SaveMediaListEntry")
        
        if (savedEntry != null) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to update score"))
        }
    }
    
    /**
     * Search anime on AniList.
     */
    fun searchAnime(query: String, page: Int = 1, perPage: Int = 20): Flow<Result<List<AniListMedia>>> = flow {
        val graphqlQuery = """
            query (${"$"}search: String, ${"$"}page: Int, ${"$"}perPage: Int) {
                Page(page: ${"$"}page, perPage: ${"$"}perPage) {
                    media(search: ${"$"}search, type: ANIME) {
                        id
                        idMal
                        title {
                            romaji
                            english
                            native
                        }
                        coverImage {
                            large
                            medium
                        }
                        bannerImage
                        description
                        episodes
                        status
                        averageScore
                        genres
                        season
                        seasonYear
                    }
                }
            }
        """.trimIndent()
        
        val variables = JSONObject().apply {
            put("search", query)
            put("page", page)
            put("perPage", perPage)
        }
        
        val response = executeGraphQL(graphqlQuery, variables)
        val mediaList = response?.optJSONObject("data")
            ?.optJSONObject("Page")
            ?.optJSONArray("media")
        
        if (mediaList == null) {
            emit(Result.failure(Exception("Failed to search anime")))
            return@flow
        }
        
        val results = mutableListOf<AniListMedia>()
        for (i in 0 until mediaList.length()) {
            results.add(parseMedia(mediaList.getJSONObject(i)))
        }
        
        emit(Result.success(results))
    }.flowOn(ioDispatcher)
    
    /**
     * Get anime details by AniList ID.
     */
    suspend fun getAnimeDetails(anilistId: Int): Result<AniListMedia?> = withContext(ioDispatcher) {
        val query = """
            query (${"$"}id: Int) {
                Media(id: ${"$"}id, type: ANIME) {
                    id
                    idMal
                    title {
                        romaji
                        english
                        native
                    }
                    coverImage {
                        extraLarge
                        large
                        medium
                    }
                    bannerImage
                    description
                    episodes
                    duration
                    status
                    averageScore
                    popularity
                    genres
                    season
                    seasonYear
                    startDate {
                        year
                        month
                        day
                    }
                    endDate {
                        year
                        month
                        day
                    }
                    studios {
                        nodes {
                            name
                        }
                    }
                    trailer {
                        id
                        site
                    }
                    relations {
                        edges {
                            relationType
                            node {
                                id
                                title {
                                    romaji
                                }
                                coverImage {
                                    medium
                                }
                                type
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val variables = JSONObject().apply {
            put("id", anilistId)
        }
        
        val response = executeGraphQL(query, variables)
        val media = response?.optJSONObject("data")?.optJSONObject("Media")
        
        if (media != null) {
            Result.success(parseMedia(media))
        } else {
            Result.success(null)
        }
    }
    
    private fun parseMediaEntry(json: JSONObject): AniListMediaEntry {
        val mediaJson = json.optJSONObject("media")
        return AniListMediaEntry(
            id = json.getInt("id"),
            mediaId = json.getInt("mediaId"),
            status = AniListMediaStatus.valueOf(json.getString("status")),
            score = json.optDouble("score", 0.0).toFloat(),
            progress = json.optInt("progress", 0),
            repeat = json.optInt("repeat", 0),
            media = mediaJson?.let { parseMedia(it) }
        )
    }
    
    private fun parseMedia(json: JSONObject): AniListMedia {
        val titleJson = json.optJSONObject("title")
        val coverJson = json.optJSONObject("coverImage")
        val genresArray = json.optJSONArray("genres")
        
        val genres = mutableListOf<String>()
        genresArray?.let {
            for (i in 0 until it.length()) {
                genres.add(it.getString(i))
            }
        }
        
        return AniListMedia(
            id = json.getInt("id"),
            idMal = json.optInt("idMal", 0).takeIf { it > 0 },
            titleRomaji = titleJson?.optString("romaji"),
            titleEnglish = titleJson?.optString("english"),
            titleNative = titleJson?.optString("native"),
            coverImageLarge = coverJson?.optString("large") ?: coverJson?.optString("extraLarge"),
            coverImageMedium = coverJson?.optString("medium"),
            bannerImage = json.optString("bannerImage").takeIf { it.isNotEmpty() },
            description = json.optString("description").takeIf { it.isNotEmpty() },
            episodes = json.optInt("episodes", 0).takeIf { it > 0 },
            duration = json.optInt("duration", 0).takeIf { it > 0 },
            status = json.optString("status").takeIf { it.isNotEmpty() },
            averageScore = json.optInt("averageScore", 0).takeIf { it > 0 },
            popularity = json.optInt("popularity", 0).takeIf { it > 0 },
            genres = genres,
            season = json.optString("season").takeIf { it.isNotEmpty() },
            seasonYear = json.optInt("seasonYear", 0).takeIf { it > 0 }
        )
    }
}

/**
 * AniList user model.
 */
data class AniListUser(
    val id: String,
    val name: String,
    val avatarUrl: String?
)

/**
 * AniList media list status.
 */
enum class AniListMediaStatus {
    CURRENT,
    PLANNING,
    COMPLETED,
    DROPPED,
    PAUSED,
    REPEATING
}

/**
 * AniList media entry (user's list entry).
 */
data class AniListMediaEntry(
    val id: Int,
    val mediaId: Int,
    val status: AniListMediaStatus,
    val score: Float,
    val progress: Int,
    val repeat: Int,
    val media: AniListMedia?
)

/**
 * AniList media model.
 */
data class AniListMedia(
    val id: Int,
    val idMal: Int?,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val coverImageLarge: String?,
    val coverImageMedium: String?,
    val bannerImage: String?,
    val description: String?,
    val episodes: Int?,
    val duration: Int?,
    val status: String?,
    val averageScore: Int?,
    val popularity: Int?,
    val genres: List<String>,
    val season: String?,
    val seasonYear: Int?
) {
    val displayTitle: String
        get() = titleEnglish ?: titleRomaji ?: titleNative ?: "Unknown"
}
