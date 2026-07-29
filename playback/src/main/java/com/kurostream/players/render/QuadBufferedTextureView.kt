package com.kurostream.players.render

import android.content.Context
import android.widget.FrameLayout
import android.view.TextureView

class QuadBufferedTextureView(context: Context) : FrameLayout(context) {
    private val textureViews = listOf(
        TextureView(context),
        TextureView(context),
        TextureView(context),
        TextureView(context)
    )
    private var currentIndex = 0

    init {
        textureViews.forEach { addView(it) }
        textureViews.drop(1).forEach { it.visibility = GONE }
    }

    fun swapBuffers() {
        textureViews[currentIndex].visibility = GONE
        currentIndex = (currentIndex + 1) % 4
        textureViews[currentIndex].visibility = VISIBLE
    }

    fun getSurfaceTexture() = textureViews[currentIndex].surfaceTexture
}