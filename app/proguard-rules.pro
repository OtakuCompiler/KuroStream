# KuroStream ProGuard Rules - GPL-3.0

# === MISSING CLASS SUPPRESSION (R8 full mode) ===
# These exact rules are required by R8's missing class check.
# Do NOT remove or reorder these.
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options$GpuBackend
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options

# === SERIALIZATION ===
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }

# === ROOM ===
-keep class com.kurostream.data.local.entity.** { *; }
-keepclassmembers class com.kurostream.data.local.entity.** { *; }

# === RETROFIT DTOs ===
-keep class com.kurostream.data.remote.dto.** { *; }
-keepclassmembers class com.kurostream.data.remote.dto.** { <fields>; }

# === HILT ===
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keepclassmembers @dagger.hilt.android.HiltAndroidApp class * { *; }

# === NAVIGATION ===
-keep @kotlinx.serialization.Serializable class com.kurostream.app.navigation.** { *; }

# === DOMAIN MODELS ===
-keep class com.kurostream.domain.entity.** { *; }
-keep class com.kurostream.domain.network.** { *; }
-keep class com.kurostream.domain.metadata.** { *; }

# === LOGGING ===
-dontwarn timber.log.Timber

# === MEDIA ===
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class org.videolan.libvlc.** { *; }
-keep class is.xyz.mpv.** { *; }

# === ANILIST ===
-keep class com.kurostream.data.anilist.** { *; }

# === ML ===
-keep class org.tensorflow.lite.Interpreter { *; }
-keep class org.tensorflow.lite.InterpreterFactory { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class com.microsoft.onnxruntime.** { *; }

# === PLAYBACK ===
-keep class com.kurostream.playback.** { *; }
-keep class com.kurostream.players.** { *; }

# === UI ===
-keep class com.kurostream.app.ui.arctic.** { *; }

# === COMMON ===
-keep class com.kurostream.common.** { *; }

# === NETWORKING ===
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# === IMAGE LOADING ===
-keep class coil.** { *; }

# === FIREBASE ===
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }

# === JSOUP ===
-keep class org.jsoup.** { *; }

# === COMPOSE ===
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers class * {
    @androidx.compose.runtime.Immutable <fields>;
    @androidx.compose.runtime.Stable <fields>;
}

# === ANDROIDX ===
-keep class androidx.profileinstaller.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.hilt.work.HiltWorker
-keep class androidx.security.crypto.** { *; }

# === APP SPECIFIC ===
-keep class com.kurostream.app.notification.NotificationChannels { *; }
-keep class com.kurostream.app.deeplink.DeepLinkHandler { *; }
-keep class com.kurostream.app.analytics.CrashReporter { *; }

# === PLAY INTEGRITY ===
-keep class com.google.android.play.core.integrity.** { *; }
-keep class com.google.android.gms.common.ConnectionResult { *; }
-keep class com.google.android.gms.cast.** { *; }

# === SQLCIPHER ===
-keep class net.sqlcipher.** { *; }

# === WEBRTC ===
-keep class org.webrtc.** { *; }

# === NATIVE ===
-keepclasseswithmembernames class * { native <methods>; }
-keep class * { static <fields>; }

# === OPTIONAL / PLATFORM-SPECIFIC ===
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.xml.bind.**
-dontwarn org.xmlpull.**

# === STRIP DEBUG LOGGING ===
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
