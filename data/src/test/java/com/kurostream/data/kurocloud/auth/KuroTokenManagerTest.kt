package com.kurostream.data.kurocloud.auth

import com.google.common.truth.Truth.assertThat
import com.kurostream.data.security.EncryptedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify

class KuroTokenManagerTest {

    @Mock
    lateinit var mockPrefs: EncryptedPreferences

    lateinit var tokenManager: KuroTokenManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this).close()
        tokenManager = KuroTokenManager(mockPrefs)
    }

    @After
    fun teardown() {}

    @Test
    fun `initial state has no tokens`() {
        assertThat(tokenManager.accessToken).isNull()
        assertThat(tokenManager.refreshToken).isNull()
        assertThat(tokenManager.isTokenValid()).isFalse()
    }

    @Test
    fun `saveTokens stores all values`() = runTest {
        tokenManager.saveTokens("access-123", "refresh-456", 3600)

        assertThat(tokenManager.accessToken).isEqualTo("access-123")
        assertThat(tokenManager.refreshToken).isEqualTo("refresh-456")
        assertThat(tokenManager.expiryTimestamp).isGreaterThan(System.currentTimeMillis())
    }

    @Test
    fun `isTokenValid returns true for valid token`() = runTest {
        tokenManager.saveTokens("access-123", "refresh-456", 3600)

        assertThat(tokenManager.isTokenValid()).isTrue()
    }

    @Test
    fun `isTokenValid returns false for expired token`() = runTest {
        tokenManager.saveTokens("access-123", "refresh-456", -1) // Expired

        assertThat(tokenManager.isTokenValid()).isFalse()
    }

    @Test
    fun `clear removes all tokens`() = runTest {
        tokenManager.saveTokens("access-123", "refresh-456", 3600)
        tokenManager.clear()

        assertThat(tokenManager.accessToken).isNull()
        assertThat(tokenManager.refreshToken).isNull()
        assertThat(tokenManager.isTokenValid()).isFalse()
    }

    @Test
    fun `thread safety - concurrent access`() = runTest {
        val threads = 10
        val iterations = 100

        val jobs = (1..threads).map { i ->
            launch(Dispatchers.IO) {
                repeat(iterations) {
                    val access = "access-$i-$it"
                    val refresh = "refresh-$i-$it"
                    tokenManager.saveTokens(access, refresh, 3600)
                    val retrieved = tokenManager.accessToken
                    assertThat(retrieved).isNotNull()
                }
            }
        }

        jobs.forEach { it.join() }
    }
}