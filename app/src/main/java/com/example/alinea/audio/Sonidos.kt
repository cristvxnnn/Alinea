package com.example.alinea.audio
import android.media.AudioManager
import android.media.ToneGenerator
import com.example.alinea.core.Sonido
class Sonidos {
    private val generador = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    fun reproducir(sonido:Sonido){
        when(sonido){
            Sonido.TICK -> generador.startTone(ToneGenerator.TONE_PROP_BEEP,120)
            Sonido.EXITO -> generador.startTone(ToneGenerator.TONE_PROP_ACK, 500)
            Sonido.ERROR -> generador.startTone(ToneGenerator.TONE_PROP_NACK, 300)

        }
    }
    fun liberar()=generador.release()
}