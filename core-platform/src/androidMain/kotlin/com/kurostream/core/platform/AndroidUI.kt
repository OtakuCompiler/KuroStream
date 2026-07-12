// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.core.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast

class AndroidUI(private val context: Context) : PlatformUI {
    
    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    override fun shareText(text: String, subject: String?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
    
    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun showError(message: String) {
        Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
    }
    
    override fun vibrate(pattern: LongArray?) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pattern?.let { vibrator.vibrate(VibrationEffect.createWaveform(it, -1)) }
                ?: vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            pattern?.let { vibrator.vibrate(it, -1) } ?: vibrator.vibrate(100)
        }
    }
    
    override fun requestFocus(focusable: Focusable) {
        // Android TV focus is handled by the framework
    }
    
    override fun clearFocus() {
        (context as? Activity)?.currentFocus?.clearFocus()
    }
    
    override fun setDisplayRefreshRate(rate: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = (context as? Activity)?.display
            display?.let { d ->
                val mode = d.supportedModes.firstOrNull() ?: d.mode
                val currentRate = mode.refreshRate
                if (Math.abs(currentRate - rate) > 0.1f) {
                    // Request display refresh rate change
                    // This requires the CHANGE_CONFIGURATION permission
                }
            }
        }
    }
    
    override fun getDisplayRefreshRate(): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (context as? Activity)?.display?.mode?.refreshRate ?: 60f
        } else {
            60f
        }
    }
    
    override fun setImmersiveMode(enabled: Boolean) {
        val activity = context as? Activity ?: return
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (enabled) {
                controller?.hide(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or 
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            } else {
                controller?.show(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
            }
        } else {
            if (enabled) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or 
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
    
    override fun isImmersiveMode(): Boolean {
        val activity = context as? Activity ?: return false
        return (activity.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0
    }
}