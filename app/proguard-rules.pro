# KuroStream App ProGuard/R8 Rules

# Kotlin Serialization
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Transient <fields>;
}

# Domain Entities (serialized via Room/JSON)
-keep class com.kurostream.domain.entity.** { *; }
-keep class com.kurostream.domain.model.** { *; }
-keep class com.kurostream.tv.model.** { *; }

# Retrofit / OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**
-keep class okio.** { *; }
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines
-dontwarn kotlinx.coroutines.**

# Coil
-dontwarn coil.**

# Dagger Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }

# Room
-keep class com.kurostream.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Media3
-keep class androidx.media3.** { *; }

# LibVLC
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.medialibrary.** { *; }

# MPV
-keep class is.xyz.mpv.** { *; }
-keepclasseswithmembernames class * {
    @is.xyz.mpv.* <methods>;
}

# Playback Module (imported via consumer-rules.pro)
# This is included via playback/consumer-rules.pro

# Timber
-dontwarn timber.log.Timber

# Gson / Moshi
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Firebase
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

# AppAuth
-dontwarn net.openid.appauth.**
-keep class net.openid.appauth.** { *; }

# Vosk (Speech Recognition)
-dontwarn com.alphacephei.vosk.**
-keep class com.alphacephei.vosk.** { *; }

# WebRTC
-dontwarn org.webrtc.**
-keep class org.webrtc.** { *; }

# NanoHTTPD
-dontwarn org.nanohttpd.**
-keep class org.nanohttpd.** { *; }

# PyTorch Mobile
-dontwarn org.pytorch.**
-keep class org.pytorch.** { *; }

# Oboe
-dontwarn com.google.oboe.**
-keep class com.google.oboe.** { *; }

# TensorFlow Lite
-dontwarn org.tensorflow.lite.**
-keep class org.tensorflow.lite.** { *; }

# Keep Native Method Names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable Creators
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Enum Values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Setters/Getters for Data Classes
-keepclassmembers class * {
    public <fields>;
    public <methods>;
}