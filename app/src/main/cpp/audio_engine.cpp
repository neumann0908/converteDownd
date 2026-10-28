#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>

#define TAG "AudioEngineNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

class MyAudioStreamCallback : public oboe::AudioStreamCallback {
public:
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override {
        // Aquí generaremos o mezclaremos las muestras PCM de nuestros instrumentos en tiempo real
        auto *outputBuffer = static_cast<float *>(audioData);
        for (int i = 0; i < numFrames * oboeStream->getChannelCount(); ++i) {
            outputBuffer[i] = 0.0f; // Silencio por defecto hasta reproducir el sample
        }
        return oboe::DataCallbackResult::Continue;
    }
};

static oboe::ManagedStream g_stream;
static MyAudioStreamCallback g_callback;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativeStart(JNIEnv *env, jobject thiz) {
    oboe::AudioStreamBuilder builder;
    builder.setCallback(&g_callback);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(oboe::ChannelCount::Stereo);

    oboe::Result result = builder.openManagedStream(g_stream);
    if (result != oboe::Result::OK) {
        LOGI("Error al abrir el stream de Oboe: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    result = g_stream->start();
    if (result != oboe::Result::OK) {
        LOGI("Error al iniciar el stream de Oboe: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    LOGI("¡Motor Oboe iniciado con éxito a ultra baja latencia!");
    return JNI_TRUE;
}
