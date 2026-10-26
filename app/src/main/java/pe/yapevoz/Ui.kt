package pe.yapevoz

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date

/** Piezas visuales compartidas por las pantallas (todo en código, sin XML). */
class Ui(val act: Activity, val p: Paleta) {

    val diaCorto = SimpleDateFormat("EEE d MMM", Balance.ESPANOL)
    val diaLargo = SimpleDateFormat("EEE d MMM yyyy", Balance.ESPANOL)
    val mes = SimpleDateFormat("MMMM yyyy", Balance.ESPANOL)
    val hora = SimpleDateFormat("HH:mm", Balance.ESPANOL)

    fun dp(valor: Int) = (valor * act.resources.displayMetrics.density).toInt()

    fun margen(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(0, dp(top), 0, dp(bottom)) }

    fun fondoRedondeado(color: Int, radio: Int = 14) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radio).toFloat()
    }

    fun columna(padding: Int = 16) = LinearLayout(act).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(padding), dp(8), dp(padding), dp(24))
    }

    fun titulo(texto: String = "") = TextView(act).apply {
        text = texto
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(p.texto)
    }

    fun texto(texto: String = "", tamano: Float = 14f, color: Int = p.textoSuave) = TextView(act).apply {
        text = texto
        textSize = tamano
        setTextColor(color)
    }

    fun boton(texto: String, alPulsar: () -> Unit) = Button(act).apply {
        text = texto
        isAllCaps = false
        setOnClickListener { alPulsar() }
    }

    /**
     * Botón con el mismo estilo que los chips (fondo de tarjeta, esquinas redondeadas),
     * ícono opcional teñido como el texto; se atenúa al deshabilitarlo.
     */
    fun botonRedondeado(texto: String, icono: Int? = null, alPulsar: () -> Unit): View =
        object : LinearLayout(act) {
            override fun setEnabled(enabled: Boolean) {
                super.setEnabled(enabled)
                alpha = if (enabled) 1f else 0.4f
            }
        }.apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = RippleDrawable(ColorStateList.valueOf(p.textoSuave), fondoRedondeado(p.tarjeta), null)
            icono?.let {
                addView(ImageView(act).apply {
                    setImageResource(it)
                    setColorFilter(p.texto)
                }, LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(8) })
            }
            addView(TextView(act).apply {
                text = texto
                textSize = 15f
                setTextColor(p.texto)
            })
            setOnClickListener { alPulsar() }
        }

    fun botonPlano(texto: String, alPulsar: () -> Unit) = Button(act, null, android.R.attr.borderlessButtonStyle).apply {
        text = texto
        isAllCaps = false
        setTextColor(p.textoSuave)
        setOnClickListener { alPulsar() }
    }

    /** Dos o más vistas repartidas en partes iguales a lo ancho. */
    fun enFila(vararg vistas: View) = LinearLayout(act).apply {
        vistas.forEachIndexed { i, vista ->
            addView(vista, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginStart = if (i == 0) 0 else dp(6)
                marginEnd = if (i == vistas.lastIndex) 0 else dp(6)
            })
        }
    }

    /** Tarjeta con un monto grande: etiqueta arriba, monto y un resumen debajo. */
    class TarjetaTotal(val vista: View, val etiqueta: TextView, val total: TextView, val resumen: TextView)

    fun tarjetaTotal(): TarjetaTotal {
        val etiqueta = texto(tamano = 15f)
        val total = TextView(act).apply {
            textSize = 40f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(p.texto)
        }
        val resumen = texto().apply { gravity = Gravity.CENTER }
        val vista = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = fondoRedondeado(p.tarjeta)
            elevation = dp(2).toFloat()
            setPadding(dp(16), dp(20), dp(16), dp(20))
            addView(etiqueta)
            addView(total)
            addView(resumen)
        }
        return TarjetaTotal(vista, etiqueta, total, resumen)
    }

    /** "N pagos · Yape S/ x · Plin S/ y" (sin el desglose si hay un medio filtrado). */
    fun resumen(pagos: List<PagoRegistrado>, conDesglose: Boolean): String {
        val cantidad = "${pagos.size} ${if (pagos.size == 1) "pago" else "pagos"}"
        if (!conDesglose) return cantidad
        return cantidad + Fuente.entries.joinToString("") { f ->
            "  ·  ${f.etiqueta} ${Balance.soles(pagos.filter { it.fuente == f }.sumOf { it.centimos })}"
        }
    }

    /** Botones tipo pestaña; el elegido se rellena con su color. */
    fun chips(contenedor: LinearLayout, opciones: List<Pair<String, Int>>, elegido: Int, alElegir: (Int) -> Unit) {
        contenedor.removeAllViews()
        opciones.forEachIndexed { i, (texto, color) ->
            val activo = i == elegido
            contenedor.addView(TextView(act).apply {
                text = texto
                gravity = Gravity.CENTER
                textSize = 15f
                setPadding(0, dp(10), 0, dp(10))
                background = fondoRedondeado(if (activo) color else p.tarjeta)
                setTextColor(if (activo) p.fondo else p.texto)
                if (activo) setTypeface(typeface, Typeface.BOLD)
                setOnClickListener { alElegir(i) }
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginStart = if (i == 0) 0 else dp(4)
                marginEnd = if (i == opciones.lastIndex) 0 else dp(4)
            })
        }
    }

    /** Chips Todos / Yape / Plin. */
    fun chipsFuente(contenedor: LinearLayout, elegido: Fuente?, alElegir: (Fuente?) -> Unit) {
        val fuentes = listOf<Fuente?>(null) + Fuente.entries
        chips(
            contenedor,
            fuentes.map { f -> (f?.etiqueta ?: "Todos") to (f?.let(p::color) ?: p.texto) },
            fuentes.indexOf(elegido)
        ) { alElegir(fuentes[it]) }
    }

    fun fila(
        colorBarra: Int, principal: String, secundario: String, colorSecundario: Int, monto: Long, alTocar: () -> Unit,
    ) = LinearLayout(act).apply {
        background = fondoRedondeado(p.tarjeta)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 0, dp(16), 0)
        setOnClickListener { alTocar() }

        addView(View(act).apply {
            background = fondoRedondeado(colorBarra)
        }, LinearLayout.LayoutParams(dp(6), dp(56)))

        addView(LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), 0, dp(8))
            addView(TextView(act).apply {
                text = principal
                textSize = 16f
                setTextColor(p.texto)
            })
            addView(TextView(act).apply {
                text = secundario
                textSize = 13f
                setTextColor(colorSecundario)
            })
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

        addView(TextView(act).apply {
            text = Balance.soles(monto)
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(p.texto)
        })
    }

    fun vacio(mensaje: String) = texto(mensaje).apply {
        gravity = Gravity.CENTER
        setPadding(0, dp(24), 0, dp(24))
    }

    fun aviso(mensaje: String, accion: String, alPulsar: () -> Unit) = LinearLayout(act).apply {
        orientation = LinearLayout.VERTICAL
        background = fondoRedondeado(p.aviso)
        setPadding(dp(16), dp(12), dp(16), dp(4))
        addView(texto("⚠️  $mensaje", color = p.texto))
        addView(Button(act, null, android.R.attr.borderlessButtonStyle).apply {
            text = accion
            isAllCaps = false
            setOnClickListener { alPulsar() }
        }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.END })
    }

    /** "Hoy · dom 25 oct", "Ayer · sáb 24 oct" o "Vie 23 oct 2026". */
    fun nombreDia(fecha: Long): String {
        val ayer = Balance.mover(Periodo.DIA, System.currentTimeMillis(), -1)
        return when {
            Balance.esActual(Periodo.DIA, fecha) -> "Hoy · " + diaCorto.format(Date(fecha))
            Balance.esActual(Periodo.DIA, fecha, ahora = ayer) -> "Ayer · " + diaCorto.format(Date(fecha))
            else -> mayuscula(diaLargo.format(Date(fecha)))
        }
    }

    fun nombreMes(fecha: Long) = mayuscula(mes.format(Date(fecha)))

    fun mayuscula(texto: String) = texto.replaceFirstChar { it.titlecase(Balance.ESPANOL) }
}
