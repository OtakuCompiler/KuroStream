# KuroStream Playback Module ProGuard Rules

# MPV (Native)
-keep class is.xyz.mpv.** { *; }
-keepclasseswithmembernames class * {
    @is.xyz.mpv.* <methods>;
}

# VLC (Native)
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.medialibrary.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Playback Core Interface
-keep class com.kurostream.playback.core.** { *; }
-keepclassmembers class * implements com.kurostream.playback.core.PlayerInterface { *; }

# MPV Player Implementation
-keep class com.kurostream.playback.mpv.** { *; }

# VLC Player Implementation
-keep class com.kurostream.playback.vlc.** { *; }

# Media3 Player Implementation
-keep class com.kurostream.playback.media3.** { *; }

# Backend Selector
-keep class com.kurostream.playback.selector.** { *; }

# Advanced Features
-keep class com.kurostream.playback.advanced.render.** { *; }
-keep class com.kurostream.playback.advanced.audio.** { *; }
-keep class com.kurostream.playback.advanced.ai.** { *; }
-keep class com.kurostream.playback.advanced.captions.** { *; }
-keep class com.kurostream.playback.advanced.drm.** { *; }
-keep class com.kurostream.playback.advanced.watchparty.** { *; }
-keep class com.kurostream.playback.advanced.community.** { *; }
-keep class com.kurostream.playback.advanced.marketplace.** { *; }
-keep class com.kurostream.playback.advanced.settings.** { *; }
-keep class com.kurostream.playback.advanced.extensions.** { *; }

# Diagnostics
-keep class com.kurostream.playback.diagnostics.** { *; }

# Buffering
-keep class com.kurostream.playback.buffering.** { *; }

# Disk Buffer (NEW)
-keep class com.kurostream.playback.buffer.** { *; }

# Display / Refresh Rate
-keep class com.kurostream.playback.display.** { *; }

# Oboe (Native Audio)
-keep class com.google.oboe.** { *; }

# PyTorch Mobile
-keep class org.pytorch.** { *; }
-keep class org.pytorch.jni.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }

# Keep JNI Methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Native Library Loading
-keep class * {
    static void System.loadLibrary(java.lang.String);
    static void System.load(java.lang.String);
}