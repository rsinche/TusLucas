package pe.yapevoz

import android.content.Context

object Prefs {
    private const val ARCHIVO = "yapevoz"
    private const val ACTIVADO = "activado"
    private const val TEMA = "tema"

    fun activado(c: Context) = c.getSharedPreferences(ARCHIVO, 0).getBoolean(ACTIVADO, true)
    fun setActivado(c: Context, valor: Boolean) =
        c.getSharedPreferences(ARCHIVO, 0).edit().putBoolean(ACTIVADO, valor).apply()

    fun tema(c: Context) = c.getSharedPreferences(ARCHIVO, 0).getString(TEMA, null)
        ?.let { runCatching { Tema.valueOf(it) }.getOrNull() } ?: Tema.SISTEMA
    fun setTema(c: Context, valor: Tema) =
        c.getSharedPreferences(ARCHIVO, 0).edit().putString(TEMA, valor.name).apply()

    /** Texto crudo de la última notificación de cada fuente, útil para ajustar el parser. */
    fun ultima(c: Context, fuente: Fuente) =
        c.getSharedPreferences(ARCHIVO, 0).getString(clave(fuente), null)
    fun setUltima(c: Context, fuente: Fuente, valor: String) =
        c.getSharedPreferences(ARCHIVO, 0).edit().putString(clave(fuente), valor).apply()

    private fun clave(fuente: Fuente) = "ultima_${fuente.name.lowercase()}"
}
