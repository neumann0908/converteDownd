#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstdio> // Necesario para fopen, fread, fclose

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
 * Función JNI de integración quirúrgica para cargar samples de audio desde la ruta absoluta 
 * del almacenamiento interno (filesDir/kits/).
 * Utiliza E/S estándar de C (fopen) para lectura directa y eficiente en memoria.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativeLoadSampleByPath(
        JNIEnv *env, jobject thiz, jstring filePathStr, jint padIndex) {

    if (filePathStr == nullptr) {
        LOGE("nativeLoadSampleByPath: Error crítico - La ruta del archivo es nula.");
        return JNI_FALSE;
    }

    const char *filePath = env->GetStringUTFChars(filePathStr, nullptr);
    if (filePath == nullptr) {
        LOGE("nativeLoadSampleByPath: Error al convertir la ruta de archivo a UTF.");
        return JNI_FALSE;
    }

    LOGI("Intentando abrir sample de audio interno en ruta: %s para el pad: %d", filePath, padIndex);

    // Abrir el archivo directamente desde el almacenamiento interno en modo lectura binaria ("rb")
    FILE *file = fopen(filePath, "rb");
    if (file == nullptr) {
        LOGE("nativeLoadSampleByPath: Fallo al abrir el archivo en la ruta absoluta: %s", filePath);
        env->ReleaseStringUTFChars(filePathStr, filePath);
        return JNI_FALSE;
    }

    // Obtener el tamaño total del archivo en bytes
    fseek(file, 0, SEEK_END);
    long fileSize = ftell(file);
    fseek(file, 0, SEEK_SET);

    if (fileSize <= 0) {
        LOGE("nativeLoadSampleByPath: El archivo está vacío o no se pudo medir su tamaño: %s", filePath);
        fclose(file);
        env->ReleaseStringUTFChars(filePathStr, filePath);
        return JNI_FALSE;
    }

    // Reservar buffer en memoria nativa y leer los datos binarios del PCM/WAV
    std::vector<uint8_t> fileBuffer(fileSize);
    size_t bytesRead = fread(fileBuffer.data(), 1, fileSize, file);
    
    fclose(file); // Cerramos el descriptor de archivo de inmediato tras la lectura

    if (bytesRead != static_cast<size_t>(fileSize)) {
        LOGE("nativeLoadSampleByPath: Error de lectura, se leyeron %zu de %ld bytes.", bytesRead, fileSize);
        env->ReleaseStringUTFChars(filePathStr, filePath);
        return JNI_FALSE;
    }

    LOGI("¡Sample cargado exitosamente en C++ desde almacenamiento interno! Tamaño: %ld bytes en pad %d", fileSize, padIndex);

    // TODO: Aquí enlazaras el vector 'fileBuffer' con el mezclador de Oboe para reproducirlo en tiempo real

    // Liberación segura de los caracteres de JNI
    env->ReleaseStringUTFChars(filePathStr, filePath);
    return JNI_TRUE;
}

/**
 * Disparador del pad en tiempo real para el motor nativo Oboe.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativePlayPad(
        JNIEnv *env, jobject thiz, jint padIndex) {
    LOGI("Reproduciendo pad nativo índice: %d", padIndex);
    // TODO: Activar el trigger de reproducción en el callback de Oboe
}
