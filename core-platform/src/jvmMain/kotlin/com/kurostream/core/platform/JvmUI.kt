package com.kurostream.core.platform

class JvmUI : PlatformUI {
    override fun openUrl(url: String) {
        throw UnsupportedOperationException("JvmUI not implemented for JVM.")
    }
    override fun shareText(text: String, subject: String?) {
        throw UnsupportedOperationException("JvmUI not implemented for JVM.")
    }
    override fun showToast(message: String) {
        throw UnsupportedOperationException("JvmUI not implemented for JVM.")
    }
    override fun showError(message: String) {
        throw UnsupportedOperationException("JvmUI not implemented for JVM.")
    }
    override fun vibrate(pattern: LongArray?) {
        throw UnsupportedOperationException("JvmUI not implemented for JVM.")
    }
    override fun requestFocus(focusable: Focusable) {
        throw UnsupportedOperationException("JvmUI not implemented for JVM.")
    }
    override fun clearFocus() {
        throw UnsupportedOperationException("JvmUI not implemented for JVM.")
    }
    override fun setDisplayRefreshRate(rate: Float) {
        throw UnsupportedOperationException("JvmUI not implemented for JVM.")
    }
    override fun getDisplayRefreshRate(): Float = 60f
    override fun setImmersiveMode(enabled: Boolean) {
        throw UnsupportedOperationException("JvmUI not implemented for JVM.")
    }
    override fun isImmersiveMode(): Boolean = false
}
