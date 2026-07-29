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

package com.kurostream.players.advanced.render

import android.opengl.GLSurfaceView
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.Keep
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Zero-copy libmpv OpenGL renderer.
 * Directly renders video frames to a Surface without intermediate copies.
 */
@Keep
class MpvRenderer : GLSurfaceView.Renderer, SurfaceHolder.Callback {

    private var nativeHandle: Long = 0
    private val isInitialized = AtomicBoolean(false)
    private val pendingCommands = mutableListOf<() -> Unit>()
    private val lock = Object()

    init {
        System.loadLibrary("mpvrenderer")
        nativeHandle = nativeCreate()
    }

    //region Native Methods
    @Keep
    private external fun nativeCreate(): Long
    @Keep
    private external fun nativeDestroy(handle: Long)
    @Keep
    private external fun nativeInitializeGL(handle: Long, surface: Surface, width: Int, height: Int)
    @Keep
    private external fun nativeRenderFrame(handle: Long)
    @Keep
    private external fun nativeSetSurfaceSize(handle: Long, width: Int, height: Int)
    @Keep
    private external fun nativeReportSwap(handle: Long)
    //endregion

    //region GLSurfaceView.Renderer
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // EGL context created by GLSurfaceView, we bridge to mpv
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        synchronized(lock) {
            nativeSetSurfaceSize(nativeHandle, width, height)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        if (!isInitialized.get()) return
        synchronized(lock) {
            nativeRenderFrame(nativeHandle)
            nativeReportSwap(nativeHandle)
        }
    }
    //endregion

    //region SurfaceHolder.Callback
    override fun surfaceCreated(holder: SurfaceHolder) {
        val surface = holder.surface
        val width = holder.surfaceFrame.width()
        val height = holder.surfaceFrame.height()

        synchronized(lock) {
            nativeInitializeGL(nativeHandle, surface, width, height)
            isInitialized.set(true)

            // Execute pending commands
            pendingCommands.forEach { it() }
            pendingCommands.clear()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        synchronized(lock) {
            nativeSetSurfaceSize(nativeHandle, width, height)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isInitialized.set(false)
        // Cleanup handled in nativeDestroy
    }
    //endregion

    /**
     * Called from native render thread when a new frame is available.
     */
    @Keep
    fun onFrameAvailable() {
        // Trigger GLSurfaceView redraw
        // This is called from native thread, post to main thread if needed
    }

    fun loadFile(path: String) {
        val command = {
            // Send loadfile command to mpv via native
            nativeSendCommand(nativeHandle, "loadfile", path)
        }

        if (isInitialized.get()) {
            command()
        } else {
            synchronized(lock) {
                pendingCommands.add(command)
            }
        }
    }

    @Keep
    private external fun nativeSendCommand(handle: Long, command: String, arg: String)

    fun destroy() {
        isInitialized.set(false)
        nativeDestroy(nativeHandle)
        nativeHandle = 0
    }
}
