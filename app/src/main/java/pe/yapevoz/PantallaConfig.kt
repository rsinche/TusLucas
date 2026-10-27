package pe.yapevoz

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/** Configuración: qué pagos se leen en voz alta y a qué volumen. */
class PantallaConfig(private val ui: Ui, private val hablador: Hablador) : Pantalla {

    private val interruptores = Fuente.entries.associateWith { fuente ->
        Switch(ui.act).apply {
            setOnCheckedChangeListener { _, activo -> Prefs.setLeer(ui.act, fuente, activo) }
        }
    }
    private val porcentaje = ui.titulo().apply {
        textSize = 16f
        gravity = Gravity.END
    }
    private val volumen = SeekBar(ui.act).apply {
        max = 100
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(barra: SeekBar, valor: Int, delUsuario: Boolean) = mostrarPorcentaje(valor)
            override fun onStartTrackingTouch(barra: SeekBar) {}
            // Al soltar se guarda y se escucha una muestra con el nuevo volumen
            override fun onStopTrackingTouch(barra: SeekBar) {
                Prefs.setVolumen(ui.act, barra.progress)
                if (barra.progress > 0) hablador.decir("Así se escuchará cada pago")
            }
        })
    }

    override val vista: View = ui.columna().apply {
        addView(ui.titulo("Leer en voz alta"), ui.margen(top = 12))
        addView(ui.texto("Si apagas uno, sus pagos se siguen guardando en Caja y Reportes, pero no se leen."), ui.margen(top = 2))
        Fuente.entries.forEach { fuente -> addView(filaInterruptor(fuente), ui.margen(top = 8)) }

        addView(ui.titulo("Volumen de la voz"), ui.margen(top = 20))
        addView(ui.texto("100 % es el volumen multimedia del teléfono."), ui.margen(top = 2))
        addView(LinearLayout(ui.act).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = ui.fondoRedondeado(ui.p.tarjeta)
            setPadding(ui.dp(8), ui.dp(14), ui.dp(16), ui.dp(14))
            addView(volumen, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(porcentaje, LinearLayout.LayoutParams(ui.dp(64), LinearLayout.LayoutParams.WRAP_CONTENT))
        }, ui.margen(top = 8))
        addView(ui.botonRedondeado("PROBAR VOZ") {
            hablador.decir(PagoParser.frase("JUAN PEREZ te envió un pago por S/ 10", Fuente.YAPE)!!)
        }, ui.margen(top = 12))
    }

    private fun filaInterruptor(fuente: Fuente) = LinearLayout(ui.act).apply {
        gravity = Gravity.CENTER_VERTICAL
        background = ui.fondoRedondeado(ui.p.tarjeta)
        setPadding(0, 0, ui.dp(16), 0)
        addView(View(ui.act).apply {
            background = ui.fondoRedondeado(ui.p.color(fuente))
        }, LinearLayout.LayoutParams(ui.dp(6), ui.dp(56)))
        addView(TextView(ui.act).apply {
            text = "Pagos de ${fuente.etiqueta}"
            textSize = 16f
            setTextColor(ui.p.texto)
            setPadding(ui.dp(12), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val interruptor = interruptores.getValue(fuente)
        addView(interruptor)
        // Tocar toda la fila también cambia el interruptor
        setOnClickListener { interruptor.toggle() }
    }

    private fun mostrarPorcentaje(valor: Int) {
        porcentaje.text = if (valor == 0) "Mudo" else "$valor %"
    }

    override fun refrescar() {
        interruptores.forEach { (fuente, interruptor) -> interruptor.isChecked = Prefs.leer(ui.act, fuente) }
        volumen.progress = Prefs.volumen(ui.act)
        mostrarPorcentaje(volumen.progress)
    }
}
