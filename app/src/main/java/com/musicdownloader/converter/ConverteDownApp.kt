package com.musicdownloader.converter

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.ffmpeg.FFmpeg
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

@HiltAndroidApp
class ConverteDownApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d("ConverteDownApp", "YoutubeDL y FFmpeg inicializados correctamente")
        } catch (e: YoutubeDLException) {
            Log.e("ConverteDownApp", "Error al inicializar YoutubeDL o FFmpeg", e)
        } catch (e: Exception) {
            Log.e("ConverteDownApp", "Error general de inicialización nativa", e)
        }

        // 🎛️ Verificación y descarga quirúrgica de los kits de audio en el primer arranque
        verificarYDescargarKitsDeAudio()
    }

    private fun verificarYDescargarKitsDeAudio() {
        Thread {
            try {
                // Directorio en el almacenamiento interno privado de la app donde vivirán los WAVs descomprimidos
                val kitsDir = File(filesDir, "kits")

                // Verificamos si los audios ya existen y no están vacíos
                if (!kitsDir.exists() || kitsDir.listFiles().isNullOrEmpty()) {
                    Log.i("ConverteDownApp", "Kits de audio no encontrados localmente. Iniciando descarga...")

                    // URL directa al archivo ZIP alojado en las Releases de tu repositorio de GitHub
                    val url = URL("https://github.com/neumann0908/converteDownd/releases/download/v1.0.0/kits.zip")
                    val zipFile = File(filesDir, "kits.zip")

                    // Descarga binaria del archivo optimizado de 671MB
                    url.openStream().use { input ->
                        FileOutputStream(zipFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    Log.i("ConverteDownApp", "Descarga finalizada. Descomprimiendo archivos...")

                    // Creación del directorio destino
                    kitsDir.mkdirs()

                    // Descompresión quirúrgica del archivo ZIP
                    ZipInputStream(zipFile.inputStream()).use { zis ->
                        var ze = zis.nextEntry
                        while (ze != null) {
                            val newFile = File(kitsDir, ze.name)
                            if (ze.isDirectory) {
                                newFile.mkdirs()
                            } else {
                                newFile.parentFile?.mkdirs()
                                FileOutputStream(newFile).use { fos ->
                                    val buffer = ByteArray(8192)
                                    var len: Int
                                    while (zis.read(buffer).also { len = it } > 0) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                            }
                            zis.closeEntry()
                            ze = zis.nextEntry
                        }
                    }

                    // Limpieza: eliminamos el archivo ZIP temporal para liberar espacio en el teléfono
                    zipFile.delete()
                    Log.i("ConverteDownApp", "¡Kits de audio instalados y descomprimidos con éxito en ${kitsDir.absolutePath}!")
                } else {
                    Log.i("ConverteDownApp", "Los kits de audio ya se encuentran listos en la memoria interna.")
                }
            } catch (e: Exception) {
                Log.e("ConverteDownApp", "Error crítico durante la descarga o descompresión de los kits de audio", e)
            }
        }.start()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
