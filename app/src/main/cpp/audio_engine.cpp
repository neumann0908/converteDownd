#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <string>
#include <vector>

#define TAG "AudioEngineNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

class MyAudioStreamCallback : public oboe::AudioStreamCallback {
public:
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override {
        // Renderizado en tiempo real de las muestras PCM
        auto *outputBuffer = static_cast<float *>(audioData);
        for (int i = 0; i < numFrames * oboeStream->getChannelCount(); ++i) {
            outputBuffer[i] = 0.0f; // Silencio por defecto hasta reproducir el sample activo
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
        LOGE("Error al abrir el stream de Oboe: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    result = g_stream->start();
    if (result != oboe::Result::OK) {
        LOGE("Error al iniciar el stream de Oboe: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    LOGI("¡Motor Oboe iniciado con éxito a ultra baja latencia!");
    return JNI_TRUE;
}

/**
 * Función JNI de integración quirúrgica para cargar y validar assets de audio de forma segura.
 * Evita cierres forzosos (crashes) validando punteros nulos y concatenando la ruta de kits/.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativeLoadSample(
        JNIEnv *env, jobject thiz, jobject assetManagerObj, jstring filenameStr) {

    if (assetManagerObj == nullptr || filenameStr == nullptr) {
        LOGE("nativeLoadSample: Error crítico - AssetManager o filename son nulos.");
        return JNI_FALSE;
    }

    // Obtener el puente nativo del AssetManager de Android
    AAssetManager *assetManager = AAssetManager_fromJava(env, assetManagerObj);
    if (assetManager == nullptr) {
        LOGE("nativeLoadSample: No se pudo extraer AAssetManager desde Java.");
        return JNI_FALSE;
    }

    const char *filename = env->GetStringUTFChars(filenameStr, nullptr);
    if (filename == nullptr) {
        LOGE("nativeLoadSample: Error al convertir el nombre de archivo a UTF.");
        return JNI_FALSE;
    }

    // Construir la ruta relativa exacta combinando el directorio raíz de kits y el archivo seleccionado
    std::string fullPath = std::string("kits/") + filename;
    LOGI("Intentando abrir asset de audio seguro: %s", fullPath.c_str());

    // Abrir el asset en modo buffer de solo lectura
    AAsset *asset = AAssetManager_open(assetManager, fullPath.c_str(), AASSET_MODE_BUFFER);
    if (asset == nullptr) {
        LOGE("nativeLoadSample: Fallo al abrir el asset en la ruta: %s", fullPath.c_str());
        env->ReleaseStringUTFChars(filenameStr, filename);
        return JNI_FALSE; // Corregido: Sin acentos graves
    }

    off_t assetSize = AAsset_getLength(asset);
    const void *assetBuffer = AAsset_getBuffer(asset);

    if (assetBuffer == nullptr) {
        LOGE("nativeLoadSample: El buffer en memoria para el asset vino vacío: %s", fullPath.c_str());
        AAsset_close(asset);
        env->ReleaseStringUTFChars(filenameStr, filename);
        return JNI_FALSE;
    }

    LOGI("¡Asset cargado exitosamente en C++! Tamaño del archivo: %ld bytes", (long)assetSize);

    // TODO: Aquí se enlazará el buffer PCM descampado con el mezclador de Oboe

    // Liberación segura de recursos nativos
    AAsset_close(asset);
    env->ReleaseStringUTFChars(filenameStr, filename);
    return JNI_TRUE;
}
