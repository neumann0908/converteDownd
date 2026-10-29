#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <mutex>
#include <array>
#include <cstring>
#include <cstdio>
#include <cmath> // Necesario para tanhf y funciones trigonométricas

#define TAG "AudioEngineNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Estructura avanzada con soporte de Pitch fraccional, Pan y Fade-in/out
struct PlayingVoice {
    int padIndex = -1;
    float cursor = 0.0f;     // Cursor en float para permitir control de afinación (Pitch)
    bool isPlaying = false;
    bool isFadingOut = false;
    float currentGain = 0.0f; 
    int fadeOutCounter = 0;   
};

// Parámetros configurables por cada pad desde la UI (Sliders)
struct PadConfig {
    float pitch = 1.0f;      // 1.0 = normal, >1.0 = agudo, <1.0 = grave
    float pan = 0.0f;        // -1.0 (Izquierda) a 1.0 (Derecha)
    float volume = 1.0f;     // 0.0 a 1.0
};

// Almacén global de buffers PCM de 16 bits y configuraciones para hasta 64 pads
static std::vector<int16_t> g_sampleBuffers[64];
static PadConfig g_padConfigs[64];
static std::mutex g_samplesMutex;
static std::array<PlayingVoice, 64> g_voices;

/**
 * Callback de audio con mezclador polifónico en tiempo real, Pitch, Pan, Anti-Clipping y Anti-Chasquidos.
 */
class MyAudioStreamCallback : public oboe::AudioStreamCallback {
public:
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override {
        auto *outputBuffer = static_cast<float *>(audioData);
        int32_t channelCount = oboeStream->getChannelCount();
        int32_t totalSamples = numFrames * channelCount;

        // Inicializar búfer de salida con silencio limpio
        std::memset(outputBuffer, 0, totalSamples * sizeof(float));

        {
            std::lock_guard<std::mutex> lock(g_samplesMutex);
            
            for (int p = 0; p < 64; ++p) {
                auto &voice = g_voices[p];
                if (voice.isPlaying) {
                    auto &buffer = g_sampleBuffers[voice.padIndex];
                    if (buffer.empty()) {
                        voice.isPlaying = false;
                        continue;
                    }

                    auto &config = g_padConfigs[voice.padIndex];

                    // Leyes de paneo estéreo (Constant Power Panning)
                    float panRad = (config.pan + 1.0f) * 0.78539816339f; // Mapeo de -1..1 a 0..PI/2
                    float leftGainFactor = cosf(panRad) * config.volume;
                    float rightGainFactor = sinf(panRad) * config.volume;

                    for (int i = 0; i < totalSamples; i += channelCount) {
                        size_t sampleIndex = static_cast<size_t>(voice.cursor);

                        if (sampleIndex < buffer.size() && !voice.isFadingOut) {
                            // 1. Fade-in ultra rápido en las primeras 64 muestras para evitar chasquidos
                            if (voice.currentGain < 1.0f) {
                                voice.currentGain += 1.0f / 64.0f;
                                if (voice.currentGain > 1.0f) voice.currentGain = 1.0f;
                            }

                            float sampleVal = (static_cast<float>(buffer[sampleIndex]) / 32768.0f) * voice.currentGain;
                            
                            // Inyectar con ganancia de volumen y ley de paneo estéreo
                            outputBuffer[i] += sampleVal * leftGainFactor;
                            if (channelCount > 1 && (i + 1) < totalSamples) {
                                outputBuffer[i + 1] += sampleVal * rightGainFactor;
                            }

                            // Avanzar cursor aplicando el factor de Pitch configurado
                            voice.cursor += config.pitch;
                        } else {
                            // 2. Fade-out suave al terminar el sample o al pausar
                            if (!voice.isFadingOut) {
                                voice.isFadingOut = true;
                                voice.fadeOutCounter = 64;
                            }

                            if (voice.fadeOutCounter > 0) {
                                voice.currentGain = static_cast<float>(voice.fadeOutCounter) / 64.0f;
                                voice.fadeOutCounter--;

                                if (sampleIndex < buffer.size()) {
                                    float sampleVal = (static_cast<float>(buffer[sampleIndex]) / 32768.0f) * voice.currentGain;
                                    outputBuffer[i] += sampleVal * leftGainFactor;
                                    if (channelCount > 1 && (i + 1) < totalSamples) {
                                        outputBuffer[i + 1] += sampleVal * rightGainFactor;
                                    }
                                    voice.cursor += config.pitch;
                                }
                            } else {
                                voice.isPlaying = false;
                                voice.isFadingOut = false;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 3. Etapa de Masterización: Soft Clipping (tanhf) para evitar saturación y distorsión digital
        for (int i = 0; i < totalSamples; ++i) {
            outputBuffer[i] = tanhf(outputBuffer[i]);
        }

        return oboe::DataCallbackResult::Continue;
    }
};

static oboe::ManagedStream g_stream;
static MyAudioStreamCallback g_callback;

/**
 * Inicia el motor Oboe aplicando estrategias de respaldo (Fallbacks).
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativeStart(JNIEnv *env, jobject thiz) {
    if (g_stream && g_stream->getState() == oboe::StreamState::Open) {
        return JNI_TRUE;
    }

    oboe::AudioStreamBuilder builder;
    builder.setCallback(&g_callback);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setChannelCount(oboe::ChannelCount::Stereo);

    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    
    oboe::Result result = builder.openManagedStream(g_stream);
    if (result != oboe::Result::OK) {
        builder.setPerformanceMode(oboe::PerformanceMode::None);
        builder.setSharingMode(oboe::SharingMode::Shared);
        result = builder.openManagedStream(g_stream);
    }

    if (result != oboe::Result::OK) {
        LOGE("❌ Error crítico al abrir Oboe: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    result = g_stream->start();
    if (result != oboe::Result::OK) {
        LOGE("❌ Error al iniciar Oboe: %s", oboe::convertToText(result));
        return JNI_FALSE;
    }

    LOGI("✅ ¡Motor Oboe iniciado con éxito!");
    return JNI_TRUE;
}

/**
 * Detiene y cierra el stream de Oboe.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativeStop(JNIEnv *env, jobject thiz) {
    if (g_stream) {
        g_stream->stop();
        g_stream->close();
        g_stream.reset();
        LOGI("🛑 Recursos de audio liberados.");
    }
}

/**
 * Pausa la reproducción aplicando desvanecimiento suave (fade-out) en todas las voces.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativePause(JNIEnv *env, jobject thiz) {
    std::lock_guard<std::mutex> lock(g_samplesMutex);
    for (auto &voice : g_voices) {
        if (voice.isPlaying && !voice.isFadingOut) {
            voice.isFadingOut = true;
            voice.fadeOutCounter = 64;
        }
    }
    LOGI("⏸️ Pausa solicitada: desvanecimiento suave aplicado.");
}

/**
 * Conecta los sliders de Pitch, Pan y Volumen desde Kotlin hacia C++ (Función 4).
 */
extern "C" JNIEXPORT void JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativeSetPadParams(
        JNIEnv *env, jobject thiz, jint padIndex, jfloat pitch, jfloat pan, jfloat volume) {
    if (padIndex < 0 || padIndex >= 64) return;
    std::lock_guard<std::mutex> lock(g_samplesMutex);
    g_padConfigs[padIndex] = PadConfig{pitch, pan, volume};
}

/**
 * Carga un sample de audio normalizado (WAV 16-bit).
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativeLoadSampleByPath(
        JNIEnv *env, jobject thiz, jstring filePathStr, jint padIndex) {

    if (filePathStr == nullptr || padIndex < 0 || padIndex >= 64) return JNI_FALSE;

    const char *filePath = env->GetStringUTFChars(filePathStr, nullptr);
    if (filePath == nullptr) return JNI_FALSE;

    FILE *file = fopen(filePath, "rb");
    if (file == nullptr) {
        env->ReleaseStringUTFChars(filePathStr, filePath);
        return JNI_FALSE;
    }

    fseek(file, 0, SEEK_END);
    long fileSize = ftell(file);
    fseek(file, 0, SEEK_SET);

    if (fileSize <= 44) {
        fclose(file);
        env->ReleaseStringUTFChars(filePathStr, filePath);
        return JNI_FALSE;
    }

    std::vector<uint8_t> fileBuffer(fileSize);
    fread(fileBuffer.data(), 1, fileSize, file);
    fclose(file);
    env->ReleaseStringUTFChars(filePathStr, filePath);

    if (std::memcmp(fileBuffer.data(), "RIFF", 4) != 0 || std::memcmp(fileBuffer.data() + 8, "WAVE", 4) != 0) {
        return JNI_FALSE;
    }

    const size_t headerSize = 44;
    size_t pcmDataSize = fileSize - headerSize;
    size_t numSamples = pcmDataSize / sizeof(int16_t);

    std::vector<int16_t> pcmSamples(numSamples);
    std::memcpy(pcmSamples.data(), fileBuffer.data() + headerSize, pcmDataSize);

    {
        std::lock_guard<std::mutex> lock(g_samplesMutex);
        g_sampleBuffers[padIndex] = std::move(pcmSamples);
    }

    return JNI_TRUE;
}

/**
 * Disparador instantáneo del pad en tiempo real.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativePlayPad(
        JNIEnv *env, jobject thiz, jint padIndex) {
    if (padIndex < 0 || padIndex >= 64) return;

    std::lock_guard<std::mutex> lock(g_samplesMixin); // (protegido por g_samplesMutex)
    std::lock_guard<std::mutex> lockMutex(g_samplesMutex);
    g_voices[padIndex] = PlayingVoice{padIndex, 0.0f, true, false, 0.0f, 0};
}

/**
 * Sistema de Exportación Offline a WAV (Función 3): Renderiza un búfer completo de audio a disco.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_musicdownloader_converter_converter_engine_AudioEngine_nativeExportWavTest(
        JNIEnv *env, jobject thiz, jstring outputPathStr, jint padIndex) {
    if (outputPathStr == nullptr || padIndex < 0 || padIndex >= 64) return JNI_FALSE;

    const char *outPath = env->GetStringUTFChars(outputPathStr, nullptr);
    if (outPath == nullptr) return JNI_FALSE;

    std::lock_guard<std::mutex> lock(g_samplesMutex);
    auto &buffer = g_sampleBuffers[padIndex];
    if (buffer.empty()) {
        env->ReleaseStringUTFChars(outputPathStr, outPath);
        return JNI_FALSE;
    }

    FILE *file = fopen(outPath, "wb");
    if (file == nullptr) {
        env->ReleaseStringUTFChars(outputPathStr, outPath);
        return JNI_FALSE;
    }

    // Cabecera WAV estándar (44 bytes) para PCM 16-bit Estéreo / Mono
    int sampleRate = 44100;
    short channels = 2;
    short bitsPerSample = 16;
    int dataSize = buffer.size() * sizeof(int16_t) * channels;
    int chunkSize = 36 + dataSize;
    int byteRate = sampleRate * channels * (bitsPerSample / 8);
    short blockAlign = channels * (bitsPerSample / 8);

    fwrite("RIFF", 1, 4, file);
    fwrite(&chunkSize, 4, 1, file);
    fwrite("WAVE", 1, 4, file);
    fwrite("fmt ", 1, 4, file);
    int subChunk1Size = 16;
    short audioFormat = 1; // PCM
    fwrite(&subChunk1Size, 4, 1, file);
    fwrite(&audioFormat, 2, 1, file);
    fwrite(&channels, 2, 1, file);
    fwrite(&sampleRate, 4, 1, file);
    fwrite(&byteRate, 4, 1, file);
    fwrite(&blockAlign, 2, 1, file);
    fwrite(&bitsPerSample, 2, 1, file);
    fwrite("data", 1, 4, file);
    fwrite(&dataSize, 4, 1, file);

    // Escribir datos duplicados a estéreo para la exportación de prueba
    for (size_t i = 0; i < buffer.size(); ++i) {
        int16_t sample = buffer[i];
        fwrite(&sample, sizeof(int16_t), 1, file); // Canal Izquierdo
        fwrite(&sample, sizeof(int16_t), 1, file); // Canal Derecho
    }

    fclose(file);
    env->ReleaseStringUTFChars(outputPathStr, outPath);
    LOGI("💾 Archivo WAV exportado exitosamente a: %s", outPath);
    return JNI_TRUE;
}
