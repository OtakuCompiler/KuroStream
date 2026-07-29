package com.kurostream.app.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PermissionManager {
    private val _permissionState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val permissionState: StateFlow<Map<String, Boolean>> = _permissionState.asStateFlow()

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReadStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun hasWriteStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun hasCameraPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.CAMERA)
    }

    fun hasRecordAudioPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO)
    }

    fun requestReadStoragePermission(activity: Activity, requestCode: Int = 1001) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    fun requestWriteStoragePermission(activity: Activity, requestCode: Int = 1002) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                requestCode
            )
        }
    }

    fun requestCameraPermission(activity: Activity, requestCode: Int = 1003) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), requestCode)
    }

    fun requestRecordAudioPermission(activity: Activity, requestCode: Int = 1004) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), requestCode)
    }

    fun updatePermissionState(context: Context, permissions: Array<out String>, grantResults: IntArray) {
        val newState = _permissionState.value.toMutableMap()
        permissions.forEachIndexed { index, permission ->
            newState[permission] = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
        }
        _permissionState.value = newState
    }

    fun getRequiredStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}