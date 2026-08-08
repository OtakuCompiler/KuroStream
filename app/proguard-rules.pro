# KuroStream ProGuard Rules - GPL-3.0
-ignorewarnings

# Keep Serializable classes for kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }

# Keep Room entities
-keep class com.kurostream.data.local.entity.** { *; }
-keepclassmembers class com.kurostream.data.local.entity.** { *; }

# Keep Retrofit models
-keep class com.kurostream.data.remote.dto.** { *; }
-keepclassmembers class com.kurostream.data.remote.dto.** { <fields>; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keepclassmembers @dagger.hilt.android.HiltAndroidApp class * { *; }

# Keep Navigation Routes
-keep @kotlinx.serialization.Serializable class com.kurostream.app.navigation.** { *; }

# Keep Domain models
-keep class com.kurostream.domain.entity.** { *; }
-keep class com.kurostream.domain.network.** { *; }
-keep class com.kurostream.domain.metadata.** { *; }

# Keep Timber
-dontwarn timber.log.Timber

# Keep ExoPlayer/Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep VLC/MPV JNI
-keep class org.videolan.libvlc.** { *; }
-keep class is.xyz.mpv.** { *; }

# Keep AniList models
-keep class com.kurostream.data.anilist.** { *; }

# Keep ML models (only JNI-accessed classes, not GPU delegate internals)
-keep class org.tensorflow.lite.Interpreter { *; }
-keep class org.tensorflow.lite.InterpreterFactory { *; }
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.lite.gpu.**
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options$GpuBackend
-keep class com.microsoft.onnxruntime.** { *; }

# Keep Playback engines
-keep class com.kurostream.playback.** { *; }
-keep class com.kurostream.players.** { *; }

# Keep Arctic Fuse UI
-keep class com.kurostream.app.ui.arctic.** { *; }

# Keep common utilities
-keep class com.kurostream.common.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coil
-keep class coil.** { *; }

# Firebase (if used)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# JSoup
-keep class org.jsoup.** { *; }

# Keep Compose @Immutable / @Stable
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers class * {
    @androidx.compose.runtime.Immutable <fields>;
    @androidx.compose.runtime.Stable <fields>;
}

# Keep BaselineProfile
-keep class androidx.profileinstaller.** { *; }

# Keep WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Keep Hilt Worker
-keep class * extends androidx.hilt.work.HiltWorker

# Keep Notification channels
-keep class com.kurostream.app.notification.NotificationChannels { *; }

# Keep Deep link route
-keep class com.kurostream.app.deeplink.DeepLinkHandler { *; }

# Keep Crash reporter
-keep class com.kurostream.app.analytics.CrashReporter { *; }

# Play Integrity
-keep class com.google.android.play.core.integrity.** { *; }
-keep class com.google.android.gms.common.ConnectionResult { *; }

# Firebase
-keep class com.google.android.gms.** { *; }

# SQLCipher
-keep class net.sqlcipher.** { *; }

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Cast
-keep class com.google.android.gms.cast.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }

# 16KB page size
-keepclasseswithmembernames class * { native <methods>; }
-keep class * { static { System.loadLibrary(*); } }

# Java SE classes missing on Android
-dontwarn java.beans.**
-dontwarn java.lang.**$Lambda**
-dontwarn javax.xml.bind.**
-dontwarn org.xmlpull.**

# Crypto/TLS providers missing on Android
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Remove all debug logging in release
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
