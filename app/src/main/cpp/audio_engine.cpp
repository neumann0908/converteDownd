#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <mutex>
#include <array>
#include <cstring>
#include <cstdio>

#define TAG "AudioEngineNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Estructura para gestionar la voz en reproducción de cada pad
struct PlayingVoice {
    int padIndex = -1;
    size_t cursor = 0;
    bool isPlaying = false;
};

// Almacén global de buffers PCM de 16 bits para hasta 64 pads/pistas
static std::vector<int16_t> g_sampleBuffers[64];
static std::mutex g_samplesMutex;

// Voces activas para mezcla polifónica
static std::array<PlayingVoice, 64> g_voices;

/**
 * Callback de audio de ultra baja latencia con mezclador en tiempo real.
 */
class MyAudioStreamCallback : public oboe::AudioStreamCallback {
public:
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override {
        auto *outputBuffer = static_cast<float *>(audioData);
        int32_t channelCount = oboeStream->getChannelCount();
        int32_t totalSamples = numFrames * channelCount;

        // Inicializar el búfer de salida con silencio limpio
        std::memset(outputBuffer, 0, totalSamples * sizeof(float));

        std::lock_guard<std::mutex> lock(g_samplesMutex);
        
        // Mezclar todos los pads que estén sonando actualmente
        for (int p = 0; p < 64; ++p) {
            auto &voice = g_voices[p];
            if (voice.isPlaying) {
                auto &buffer = g_sampleBuffers[voice.padIndex];
                if (buffer.empty()) {
                    voice.isPlaying = false;
                    continue;
                }

                for (int i = 0; i < totalSamples; i += channelCount) {
                    if (voice.cursor < buffer.size()) {
                        // Convertir PCM 16-bit (-32768 a 32767) a Float (-1.0f a 1.0f)
                        float sampleVal = static_cast<float>(buffer[voice.cursor++]) / 32768.0f;
                        
                        // Inyectar en canal izquierdo
                        outputBuffer[i] += sampleVal;
                        
                        // Inyectar en canal derecho si es estéreo
                        if (channelCount > 1 && (i + 1) < totalSamples) {
                            outputBuffer[i + 1] += sampleVal;
                        }
                    } else {
                        voice.isPlaying = false; // Fin del sample
                        break;
                    }
                }
            }
        }

        return oboe::DataCallbackResult::Continue;
    }
};

static oboe::ManagedStream g_stream;
static MyAudioStreamCallback g_callback;

/**
 * Inicia el motor Oboe aplicando estrategias de respaldo (Fallbacks) ante restricciones de hardware.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativeStart(JNIEnv *env, jobject thiz) {
    if (g_stream && g_stream->getState() == oboe::StreamState::Open) {
        return JNI_TRUE; // Ya está abierto y activo
    }

    oboe::AudioStreamBuilder builder;
    builder.setCallback(&g_callback);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(oboe::ChannelCount::Stereo);

    // 1. Intento principal: Ultra baja latencia en modo exclusivo
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    
    oboe::Result result = builder.openManagedStream(g_stream);
    if (result != oboe::Result::OK) {
        LOGI("⚠️ LowLatency/Exclusive no disponible en este hardware. Aplicando respaldo (Shared/None)...");
        
        // 2. Respaldo universal compatible con auriculares Bluetooth y dispositivos estrictos
        builder.setPerformanceMode(oboe::PerformanceMode::None);
        builder.setSharingMode(oboe::SharingMode::Shared);
        result = builder.openManagedStream(g_stream);
    }

    if (result != oboe::Result::OK) {
        LOGE("❌ Error crítico al abrir el stream de Oboe: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    result = g_stream->start();
    if (result != oboe::Result::OK) {
        LOGE("❌ Error al iniciar el stream de Oboe: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    LOGI("✅ ¡Motor Oboe iniciado con éxito! Frecuencia de muestreo: %d Hz", g_stream->getSampleRate());
    return JNI_TRUE;
}

/**
 * Carga un sample de audio normalizado (WAV 16-bit) directamente en la memoria nativa asignada al pad.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativeLoadSampleByPath(
        JNIEnv *env, jobject thiz, jstring filePathStr, jint padIndex) {

    if (filePathStr == nullptr || padIndex < 0 || padIndex >= 64) {
        LOGE("nativeLoadSampleByPath: Parámetros inválidos.");
        return JNI_FALSE;
    }

    const char *filePath = env->GetStringUTFChars(filePathStr, nullptr);
    if (filePath == nullptr) return JNI_FALSE;

    FILE *file = fopen(filePath, "rb");
    if (file == nullptr) {
        LOGE("nativeLoadSampleByPath: No se pudo abrir el archivo en: %s", filePath);
        env->ReleaseStringUTFChars(filePathStr, filePath);
        return JNI_FALSE;
    }

    // Obtener tamaño del archivo
    fseek(file, 0, SEEK_END);
    long fileSize = ftell(file);
    fseek(file, 0, SEEK_SET);

    if (fileSize <= 44) { // Menor o igual a la cabecera WAV mínima
        LOGE("nativeLoadSampleByPath: Archivo corrupto o demasiado pequeño: %s", filePath);
        fclose(file);
        env->ReleaseStringUTFChars(filePathStr, filePath);
        return JNI_FALSE;
    }

    // Leer todo el archivo WAV a un vector temporal
    std::vector<uint8_t> fileBuffer(fileSize);
    size_t bytesRead = fread(fileBuffer.data(), 1, fileSize, file);
    fclose(file);

    env->ReleaseStringUTFChars(filePathStr, filePath);

    if (bytesRead != static_cast<size_t>(fileSize)) {
        LOGE("nativeLoadSampleByPath: Error leyendo bytes del archivo.");
        return JNI_FALSE;
    }

    // Validar cabecera RIFF / WAVE básica
    if (std::memcmp(fileBuffer.data(), "RIFF", 4) != 0 || std::memcmp(fileBuffer.data() + 8, "WAVE", 4) != 0) {
        LOGE("nativeLoadSampleByPath: El archivo no posee una cabecera WAV válida.");
        return JNI_FALSE;
    }

    // Extraer los datos PCM puros omitiendo la cabecera WAV estándar de 44 bytes
    const size_t headerSize = 44;
    size_t pcmDataSize = fileSize - headerSize;
    size_t numSamples = pcmDataSize / sizeof(int16_t);

    std::vector<int16_t> pcmSamples(numSamples);
    std::memcpy(pcmSamples.data(), fileBuffer.data() + headerSize, pcmDataSize);

    // Guardar los samples en el banco de memoria protegido por el Mutex
    {
        std::lock_guard<std::mutex> lock(g_samplesMutex);
        g_sampleBuffers[padIndex] = std::move(pcmSamples);
    }

    LOGI("✅ Sample cargado y mapeado con éxito en C++ para el pad %d. Muestras PCM: %zu", padIndex, numSamples);
    return JNI_TRUE;
}

/**
 * Disparador instantáneo del pad en tiempo real.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativePlayPad(
        JNIEnv *env, jobject thiz, jint padIndex) {
    if (padIndex < 0 || padIndex >= 64) return;

    std::lock_guard<std::mutex> lock(g_samplesMutex);
    // Activa la voz y reinicia el cursor de reproducción al inicio (muestra 0)
    g_voices[padIndex] = PlayingVoice{padIndex, 0, true};
}
