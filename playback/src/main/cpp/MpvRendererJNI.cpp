/* MpvRendererJNI.cpp - Zero-copy libmpv OpenGL renderer */
#include "MpvRendererJNI.h"
#include <android/log.h>
#include <chrono>

#define LOG_TAG "MpvRenderer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void* render_thread_loop(void* arg);
static void on_mpv_update(void* ctx);
static void on_mpv_wakeup(void* ctx);

extern "C" JNIEXPORT jlong JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeCreate(JNIEnv* env, jobject thiz) {
    MpvRenderContext* ctx = new MpvRenderContext();

    ctx->mpv = mpv_create();
    if (!ctx->mpv) {
        LOGE("Failed to create mpv handle");
        delete ctx;
        return 0;
    }

    // Enable hardware decoding
    mpv_set_option_string(ctx->mpv, "hwdec", "auto-safe");
    mpv_set_option_string(ctx->mpv, "vo", "libmpv");
    mpv_set_option_string(ctx->mpv, "gpu-context", "android");

    // Initialize mpv
    if (mpv_initialize(ctx->mpv) < 0) {
        LOGE("Failed to initialize mpv");
        mpv_terminate_destroy(ctx->mpv);
        delete ctx;
        return 0;
    }

    // Store weak ref for callbacks
    ctx->java_weak_ref = env->NewWeakGlobalRef(thiz);
    env->GetJavaVM(&ctx->jvm);

    // Set wakeup callback for event processing
    mpv_set_wakeup_callback(ctx->mpv, on_mpv_wakeup, ctx);

    LOGI("MpvRenderContext created successfully");
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeDestroy(JNIEnv* env, jobject thiz, jlong handle) {
    MpvRenderContext* ctx = reinterpret_cast<MpvRenderContext*>(handle);
    if (!ctx) return;

    ctx->running = false;
    if (ctx->render_thread) {
        pthread_join(ctx->render_thread, nullptr);
    }

    if (ctx->render_ctx) {
        mpv_render_context_free(ctx->render_ctx);
    }

    if (ctx->egl_surface != EGL_NO_SURFACE) {
        eglMakeCurrent(ctx->egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroySurface(ctx->egl_display, ctx->egl_surface);
    }

    if (ctx->egl_context != EGL_NO_CONTEXT) {
        eglDestroyContext(ctx->egl_display, ctx->egl_context);
    }

    if (ctx->native_window) {
        ANativeWindow_release(ctx->native_window);
    }

    mpv_terminate_destroy(ctx->mpv);

    if (ctx->java_weak_ref) {
        env->DeleteWeakGlobalRef(ctx->java_weak_ref);
    }

    delete ctx;
    LOGI("MpvRenderContext destroyed");
}

extern "C" JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeInitializeGL(JNIEnv* env, jobject thiz,
    jlong handle, jobject surface, jint width, jint height) {

    MpvRenderContext* ctx = reinterpret_cast<MpvRenderContext*>(handle);
    if (!ctx) return;

    std::lock_guard<std::mutex> lock(ctx->render_mutex);

    ctx->native_window = ANativeWindow_fromSurface(env, surface);
    ctx->surface_width = width;
    ctx->surface_height = height;

    // Initialize EGL
    ctx->egl_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(ctx->egl_display, nullptr, nullptr);

    EGLint attribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_BLUE_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 0,
        EGL_STENCIL_SIZE, 0,
        EGL_NONE
    };

    EGLint num_configs;
    eglChooseConfig(ctx->egl_display, attribs, &ctx->egl_config, 1, &num_configs);

    EGLint context_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };

    ctx->egl_context = eglCreateContext(ctx->egl_display, ctx->egl_config,
        EGL_NO_CONTEXT, context_attribs);

    ctx->egl_surface = eglCreateWindowSurface(ctx->egl_display, ctx->egl_config,
        ctx->native_window, nullptr);

    eglMakeCurrent(ctx->egl_display, ctx->egl_surface, ctx->egl_surface, ctx->egl_context);

    // Initialize mpv render context with OpenGL
    mpv_opengl_init_params gl_init_params = {
        .get_proc_address = [](void* fn_ctx, const char* name) -> void* {
            return eglGetProcAddress(name);
        },
        .get_proc_address_ctx = nullptr,
        .extra_exts = nullptr,
    };

    mpv_render_param params[] = {
        {MPV_RENDER_PARAM_API_TYPE, const_cast<char*>(MPV_RENDER_API_TYPE_OPENGL)},
        {MPV_RENDER_PARAM_OPENGL_INIT_PARAMS, &gl_init_params},
        {MPV_RENDER_PARAM_INVALID, nullptr}
    };

    int err = mpv_render_context_create(&ctx->render_ctx, ctx->mpv, params);
    if (err < 0) {
        LOGE("Failed to create mpv render context: %d", err);
        return;
    }

    mpv_render_context_set_update_callback(ctx->render_ctx, on_mpv_update, ctx);

    ctx->initialized = true;
    ctx->running = true;
    pthread_create(&ctx->render_thread, nullptr, render_thread_loop, ctx);

    LOGI("GL initialized: %dx%d", width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeRenderFrame(JNIEnv* env, jobject thiz, jlong handle) {
    MpvRenderContext* ctx = reinterpret_cast<MpvRenderContext*>(handle);
    if (!ctx || !ctx->initialized) return;

    std::lock_guard<std::mutex> lock(ctx->render_mutex);

    eglMakeCurrent(ctx->egl_display, ctx->egl_surface, ctx->egl_surface, ctx->egl_context);

    int fbo_width, fbo_height;
    eglQuerySurface(ctx->egl_display, ctx->egl_surface, EGL_WIDTH, &fbo_width);
    eglQuerySurface(ctx->egl_display, ctx->egl_surface, EGL_HEIGHT, &fbo_height);

    mpv_opengl_fbo mpv_fbo = {
        .fbo = 0,
        .w = fbo_width,
        .h = fbo_height,
        .internal_format = 0,
    };

    int flip_y = 1;

    mpv_render_param params[] = {
        {MPV_RENDER_PARAM_OPENGL_FBO, &mpv_fbo},
        {MPV_RENDER_PARAM_FLIP_Y, &flip_y},
        {MPV_RENDER_PARAM_INVALID, nullptr}
    };

    mpv_render_context_render(ctx->render_ctx, params);

    eglSwapBuffers(ctx->egl_display, ctx->egl_surface);
    ctx->frame_pending = false;
}

extern "C" JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeSetSurfaceSize(JNIEnv* env, jobject thiz,
    jlong handle, jint width, jint height) {

    MpvRenderContext* ctx = reinterpret_cast<MpvRenderContext*>(handle);
    if (!ctx) return;

    std::lock_guard<std::mutex> lock(ctx->render_mutex);
    ctx->surface_width = width;
    ctx->surface_height = height;

    // Notify mpv of size change
    if (ctx->render_ctx) {
        // mpv handles surface resize automatically via FBO dimensions
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_render_MpvRenderer_nativeReportSwap(JNIEnv* env, jobject thiz, jlong handle) {
    MpvRenderContext* ctx = reinterpret_cast<MpvRenderContext*>(handle);
    if (!ctx || !ctx->render_ctx) return;
    mpv_render_context_report_swap(ctx->render_ctx);
}

static void* render_thread_loop(void* arg) {
    MpvRenderContext* ctx = static_cast<MpvRenderContext*>(arg);

    while (ctx->running) {
        if (ctx->frame_pending) {
            JNIEnv* env;
            jint attach_result = ctx->jvm->AttachCurrentThread(&env, nullptr);
            if (attach_result == JNI_OK) {
                jobject weak_ref = env->NewLocalRef(ctx->java_weak_ref);
                if (weak_ref) {
                    jclass clazz = env->GetObjectClass(weak_ref);
                    jmethodID render_method = env->GetMethodID(clazz, "onFrameAvailable", "()V");
                    env->CallVoidMethod(weak_ref, render_method);
                    env->DeleteLocalRef(clazz);
                    env->DeleteLocalRef(weak_ref);
                }
                ctx->jvm->DetachCurrentThread();
            }
        }

        // Frame pacing: target 60fps or content fps
        struct timespec ts;
        ts.tv_sec = 0;
        ts.tv_nsec = 16666666L; // ~60fps
        nanosleep(&ts, nullptr);
    }

    return nullptr;
}

static void on_mpv_update(void* ctx_ptr) {
    MpvRenderContext* ctx = static_cast<MpvRenderContext*>(ctx_ptr);
    ctx->frame_pending = true;
}

static void on_mpv_wakeup(void* ctx_ptr) {
    MpvRenderContext* ctx = static_cast<MpvRenderContext*>(ctx_ptr);
    // Process mpv events on main thread or dedicated event thread
}
