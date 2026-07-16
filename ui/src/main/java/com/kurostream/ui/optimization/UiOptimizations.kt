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

package com.kurostream.ui.optimization

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Texture Atlas for UI - Optimization #11
 * 
 * Combines small UI images into a single texture to reduce draw calls.
 * 
 * Target: 60fps UI, reduced GPU overhead
 */
class UiTextureAtlas(
    private val maxSize: Int = 1024
) {
    private val TAG = "UiTextureAtlas"
    private val atlasBitmapStore by lazy { Bitmap.createBitmap(maxSize, maxSize, Bitmap.Config.RGB_565) }
    private val canvas by lazy { Canvas(atlasBitmapStore) }
    private val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
    
    private var currentX = 0
    private var currentY = 0
    private var rowHeight = 0
    private val padding = 2
    
    private val regions = ConcurrentHashMap<String, AtlasRegion>()
    private val bitmapCache = LruCache<String, Bitmap>(50)
    
    data class AtlasRegion(
        val name: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val u1: Float,
        val v1: Float,
        val u2: Float,
        val v2: Float
    )
    
    fun addBitmap(name: String, bitmap: Bitmap): Boolean {
        val paddedWidth = bitmap.width + padding * 2
        val paddedHeight = bitmap.height + padding * 2
        
        if (currentX + paddedWidth > maxSize) {
            currentX = 0
            currentY += rowHeight + padding
            rowHeight = 0
        }
        
        if (currentY + paddedHeight > maxSize) {
            Log.w(TAG, "Atlas full, cannot add $name")
            return false
        }
        
        canvas.drawBitmap(bitmap, (currentX + padding).toFloat(), (currentY + padding).toFloat(), paint)
        
        val region = AtlasRegion(
            name = name,
            x = currentX + padding,
            y = currentY + padding,
            width = bitmap.width,
            height = bitmap.height,
            u1 = (currentX + padding).toFloat() / maxSize,
            v1 = (currentY + padding).toFloat() / maxSize,
            u2 = (currentX + padding + bitmap.width).toFloat() / maxSize,
            v2 = (currentY + padding + bitmap.height).toFloat() / maxSize
        )
        
        regions[name] = region
        currentX += paddedWidth
        rowHeight = maxOf(rowHeight, paddedHeight)
        
        bitmapCache.put(name, bitmap)
        return true
    }
    
    fun getRegion(name: String): AtlasRegion? = regions[name]
    
    fun getAtlasBitmap(): Bitmap = atlasBitmapStore
    
    fun clear() {
        regions.clear()
        bitmapCache.evictAll()
        canvas.drawColor(0)
        currentX = 0
        currentY = 0
        rowHeight = 0
    }
}

/**
 * Compose Render-Thread Offloading - Optimization #18
 * 
 * Moves expensive composable computations to background thread
 * using SideEffect + LaunchedEffect pattern.
 * 
 * Target: 60fps UI, no jank during heavy compositions
 */
class ComposeRenderOffloader {
    
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val computeCache = ConcurrentHashMap<String, Any>()
    
    @Composable
    fun <T> offloadComputation(
        key: String,
        compute: () -> T,
        onResult: (T) -> Unit
    ) {
        var result by remember { mutableStateOf<T?>(null) }
        var computing by remember { mutableStateOf(false) }
        
        LaunchedEffect(key) {
            if (computeCache.containsKey(key)) {
                @Suppress("UNCHECKED_CAST")
                onResult(computeCache[key] as T)
                return@LaunchedEffect
            }
            
            computing = true
            backgroundScope.launch(Dispatchers.Default) {
                val computed = compute()
                computeCache[key] = computed as Any
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    result = computed
                    computing = false
                    onResult(computed)
                }
            }
        }
        
        SideEffect {
            if (result != null && !computing) {
                @Suppress("UNCHECKED_CAST")
                onResult(result as T)
            }
        }
    }
    
    fun clearCache() {
        computeCache.clear()
    }
    
    fun invalidateKey(key: String) {
        computeCache.remove(key)
    }
}