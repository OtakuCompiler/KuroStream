package com.kurostream.data.repository

import com.kurostream.domain.repository.AppTheme
import com.kurostream.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor() : SettingsRepository {
    private val themeFlow = MutableStateFlow(AppTheme.SYSTEM)
    private val dynamicColorsFlow = MutableStateFlow(true)
    private val autoUpdateExtensionsFlow = MutableStateFlow(true)
    private val defaultQualityFlow = MutableStateFlow("auto")
    private val skipIntroFlow = MutableStateFlow(true)
    private val skipOutroFlow = MutableStateFlow(true)
    private val cacheSizeFlow = MutableStateFlow(1024)

    override fun observeTheme(): Flow<AppTheme> = themeFlow
    override suspend fun setTheme(theme: AppTheme) { themeFlow.value = theme }
    override fun observeDynamicColorsEnabled(): Flow<Boolean> = dynamicColorsFlow
    override suspend fun setDynamicColorsEnabled(enabled: Boolean) { dynamicColorsFlow.value = enabled }
    override fun observeAutoUpdateExtensions(): Flow<Boolean> = autoUpdateExtensionsFlow
    override suspend fun setAutoUpdateExtensions(enabled: Boolean) { autoUpdateExtensionsFlow.value = enabled }
    override fun observeDefaultQuality(): Flow<String> = defaultQualityFlow
    override suspend fun setDefaultQuality(quality: String) { defaultQualityFlow.value = quality }
    override fun observeSkipIntroEnabled(): Flow<Boolean> = skipIntroFlow
    override suspend fun setSkipIntroEnabled(enabled: Boolean) { skipIntroFlow.value = enabled }
    override fun observeSkipOutroEnabled(): Flow<Boolean> = skipOutroFlow
    override suspend fun setSkipOutroEnabled(enabled: Boolean) { skipOutroFlow.value = enabled }
    override fun observeCacheSizeMb(): Flow<Int> = cacheSizeFlow
    override suspend fun setCacheSizeMb(size: Int) { cacheSizeFlow.value = size }
    override suspend fun clearAllSettings() {}
    override suspend fun <T> getSetting(key: String): T? = null
    override suspend fun <T> setSetting(key: String, value: T) {}
}
