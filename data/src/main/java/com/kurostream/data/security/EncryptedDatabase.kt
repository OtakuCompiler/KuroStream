package com.kurostream.data.security

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

object EncryptedDatabase {
    fun <T : RoomDatabase> create(
        context: Context,
        klass: Class<T>,
        name: String,
        passphrase: String = getDefaultPassphrase(context),
    ): T {
        val factory = SupportFactory(passphrase.toByteArray())
        return Room.databaseBuilder(context, klass, name)
            .openHelperFactory(factory)
            .build()
    }

    private fun getDefaultPassphrase(context: Context): String {
        val signature = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
        ).signingInfo?.apkContentsSigners?.firstOrNull()?.toCharsString() ?: ""
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: ""
        return (signature + deviceId).take(32).padEnd(32, '0')
    }
}