package com.kurostream.desktop.data

import androidx.compose.runtime.Stable
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Stable
class DesktopSettings private constructor(
    private val file: File,
    private var state: State,
) {
    @Serializable
    data class State(
        val theme: String = "dark",
        val playbackBackend: String = "auto", // auto | vlc | mpv
        val preferredQuality: String = "1080p",
        val dolbyAtmosPassthrough: Boolean = true,
        val frameInterpolation: Boolean = false,
        val aiUpscaling: Boolean = false,
        val hardwareDecoder: Boolean = true,
        val crossDeviceSync: Boolean = true,
        val analyticsEnabled: Boolean = false,
    )

    fun snapshot(): State = state
    fun update(transform: (State) -> State) {
        state = transform(state)
        persist()
    }
    private fun persist() {
        runCatching {
            file.parentFile.mkdirs()
            file.writeText(Json.encodeToString(state))
        }
    }

    companion object {
        fun load(file: File): DesktopSettings {
            val state = if (file.exists()) {
                runCatching { Json.decodeFromString(State.serializer(), file.readText()) }
                    .getOrElse { State() }
            } else State()
            return DesktopSettings(file, state)
        }
    }
}
