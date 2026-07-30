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
            "https://real-debrid.com/?id=KUROSTREAM_AFFILIATE_ID"
        ),
        ALL_DEBRID(
            "AllDebrid",
            "https://alldebrid.com/register?aff=KUROSTREAM_AFF"
        ),
        PREMIUMIZE(
            "Premiumize",
            "https://www.premiumize.me/affiliate/KUROSTREAM"
        )
    }

    fun getAffiliateLink(provider: DebridProvider): String = provider.signupUrl
}
