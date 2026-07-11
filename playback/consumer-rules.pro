# KuroStream Playback Consumer ProGuard Rules
# These rules are applied to consumers of the playback module

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

# Native Libraries (JNI)
-keep class com.kurostream.playback.advanced.render.** { *; }
-keep class com.kurostream.playback.advanced.audio.** { *; }
-keep class com.kurostream.playback.advanced.ai.** { *; }
-keep class com.kurostream.playback.advanced.captions.** { *; }

# Oboe (Native Audio)
-keep class com.google.oboe.** { *; }

# PyTorch Mobile
-keep class org.pytorch.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }