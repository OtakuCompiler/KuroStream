/* MpvRendererJNI.h - Zero-copy libmpv OpenGL renderer JNI bridge */
#ifndef MPV_RENDERER_JNI_H
#define MPV_RENDERER_JNI_H

#include <jni.h>
#include <mpv/render.h>
#include <mpv/render_gl.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <pthread.h>
#include <atomic>
#include <mutex>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeCreate(JNIEnv* env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeDestroy(JNIEnv* env, jobject thiz, jlong handle);

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeInitializeGL(JNIEnv* env, jobject thiz,
    jlong handle, jobject surface, jint width, jint height);

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeRenderFrame(JNIEnv* env, jobject thiz, jlong handle);

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeSetSurfaceSize(JNIEnv* env, jobject thiz,
    jlong handle, jint width, jint height);

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeReportSwap(JNIEnv* env, jobject thiz, jlong handle);

#ifdef __cplusplus
}
#endif

/* C++ Implementation Structure */
struct MpvRenderContext {
    mpv_handle* mpv;
    mpv_render_context* render_ctx;
    ANativeWindow* native_window;
    EGLDisplay egl_display;
    EGLSurface egl_surface;
    EGLContext egl_context;
    EGLConfig egl_config;
    std::atomic<bool> initialized{false};
    std::atomic<bool> frame_pending{false};
    std::mutex render_mutex;
    int surface_width;
    int surface_height;
    int64_t target_fps;
    std::atomic<bool> running{false};
    pthread_t render_thread;
    jobject java_weak_ref;
    JavaVM* jvm;
};

#endif
