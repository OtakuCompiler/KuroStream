package com.kurostream.data.kurocloud.auth

import com.google.common.truth.Truth.assertThat
import com.kurostream.data.kurocloud.KuroAuthTokens
import com.kurostream.data.kurocloud.KuroSignInRequest
import com.kurostream.data.kurocloud.KuroSignUpRequest
import com.kurostream.data.kurocloud.KuroRefreshRequest
import com.kurostream.data.security.EncryptedPreferences
import kotlinx.coroutines.Dispatchers
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

class KuroAuthServiceTest {

    @Mock
    lateinit var mockPrefs: EncryptedPreferences

    lateinit var mockWebServer: MockWebServer
    lateinit var authApi: KuroAuthService

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

        authApi = retrofit.create(KuroAuthService::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `signInWithPassword returns tokens on success`() = runBlockingTest {
        val tokens = KuroAuthTokens(
            accessToken = "access-token-123",
            refreshToken = "refresh-token-456",
            expiresIn = 3600,
        )

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(tokens))
            .addHeader("Content-Type", "application/json"))

        val response = runBlocking {
            authApi.signInWithPassword("test-key", KuroSignInRequest("user@test.com", "password123"))
        }

        assertThat(response.accessToken).isEqualTo("access-token-123")
        assertThat(response.refreshToken).isEqualTo("refresh-token-456")
        assertThat(response.expiresIn).isEqualTo(3600)

        val request = mockWebServer.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS)
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).contains("/auth/v1/token?grant_type=password")
        assertThat(request.getHeader("apikey")).isEqualTo("test-key")

        val body = request.body.readUtf8()
        assertThat(body).contains("user@test.com")
        assertThat(body).contains("password123")
    }

    @Test
    fun `signUp returns tokens on success`() = runBlockingTest {
        val tokens = KuroAuthTokens(
            accessToken = "access-token-new",
            refreshToken = "refresh-token-new",
            expiresIn = 3600,
        )

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(tokens))
            .addHeader("Content-Type", "application/json"))

        val response = runBlocking {
            authApi.signUp("test-key", KuroSignUpRequest("new@test.com", "password123"))
        }

        assertThat(response.accessToken).isEqualTo("access-token-new")
        assertThat(response.refreshToken).isEqualTo("refresh-token-new")
    }

    @Test
    fun `refreshToken returns new tokens`() = runBlockingTest {
        val tokens = KuroAuthTokens(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            expiresIn = 3600,
        )

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(kotlinx.serialization.json.Json.encodeToString(tokens))
            .addHeader("Content-Type", "application/json"))

        val response = runBlocking {
            authApi.refreshToken("test-key", KuroRefreshRequest("old-refresh-token"))
        }

        assertThat(response.accessToken).isEqualTo("new-access-token")

        val request = mockWebServer.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS)
        assertThat(request.path).contains("/auth/v1/token?grant_type=refresh_token")

        val body = request.body.readUtf8()
        assertThat(body).contains("old-refresh-token")
    }

    @Test
    fun `signInWithPassword throws on invalid credentials`() = runBlockingTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\": \"Invalid credentials\"}")
            .addHeader("Content-Type", "application/json"))

        var exception: Exception? = null
        try {
            runBlocking {
                authApi.signInWithPassword("test-key", KuroSignInRequest("wrong@test.com", "wrong"))
            }
        } catch (e: Exception) {
            exception = e
        }

        assertThat(exception).isNotNull()
    }
}