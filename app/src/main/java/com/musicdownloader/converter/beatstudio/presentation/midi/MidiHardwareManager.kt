package com.musicdownloader.converter.beatstudio.presentation.midi

import android.content.Context
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.M)
class MidiHardwareManager(
    private val context: Context,
    private val onExternalNotePlayed: (pitch: Int, velocity: Float) -> Unit,
    private val onClockTickReceived: () -> Unit
) {
    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager

    // Iniciar escaneo y escucha de dispositivos USB y Bluetooth LE conectados
    fun startListeningToExternalDevices() {
        midiManager?.devices?.forEach { device ->
            midiManager.openDevice(device, { openedDevice ->
                val inputPort = openedDevice.openInputPort(0)
                // Abrir puerto de recepción de eventos MIDI externos
                openedDevice.openOutputPort(0)?.connect(object : MidiReceiver() {
                    override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
                        if (count >= 3) {
                            val status = data[offset].toInt() and 0xF0
                            val pitch = data[offset + 1].toInt()
                            val velocity = data[offset + 2].toInt()

                            when (status) {
                                0x90 -> { // Nota Activada (Note On)
                                    val normalizedVel = velocity / 127.0f
                                    if (normalizedVel > 0f) {
                                        onExternalNotePlayed(pitch, normalizedVel)
                                    }
                                }
                                0xF8 -> { // ⏱️ MIDI Clock Tick externo recibido
                                    onClockTickReceived()
                                }
                            }
                        }
                    }
                })
            }, null)
        }
    }
}
