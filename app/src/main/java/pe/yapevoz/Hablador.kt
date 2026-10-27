package pe.yapevoz

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Envoltura de TextToSpeech que encola frases hasta que el motor esté listo. */
class Hablador(context: Context) : TextToSpeech.OnInitListener {

    private val contexto = context.applicationContext
    private val tts = TextToSpeech(context.applicationContext, this)
    private var listo = false
    private val pendientes = ArrayDeque<String>()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val peru = tts.setLanguage(Locale.forLanguageTag("es-PE"))
        if (peru == TextToSpeech.LANG_MISSING_DATA || peru == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.forLanguageTag("es"))
        }
        // Se escucha por el volumen multimedia y baja momentáneamente la música
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        listo = true
        while (pendientes.isNotEmpty()) decir(pendientes.removeFirst())
    }

    fun decir(frase: String) {
        if (!listo) {
            pendientes.addLast(frase)
            return
        }
        val volumen = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, Prefs.volumen(contexto) / 100f) }
        tts.speak(frase, TextToSpeech.QUEUE_ADD, volumen, frase.hashCode().toString())
    }

    fun cerrar() {
        tts.stop()
        tts.shutdown()
    }
}
