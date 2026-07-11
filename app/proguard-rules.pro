# KuroStream App ProGuard/R8 Rules - Ultra-Aggressive Mode
# Enable R8 full mode, obfuscation, and aggressive optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!method/propagation/*
-optimizationpasses 5
-overloadaggressively
-repackageclasses 'k0'
-allowaccessmodification
-mergeinterfacesaggressively

# R8 Full Mode - AOT-like optimizations
-forceinlineall
-optimizationpasses 7
-overloadaggressively
-repackageclasses 'k0'
-allowaccessmodification
-mergeinterfacesaggressively
-useuniqueclassmembernames
-fixdescriptors
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!method/propagation/*,class/inlining/short

# Remove unused code aggressively
-keepattributes *Annotation*, Signature, Exceptions, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable, LocalVariableTable, LocalVariableTypeTable, MethodParameters, Module, ModulePackages, NestMembers, NestHost, Record, PermittedSubclasses
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

# Kotlin Serialization
-keepattributes *Annotation*, Signature, Exceptions, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Transient <fields>;
}
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    public static *** serializer(...);
}
-keepclassmembers class kotlinx.serialization.json.Json {
    public static *** Default;
}

# Domain Entities (serialized via Room/JSON)
-keep,allowobfuscation class com.kurostream.domain.entity.** { *; }
-keep,allowobfuscation class com.kurostream.domain.model.** { *; }
-keep,allowobfuscation class com.kurostream.app.model.** { *; }

# Retrofit / OkHttp - only keep what's needed for reflection
-keep,allowobfuscation class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation class okhttp3.** { *; }
-keep,allowobfuscation class okio.** { *; }
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines - keep only required
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Coil - keep only required
-keep,allowobfuscation class coil.** { *; }

# Dagger Hilt
-keep,allowobfuscation class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Room
-keep,allowobfuscation class com.kurostream.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }

# DataStore
-keep,allowobfuscation class androidx.datastore.** { *; }

# Media3 - only keep what ExoPlayer needs
-keep,allowobfuscation class androidx.media3.** { *; }
-keep class * extends androidx.media3.common.Player { *; }
-keep class * implements androidx.media3.common.Player$Listener { *; }

# LibVLC
-keep,allowobfuscation class org.videolan.libvlc.** { *; }
-keep,allowobfuscation class org.videolan.medialibrary.** { *; }

# MPV
-keep,allowobfuscation class is.xyz.mpv.** { *; }
-keepclasseswithmembernames class * {
    @is.xyz.mpv.* <methods>;
}

# Playback Module (imported via consumer-rules.pro)

# Timber
-dontwarn timber.log.Timber

# Gson / Moshi - only keep what's needed
-keep,allowobfuscation class com.google.gson.** { *; }
-keep,allowobfuscation class com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }

# Kotlinx Serialization
-keep,allowobfuscation class kotlinx.serialization.** { *; }

# Ktor
-keep,allowobfuscation class io.ktor.** { *; }
-keep class io.ktor.client.engine.android.Android { *; }

# Firebase - keep only what's needed for crash-free runtime
-keep,allowobfuscation class com.google.firebase.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }

# AppAuth
-keep,allowobfuscation class net.openid.appauth.** { *; }

# Vosk (Speech Recognition)
-keep,allowobfuscation class com.alphacephei.vosk.** { *; }

# WebRTC
-keep,allowobfuscation class org.webrtc.** { *; }

# NanoHTTPD
-keep,allowobfuscation class org.nanohttpd.** { *; }

# PyTorch Mobile
-keep,allowobfuscation class org.pytorch.** { *; }

# Oboe
-keep,allowobfuscation class com.google.oboe.** { *; }

# TensorFlow Lite
-keep,allowobfuscation class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }

# Keep Native Method Names for JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable Creators
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Enum Values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Hilt-generated components
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.builders.** { *; }

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove logging in release builds
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}