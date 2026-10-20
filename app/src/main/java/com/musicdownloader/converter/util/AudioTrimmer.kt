package com.musicdownloader.converter.util

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

object AudioTrimmer {
    fun trim(srcPath: String, dstPath: String, startMs: Long, endMs: Long) {
        val startSec = startMs / 1000.0
        val durationSec = (endMs - startMs) / 1000.0

        // 1. Intentar recorte rápido copiando streams sin re-codificar
        val command = "-ss $startSec -i \"$srcPath\" -t $durationSec -c copy \"$dstPath\" -y"
        val session = FFmpegKit.execute(command)

        // 2. Si falla el copiado directo, reintentar con recodificación estándar
        if (!ReturnCode.isSuccess(session.returnCode)) {
            val fallbackCommand = "-ss $startSec -i \"$srcPath\" -t $durationSec \"$dstPath\" -y"
            val fallbackSession = FFmpegKit.execute(fallbackCommand)
            
            if (!ReturnCode.isSuccess(fallbackSession.returnCode)) {
                throw Exception("Error al recortar con FFmpeg: ${fallbackSession.failStackTrace ?: "Desconocido"}")
            }
        }
    }
}
