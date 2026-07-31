# ProGuard rules for KuroStream
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Hilt
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

# Keep Coil
-dontwarn coil.decode.VideoFrameDecoder

# Keep Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep VLC
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.libvlc.**

# Keep MPV
-keep class dev.jdtech.mpv.** { *; }
-dontwarn dev.jdtech.mpv.**

# Keep jlibtorrent
-keep class com.frostwire.jlibtorrent.** { *; }
-dontwarn com.frostwire.jlibtorrent.**

# Keep Timber
-assumenosideeffects class timber.log.Timber { *; }

# Jackson / java.beans desugaring (java.beans not fully present on Android)
-dontwarn java.beans.**
-dontnote java.beans.ConstructorProperties

# Keep TV Leanback
-keep class androidx.leanback.** { *; }
-dontwarn androidx.leanback.**

# Keep Kotlin Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep TV Material3
-keep class androidx.tv.material3.** { *; }

# Keep Media3 PlayerView
-keep class androidx.media3.ui.PlayerView { *; }
-keepclassmembers class androidx.media3.ui.PlayerView { *; }

# Keep ExoPlayer tunneling APIs
-keepclassmembers class androidx.media3.exoplayer.ExoPlayer {
    public void setTunnelingEnabled(boolean);
}

# General
-keep public class com.kurostream.app.AnimeStreamTvApplication { *; }
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
