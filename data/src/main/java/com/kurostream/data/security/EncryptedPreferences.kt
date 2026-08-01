package com.kurostream.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber

class EncryptedPreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "kurostream_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Timber.e(e, "EncryptedSharedPreferences failed, falling back")
        context.getSharedPreferences("kurostream_secure_fallback", Context.MODE_PRIVATE)
    }

    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getString(key: String, default: String? = null): String? = prefs.getString(key, default)
    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun remove(key: String) = prefs.edit().remove(key).apply()
    fun clear() = prefs.edit().clear().apply()
}