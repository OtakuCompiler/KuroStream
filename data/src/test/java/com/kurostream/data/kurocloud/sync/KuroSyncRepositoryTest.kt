package com.kurostream.data.kurocloud.sync

import com.google.common.truth.Truth.assertThat
import com.kurostream.data.kurocloud.KuroCatalogItem
import com.kurostream.data.kurocloud.KuroCatalogResponse
import com.kurostream.data.kurocloud.KuroClaimPurchaseRequest
import com.kurostream.data.kurocloud.KuroMeResponse
import com.kurostream.data.kurocloud.KuroPurchase
import com.kurostream.data.kurocloud.KuroPurchase
import com.kurostream.data.kurocloud.KuroSetActiveSkinRequest
import com.kurostream.data.kurocloud.KuroSyncResponse
import com.kurostream.data.kurocloud.auth.KuroAuthTokens
import com.kurostream.data.kurocloud.auth.KuroTokenManager
import com.kurostream.data.kurocloud.db.KuroCloudDatabase
import com.kurostream.data.kurocloud.db.KuroEntitlementsDao
import com.kurostream.data.kurocloud.db.KuroCatalogDao
import com.kurostream.data.kurocloud.db.KuroPurchaseDao
import com.kurostream.data.kurocloud.db.KuroCatalogMetaDao
import com.kurostream.data.kurocloud.db.KuroEntitlementsEntity
import com.kurostream.data.kurocloud.db.KuroCatalogEntity
import com.kurostream.data.kurocloud.db.KuroPurchaseEntity
import com.kurostream.data.kurocloud.db.KuroCatalogMetaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

class KuroSyncRepositoryTest {

    @Mock
    lateinit var mockTokenManager: KuroTokenManager

    @Mock
    lateinit var mockEntitlementsDao: KuroEntitlementsDao

    @Mock
    lateinit var mockCatalogDao: KuroCatalogDao

    @Mock
    lateinit var mockPurchaseDao: KuroPurchaseDao

    @Mock
    lateinit var mockCatalogMetaDao: KuroCatalogMetaDao

    @Mock
    lateinit var mockDatabase: KuroCloudDatabase

    lateinit var mockWebServer: MockWebServer
    lateinit var api: com.kurostream.data.kurocloud.api.KuroApiService
    lateinit var repository: KuroSyncRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this).close()

        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()

        api = retrofit.create(com.kurostream.data.kurocloud.api.KuroApiService::class.java)

        // Mock database and DAOs
        org.mockito.Mockito.`when`(mockDatabase.entitlementsDao()).thenReturn(mockEntitlementsDao)
        org.mockito.Mockito.`when`(mockDatabase.catalogDao()).thenReturn(mockCatalogDao)
        org.mockito.Mockito.`when`(mockDatabase.purchaseDao()).thenReturn(mockPurchaseDao)
        org.mockito.Mockito.`when`(mockDatabase.catalogMetaDao()).thenReturn(mockCatalogMetaDao)

        org.mockito.Mockito.`when`(mockTokenManager.accessToken).thenReturn("test-access-token")

        repository = KuroSyncRepository(mockDatabase, api, mockTokenManager)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `syncEntitlements updates database with server data`() = runBlockingTest {
        // Setup mock response
        val meResponse = KuroMeResponse(
            user = com.kurostream.data.kurocloud.KuroUser(
                id = "user123",
                email = "test@test.com",
                displayName = "Test User",
                avatarUrl = null,
            ),
            entitlements = com.kurostream.data.kurocloud.KuroEntitlements(
                ownedItemIds = listOf("skin_1", "skin_2"),
                hasSkinsPass = true,
                activeSkinId = "skin_1",
            ),
            purchases = listOf(
                com.kurostream.data.kurocloud.KuroPurchase(
                    itemId = "skin_1",
                    amount = 4.99,
                    status = "completed",
                    createdAt = "2024-01-01T00:00:00Z",
                )
            ),
        )

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(meResponse))
            .addHeader("Content-Type", "application/json"))

        // Mock DAO responses
        org.mockito.Mockito.`when`(mockEntitlementsDao.insert(org.mockito.ArgumentMatchers.any()))
            .thenAnswer { invocation ->
                // Just return Unit for suspend function
                kotlin.Unit
            }

        // Execute
        repository.syncEntitlements()

        // Wait for sync
        kotlinx.coroutines.delay(100)

        // Verify request
        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/api/public/v1/me")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-access-token")

        // Verify DAO was called
        org.mockito.Mockito.verify(mockEntitlementsDao).insert(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `syncCatalog updates database with server catalog`() = runBlockingTest {
        val catalogResponse = KuroCatalogResponse(
            skins = listOf(
                KuroCatalogItem(
                    id = "skin_1",
                    name = "Arctic Fuse",
                    description = "Default theme",
                    price = 0.0,
                    currency = "USD",
                    skinId = "arctic_fuse",
                    type = "skin",
                    tier = "free",
                    previewImageUrl = null,
                ),
                KuroCatalogItem(
                    id = "skin_2",
                    name = "Kyuubi",
                    description = "Premium theme",
                    price = 4.99,
                    currency = "USD",
                    skinId = "kyuubi",
                    type = "skin",
                    tier = "premium",
                    previewImageUrl = null,
                ),
            ),
            passes = listOf(
                KuroCatalogItem(
                    id = "skins_pass",
                    name = "Skins Pass",
                    description = "All skins unlocked",
                    price = 0.0,
                    currency = "USD",
                    skinId = null,
                    type = "pass",
                    tier = "free",
                    previewImageUrl = null,
                ),
            ),
            updatedAt = "2024-01-01T00:00:00Z",
        )

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(catalogResponse))
            .addHeader("Content-Type", "application/json"))

        org.mockito.Mockito.`when`(mockCatalogDao.insertAll(org.mockito.ArgumentMatchers.any()))
            .thenAnswer { invocation ->
                kotlin.Unit
            }

        repository.syncCatalog()
        kotlinx.coroutines.delay(100)

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertThat(request.path).isEqualTo("/api/public/v1/catalog")
        org.mockito.Mockito.verify(mockCatalogDao).insertAll(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `claimFreeItem calls API and triggers re-sync`() = runBlockingTest {
        val syncResponse = KuroSyncResponse(
            added = com.kurostream.data.kurocloud.KuroSyncCounts(skins = 1),
            deleted = com.kurostream.data.kurocloud.KuroSyncCounts(),
        )

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(syncResponse))
            .addHeader("Content-Type", "application/json"))

        // Mock subsequent sync calls
        val meResponse = KuroMeResponse(
            user = com.kurostream.data.kurocloud.KuroUser(
                id = "user123",
                email = "test@test.com",
                displayName = "Test User",
                avatarUrl = null,
            ),
            entitlements = com.kurostream.data.kurocloud.KuroEntitlements(
                ownedItemIds = listOf("skin_1", "skin_2", "skins_pass"),
                hasSkinsPass = true,
                activeSkinId = "skin_1",
            ),
            purchases = emptyList(),
        )

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(meResponse))
            .addHeader("Content-Type", "application/json"))

        org.mockito.Mockito.`when`(mockEntitlementsDao.insert(org.mockito.ArgumentMatchers.any()))
            .thenAnswer { invocation ->
                kotlin.Unit
            }

        repository.claimFreeItem("skins_pass")
        kotlinx.coroutines.delay(200)

        // Verify claim request
        val claimRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertThat(claimRequest.method).isEqualTo("POST")
        assertThat(claimRequest.path).isEqualTo("/api/public/v1/purchases")

        val body = claimRequest.body.readUtf8()
        assertThat(body).contains("skins_pass")

        // Verify subsequent sync
        val meRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertThat(meRequest.path).isEqualTo("/api/public/v1/me")
    }

    @Test
    fun `setActiveSkin calls API and triggers entitlements sync`() = runBlockingTest {
        val syncResponse = KuroSyncResponse()

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(syncResponse))
            .addHeader("Content-Type", "application/json"))

        val meResponse = KuroMeResponse(
            user = com.kurostream.data.kurocloud.KuroUser(
                id = "user123",
                email = "test@test.com",
                displayName = "Test User",
                avatarUrl = null,
            ),
            entitlements = com.kurostream.data.kurocloud.KuroEntitlements(
                ownedItemIds = listOf("skin_1", "skin_2"),
                hasSkinsPass = true,
                activeSkinId = "skin_2",
            ),
            purchases = emptyList(),
        )

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(meResponse))
            .addHeader("Content-Type", "application/json"))

        org.mockito.Mockito.`when`(mockEntitlementsDao.insert(org.mockito.ArgumentMatchers.any()))
            .thenAnswer { invocation ->
                kotlin.Unit
            }

        repository.setActiveSkin("skin_2")
        kotlinx.coroutines.delay(200)

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/api/public/v1/active-skin")

        val body = request.body.readUtf8()
        assertThat(body).contains("skin_2")
    }

    @Test
    fun `checkoutPremiumItem returns correct web URL`() = runBlockingTest {
        val url = repository.checkoutPremiumItem("premium_skin")
        assertThat(url).isEqualTo("https://kuro-stream-tv.lovable.app/marketplace/premium_skin")
    }

    @Test
    fun `handle 401 triggers token refresh`() = runBlockingTest {
        // First request returns 401
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(401))

        // Token refresh succeeds
        val authTokens = KuroAuthTokens(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            expiresIn = 3600,
        )

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(authTokens))
            .addHeader("Content-Type", "application/json"))

        // Retry succeeds
        val meResponse = KuroMeResponse(
            user = com.kurostream.data.kurocloud.KuroUser(
                id = "user123",
                email = "test@test.com",
                displayName = "Test User",
                avatarUrl = null,
            ),
            entitlements = com.kurostream.data.kurocloud.KuroEntitlements(
                ownedItemIds = listOf("skin_1"),
                hasSkinsPass = false,
                activeSkinId = null,
            ),
            purchases = emptyList(),
        )

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(meResponse))
            .addHeader("Content-Type", "application/json"))

        org.mockito.Mockito.`when`(mockTokenManager.refreshToken).thenReturn("old-refresh-token")
        org.mockito.Mockito.`when`(mockTokenManager.accessToken).thenReturn("new-access-token")
        org.mockito.Mockito.`when`(mockEntitlementsDao.insert(org.mockito.ArgumentMatchers.any()))
            .thenAnswer { invocation ->
                kotlin.Unit
            }

        repository.syncEntitlements()
        kotlinx.coroutines.delay(200)

        // Should have made 3 requests: initial (401), refresh, retry (200)
        assertThat(mockWebServer.requestCount).isEqualTo(3)
    }

    @Test
    fun `handle 402 payment required returns checkout URL`() = runBlockingTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(402)
            .setBody("{\"error\": \"Payment required\"}")
            .addHeader("Content-Type", "application/json"))

        val url = repository.checkoutPremiumItem("premium_skin")
        assertThat(url).isEqualTo("https://kuro-stream-tv.lovable.app/marketplace/premium_skin")
    }

    @Test
    fun `handle 403 not entitled`() = runBlockingTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(403)
            .setBody("{\"error\": \"Not entitled\"}")
            .addHeader("Content-Type", "application/json"))

        // The repository should handle 403 gracefully
        repository.syncEntitlements()
        kotlinx.coroutines.delay(100)

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertThat(request.path).isEqualTo("/api/public/v1/me")
    }

    @Test
    fun `offline state uses cached data`() = runBlockingTest {
        // Simulate network failure
        mockWebServer.shutdown()

        // Should still expose cached flows (empty initially)
        val catalog = repository.catalog.first()
        assertThat(catalog).isEmpty()

        val entitlements = repository.entitlements.first()
        assertThat(entitlements).isEqualTo(com.kurostream.data.kurocloud.sync.KuroEntitlementsState.Empty)

        val purchases = repository.purchases.first()
        assertThat(purchases).isEmpty()
    }

    @Test
    fun `syncState emits correct states`() = runBlockingTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(KuroMeResponse(
                user = com.kurostream.data.kurocloud.KuroUser(
                    id = "user123",
                    email = "test@test.com",
                    displayName = "Test",
                    avatarUrl = null,
                ),
                entitlements = com.kurostream.data.kurocloud.KuroEntitlements(
                    ownedItemIds = listOf(),
                    hasSkinsPass = false,
                    activeSkinId = null,
                ),
                purchases = emptyList(),
            )))
            .addHeader("Content-Type", "application/json"))

        org.mockito.Mockito.`when`(mockEntitlementsDao.insert(org.mockito.ArgumentMatchers.any()))
            .thenAnswer { invocation ->
                kotlin.Unit
            }

        val states = mutableListOf<com.kurostream.data.kurocloud.sync.KuroSyncState>()

        runBlocking {
            kotlinx.coroutines.launch {
                repository.syncState.collect { states.add(it) }
            }
        }

        repository.syncAll()
        kotlinx.coroutines.delay(200)

        // Should emit Syncing then Idle
        assertThat(states).containsAtLeastOnce(com.kurostream.data.kurocloud.sync.KuroSyncState.Syncing)
        assertThat(states).containsAtLeastOnce(com.kurostream.data.kurocloud.sync.KuroSyncState.Idle)
    }
}