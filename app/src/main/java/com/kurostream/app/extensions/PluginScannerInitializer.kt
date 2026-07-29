package com.kurostream.app.extensions

import android.content.Context
import androidx.startup.Initializer

class PluginScannerInitializer : Initializer<Unit> {
    override fun create(context: Context) {}
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
