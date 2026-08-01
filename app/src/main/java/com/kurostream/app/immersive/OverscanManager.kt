package com.kurostream.app.immersive

import android.content.Context
import android.graphics.Rect

class OverscanManager(context: Context) {
    private val overscanPercent = 0.05f

    fun getSafeInsets(width: Int, height: Int): Rect {
        val horizontal = (width * overscanPercent).toInt()
        val vertical = (height * overscanPercent).toInt()
        return Rect(horizontal, vertical, width - horizontal, height - vertical)
    }
}
