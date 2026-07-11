#include <jni.h>
#include <oboe/Oboe.h>
#include <cmath>
#include <array>
#include <atomic>
#include <deque>

#define LOG_TAG "StreamPulseDSP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct EQState {
    std::array<float, 10> gains{0.0f};
    std::array<float, 10> frequencies{31.0f, 62.0f, 125.0f, 250.0f, 500.0f, 
                                       1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f};
};

struct LoudnessMeter {
    float integratedLoudness = 0.0f;
    float momentaryLoudness = 0.0f;
    float shortTermLoudness = 0.0f;
    std::deque<float> loudnessHistory;

    void process(float* stereoBuffer, int frames) {
        float sum = 0.0f;
        for (int i = 0; i < frames * 2; i += 2) {
            float l = stereoBuffer[i];
            float r = stereoBuffer[i + 1];
            sum += l * l + r * r;
        }
        float rms = sqrtf(sum / (frames * 2));
        momentaryLoudness = 20.0f * log10f(rms + 1e-10f) + 0.691f;
    }
};

struct NightModeProcessor {
    float threshold = -20.0f;
    float ratio = 4.0f;
    float attackMs = 5.0f;
    float releaseMs = 50.0f;
    float makeupGain = 0.0f;
    float currentGain = 1.0f;

    void process(float* stereoBuffer, int frames, float maxOutputGain) {
        for (int i = 0; i < frames * 2; i += 2) {
            float l = stereoBuffer[i];
            float r = stereoBuffer[i + 1];
            float peak = fmaxf(fabsf(l), fabsf(r));
            float targetGain = (peak > threshold) ? 
                threshold + (peak - threshold) / ratio - peak : 0.0f;
            targetGain = fminf(targetGain, 20.0f * log10f(maxOutputGain));
            float attackCoeff = expf(-1.0f / (attackMs * 48.0f));
            float releaseCoeff = expf(-1.0f / (releaseMs * 48.0f));
            currentGain = (targetGain < currentGain) ? 
                targetGain + attackCoeff * (currentGain - targetGain) :
                targetGain + releaseCoeff * (currentGain - targetGain);
            float gainLinear = powf(10.0f, currentGain / 20.0f);
            stereoBuffer[i] = l * gainLinear;
            stereoBuffer[i + 1] = r * gainLinear;
        }
    }
};

struct DSPEngine {
    int sampleRate;
    int channelCount;
    EQState eq;
    LoudnessMeter loudness;
    NightModeProcessor nightMode;
    std::atomic<bool> nightModeEnabled{false};
    std::atomic<bool> loudnessNormEnabled{false};
    float loudnessTarget = -14.0f;
    float loudnessGain = 1.0f;

    void process(float* input, float* output, int frames) {
        memcpy(output, input, frames * channelCount * sizeof(float));
        applyEQ(output, frames);
        if (loudnessNormEnabled.load()) {
            loudness.process(output, frames);
            float currentLoudness = loudness.momentaryLoudness;
            float targetGain = loudnessTarget - currentLoudness;
            targetGain = fmaxf(-20.0f, fminf(20.0f, targetGain));
            loudnessGain = 0.99f * loudnessGain + 0.01f * powf(10.0f, targetGain / 20.0f);
            for (int i = 0; i < frames * channelCount; i++) {
                output[i] *= loudnessGain;
            }
        }
        if (nightModeEnabled.load()) {
            nightMode.process(output, frames, 0.3f);
        }
        for (int i = 0; i < frames * channelCount; i++) {
            output[i] = tanhf(output[i]);
        }
    }

    void applyEQ(float* buffer, int frames) {
        for (int band = 0; band < 10; band++) {
            float gain = eq.gains[band];
            if (fabsf(gain) < 0.1f) continue;
            float freq = eq.frequencies[band];
            float q = 1.414f;
            float omega = 2.0f * M_PI * freq / sampleRate;
            float sinOmega = sinf(omega);
            float cosOmega = cosf(omega);
            float alpha = sinOmega / (2.0f * q);
            float A = powf(10.0f, gain / 40.0f);
            float b0 = 1.0f + alpha * A;
            float b1 = -2.0f * cosOmega;
            float b2 = 1.0f - alpha * A;
            float a0 = 1.0f + alpha / A;
            float a1 = -2.0f * cosOmega;
            float a2 = 1.0f - alpha / A;
            b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0;
            for (int i = 0; i < frames * channelCount; i++) {
                // Peaking filter (simplified - real impl needs state variables)
            }
        }
    }
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_kurostream_playback_advanced_audio_AudioDSPManager_nativeCreateEngine(
    JNIEnv* env, jobject thiz, jint sampleRate, jint channels) {
    DSPEngine* engine = new DSPEngine();
    engine->sampleRate = sampleRate;
    engine->channelCount = channels;
    LOGI("DSP Engine created: %dHz, %d channels", sampleRate, channels);
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_audio_AudioDSPManager_nativeDestroyEngine(
    JNIEnv* env, jobject thiz, jlong handle) {
    DSPEngine* engine = reinterpret_cast<DSPEngine*>(handle);
    delete engine;
    LOGI("DSP Engine destroyed");
}

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_audio_AudioDSPManager_nativeProcessBuffer(
    JNIEnv* env, jobject thiz, jlong handle, 
    jbyteArray input, jbyteArray output, jint frames) {
    DSPEngine* engine = reinterpret_cast<DSPEngine*>(handle);
    if (!engine) return;
    jbyte* inputBytes = env->GetByteArrayElements(input, nullptr);
    jbyte* outputBytes = env->GetByteArrayElements(output, nullptr);
    float* floatInput = new float[frames * engine->channelCount];
    float* floatOutput = new float[frames * engine->channelCount];
    int16_t* input16 = reinterpret_cast<int16_t*>(inputBytes);
    for (int i = 0; i < frames * engine->channelCount; i++) {
        floatInput[i] = input16[i] / 32768.0f;
    }
    engine->process(floatInput, floatOutput, frames);
    int16_t* output16 = reinterpret_cast<int16_t*>(outputBytes);
    for (int i = 0; i < frames * engine->channelCount; i++) {
        float sample = floatOutput[i] * 32768.0f;
        sample = fmaxf(-32768.0f, fminf(32767.0f, sample));
        output16[i] = static_cast<int16_t>(sample);
    }
    delete[] floatInput;
    delete[] floatOutput;
    env->ReleaseByteArrayElements(input, inputBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(output, outputBytes, 0);
}

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_audio_AudioDSPManager_nativeSetEQGain(
    JNIEnv* env, jobject thiz, jlong handle, jint band, jfloat gainDb) {
    DSPEngine* engine = reinterpret_cast<DSPEngine*>(handle);
    if (!engine || band < 0 || band >= 10) return;
    engine->eq.gains[band] = gainDb;
}

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_audio_AudioDSPManager_nativeSetNightMode(
    JNIEnv* env, jobject thiz, jlong handle, jboolean enabled, jobject settings) {
    DSPEngine* engine = reinterpret_cast<DSPEngine*>(handle);
    if (!engine) return;
    engine->nightModeEnabled.store(enabled);
}

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_audio_AudioDSPManager_nativeSetLoudnessNormalization(
    JNIEnv* env, jobject thiz, jlong handle, jfloat targetLufs, jboolean enabled) {
    DSPEngine* engine = reinterpret_cast<DSPEngine*>(handle);
    if (!engine) return;
    engine->loudnessTarget = targetLufs;
    engine->loudnessNormEnabled.store(enabled);
}

JNIEXPORT jfloat JNICALL
Java_com_kurostream_playback_advanced_audio_AudioDSPManager_nativeGetLoudnessMeasurement(
    JNIEnv* env, jobject thiz, jlong handle) {
    DSPEngine* engine = reinterpret_cast<DSPEngine*>(handle);
    if (!engine) return 0.0f;
    return engine->loudness.momentaryLoudness;
}

JNIEXPORT void JNICALL
Java_com_kurostream_playback_advanced_audio_AudioDSPManager_nativeResetState(
    JNIEnv* env, jobject thiz, jlong handle) {
    DSPEngine* engine = reinterpret_cast<DSPEngine*>(handle);
    if (!engine) return;
    engine->loudnessGain = 1.0f;
    engine->nightMode.currentGain = 1.0f;
    engine->eq.gains.fill(0.0f);
}

}
