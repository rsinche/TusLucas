package pe.yapevoz

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

enum class Tema(val etiqueta: String) { SISTEMA("Igual que el sistema"), CLARO("Claro"), OSCURO("Oscuro") }

/** Colores de la pantalla para modo claro u oscuro. */
data class Paleta(
    val oscuro: Boolean,
    val fondo: Int,
    val tarjeta: Int,
    val texto: Int,
    val textoSuave: Int,
    val verde: Int,
    val rojo: Int,
    val aviso: Int,
    private val yape: Int,
    private val plin: Int,
) {
    fun color(fuente: Fuente) = when (fuente) {
        Fuente.YAPE -> yape
        Fuente.PLIN -> plin
    }

    companion object {
        private val CLARA = Paleta(
            oscuro = false,
            fondo = Color.parseColor("#F4F2F7"),
            tarjeta = Color.WHITE,
            texto = Color.parseColor("#1D1B20"),
            textoSuave = Color.parseColor("#6B6670"),
            verde = Color.parseColor("#2E9D5B"),
            rojo = Color.parseColor("#D93C3C"),
            aviso = Color.parseColor("#FFF4E0"),
            yape = Color.parseColor("#742284"),
            plin = Color.parseColor("#00A6C0"),
        )

        // Morado y celeste más claros para que se lean sobre fondo oscuro
        private val OSCURA = Paleta(
            oscuro = true,
            fondo = Color.parseColor("#121114"),
            tarjeta = Color.parseColor("#1F1D23"),
            texto = Color.parseColor("#EDEAF0"),
            textoSuave = Color.parseColor("#A9A3B0"),
            verde = Color.parseColor("#4CC27D"),
            rojo = Color.parseColor("#F06A6A"),
            aviso = Color.parseColor("#3B3018"),
            yape = Color.parseColor("#C08AD4"),
            plin = Color.parseColor("#4FD2E6"),
        )

        fun para(c: Context): Paleta {
            val oscuro = when (Prefs.tema(c)) {
                Tema.CLARO -> false
                Tema.OSCURO -> true
                Tema.SISTEMA -> c.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            }
            return if (oscuro) OSCURA else CLARA
        }
    }
}
