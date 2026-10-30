package com.example.alinea.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Voz(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    @Volatile
    private var lista = false
    var activada = true
    private var ultimoMs = 0L

    override fun onInit(status: Int) {
        val motor = tts ?: return
        if (status != TextToSpeech.SUCCESS) return
        var r = motor.setLanguage(Locale("es", "CO"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            r = motor.setLanguage(Locale("es"))
        }
        lista = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
    }

    /**
     * urgente: corta lo que se esté diciendo y habla ya.
     * Si no es urgente, respeta un tiempo de enfriamiento para no repetir frases todo el rato.
     */
    fun decir(frase: String, urgente: Boolean = false, enfriamientoMs: Long = 4000) {
        if (!activada || !lista) return
        val ahora = System.currentTimeMillis()
        if (!urgente && ahora - ultimoMs < enfriamientoMs) return
        ultimoMs = ahora
        tts?.speak(
            frase,
            if (urgente) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
            null,
            "alinea",
        )
    }

    fun callar() {
        tts?.stop()
    }

    fun liberar() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}