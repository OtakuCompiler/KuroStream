package com.kurostream.legacyui.anistream.ui.profile

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "profiles")
@Parcelize
data class UserProfile(
    @PrimaryKey
    val id: String,
    val name: String,
    val avatarIndex: Int = 0,
    val avatarRes: Int = 0,
    val isKidsMode: Boolean = false,
    val pin: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val watchTimeMinutesToday: Int = 0,
    val dailyTimeLimitMinutes: Int = 120
) : Parcelable
