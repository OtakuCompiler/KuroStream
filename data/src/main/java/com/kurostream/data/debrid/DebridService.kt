package com.kurostream.data.debrid

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebridService @Inject constructor() {
    private val _activeProvider = MutableStateFlow<DebridProvider?>(null)
    val activeProvider: Flow<DebridProvider?> = _activeProvider.asStateFlow()

    enum class DebridProvider(val displayName: String, val signupUrl: String) {
        REAL_DEBRID(
            "Real-Debrid",
            "https://real-debrid.com"
        ),
        ALL_DEBRID(
            "AllDebrid",
            "https://alldebrid.com"
        ),
        PREMIUMIZE(
            "Premiumize",
            "https://www.premiumize.me"
        )
    }

    fun getAffiliateLink(provider: DebridProvider): String = provider.signupUrl
}
