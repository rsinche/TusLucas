package pe.yapevoz

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import java.util.Date

/** Una pestaña de la barra inferior. */
interface Pantalla {
    val vista: View
    fun refrescar()
    fun guardar(estado: Bundle) {}
    fun restaurar(estado: Bundle) {}
}

/** Home: avisos de configuración y los últimos Yapes y Plineos recibidos. */
class PantallaHome(private val ui: Ui, private val hablador: Hablador) : Pantalla {

    companion object {
        private const val CANTIDAD = 50
    }

    private val avisos = LinearLayout(ui.act).apply { orientation = LinearLayout.VERTICAL }
    private val hoy = ui.texto(tamano = 15f)
    private val repetir = ui.botonRedondeado("REPETIR ÚLTIMO") {
        Historial.ultimo(ui.act)?.let { hablador.decir(it.frase) }
    }
    private val lista = LinearLayout(ui.act).apply { orientation = LinearLayout.VERTICAL }

    override val vista: View = ui.columna().apply {
        addView(avisos)
        addView(ui.titulo("Últimos Yapes y Plineos"), ui.margen(top = 12))
        addView(hoy, ui.margen(top = 2))
        addView(repetir, ui.margen(top = 12))
        addView(lista, ui.margen(top = 12))
        addView(ui.botonPlano("Probar voz") {
            hablador.decir(PagoParser.frase("JUAN PEREZ te envió un pago por S/ 10", Fuente.YAPE)!!)
            hablador.decir(PagoParser.frase("ANA TORRES te plineó S/ 20.50", Fuente.PLIN)!!)
        }, ui.margen(top = 8))
    }

    override fun refrescar() {
        val act = ui.act
        val conAcceso = Settings.Secure.getString(act.contentResolver, "enabled_notification_listeners")
            .orEmpty().contains(act.packageName)
        val sinLimiteBateria = act.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(act.packageName)

        avisos.removeAllViews()
        if (!conAcceso) avisos.addView(ui.aviso("Necesito permiso para leer las notificaciones de Yape y Plin.", "Dar acceso") {
            act.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }, ui.margen(top = 12))
        if (!sinLimiteBateria) avisos.addView(ui.aviso("El ahorro de batería puede cerrar la app y dejarías de oír los pagos.", "Quitar límite") {
            act.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }, ui.margen(top = 12))

        val (desde, hasta) = Balance.rango(Periodo.DIA, System.currentTimeMillis())
        val deHoy = Historial.entre(act, desde, hasta)
        hoy.text = "Hoy llevas ${Balance.soles(deHoy.sumOf { it.centimos })} en ${ui.resumen(deHoy, conDesglose = false)}"

        val recientes = Historial.recientes(act, CANTIDAD)
        repetir.isEnabled = recientes.isNotEmpty()
        lista.removeAllViews()
        if (recientes.isEmpty()) lista.addView(ui.vacio("Aún no hay pagos.\nCuando llegue un Yape o Plin aparecerá aquí."))
        recientes.forEach { pago ->
            lista.addView(ui.fila(
                colorBarra = ui.p.color(pago.fuente),
                principal = pago.nombre.ifEmpty { "Sin nombre" },
                secundario = "${pago.fuente.etiqueta}  ·  ${cuando(pago.hora)}",
                colorSecundario = ui.p.color(pago.fuente),
                monto = pago.centimos,
            ) { hablador.decir(pago.frase) }, ui.margen(bottom = 8)) // Tocar un pago lo vuelve a leer
        }
    }

    // "Hoy 18:24", "Ayer 10:00" o "Jue 22 oct 19:00"
    private fun cuando(hora: Long): String {
        val ayer = Balance.mover(Periodo.DIA, System.currentTimeMillis(), -1)
        val dia = when {
            Balance.esActual(Periodo.DIA, hora) -> "Hoy"
            Balance.esActual(Periodo.DIA, hora, ahora = ayer) -> "Ayer"
            else -> ui.mayuscula(ui.diaCorto.format(Date(hora)))
        }
        return "$dia ${ui.hora.format(Date(hora))}"
    }
}

/** Caja: control del cobrado por día o mes, filtrado por Yape / Plin. */
class PantallaCaja(private val ui: Ui, private val hablador: Hablador) : Pantalla {

    private var periodo = Periodo.DIA
    private var filtro: Fuente? = null
    private var fecha = System.currentTimeMillis()

    private val selectorPeriodo = LinearLayout(ui.act)
    private val selectorFuente = LinearLayout(ui.act)
    private val tituloPeriodo = ui.titulo().apply {
        textSize = 16f
        gravity = android.view.Gravity.CENTER
        // Tocar el título vuelve a hoy / al mes actual
        setOnClickListener { fecha = System.currentTimeMillis(); refrescar() }
    }
    private val siguiente = flecha("›", 1)
    private val tarjeta = ui.tarjetaTotal()
    private val tituloLista = ui.titulo()
    private val lista = LinearLayout(ui.act).apply { orientation = LinearLayout.VERTICAL }

    override val vista: View = ui.columna().apply {
        addView(selectorPeriodo, ui.margen(top = 12))
        addView(selectorFuente, ui.margen(top = 8))
        addView(LinearLayout(ui.act).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(flecha("‹", -1))
            addView(tituloPeriodo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(siguiente)
        }, ui.margen(top = 8))
        addView(tarjeta.vista, ui.margen(top = 4))
        addView(tituloLista, ui.margen(top = 20))
        addView(lista, ui.margen(top = 8))
    }

    private fun flecha(simbolo: String, pasos: Int) = ui.titulo(simbolo).apply {
        textSize = 28f
        setPadding(ui.dp(16), ui.dp(4), ui.dp(16), ui.dp(4))
        setOnClickListener {
            fecha = Balance.mover(periodo, fecha, pasos)
            refrescar()
        }
    }

    override fun refrescar() {
        ui.chips(selectorPeriodo, listOf("Día" to ui.p.texto, "Mes" to ui.p.texto), periodo.ordinal) {
            periodo = Periodo.entries[it]
            refrescar()
        }
        ui.chipsFuente(selectorFuente, filtro) { filtro = it; refrescar() }

        val actual = Balance.esActual(periodo, fecha)
        tituloPeriodo.text = if (periodo == Periodo.DIA) ui.nombreDia(fecha) else ui.nombreMes(fecha)
        siguiente.isEnabled = !actual
        siguiente.alpha = if (actual) 0.25f else 1f

        val (desde, hasta) = Balance.rango(periodo, fecha)
        val todos = Historial.entre(ui.act, desde, hasta)
        val pagos = filtro?.let { f -> todos.filter { it.fuente == f } } ?: todos

        tarjeta.etiqueta.text = "Cobrado" + (filtro?.let { " por ${it.etiqueta}" } ?: "")
        tarjeta.total.text = Balance.soles(pagos.sumOf { it.centimos })
        tarjeta.resumen.text = if (filtro != null) ui.resumen(pagos, conDesglose = false) else ui.resumen(todos, conDesglose = true)

        lista.removeAllViews()
        when (periodo) {
            Periodo.DIA -> {
                tituloLista.text = "Pagos"
                if (pagos.isEmpty()) lista.addView(ui.vacio(if (actual) "Aún no hay pagos hoy." else "No hay pagos este día."))
                pagos.forEach { pago ->
                    lista.addView(ui.fila(
                        colorBarra = ui.p.color(pago.fuente),
                        principal = pago.nombre.ifEmpty { "Sin nombre" },
                        secundario = "${pago.fuente.etiqueta}  ·  ${ui.hora.format(Date(pago.hora))}",
                        colorSecundario = ui.p.color(pago.fuente),
                        monto = pago.centimos,
                    ) { hablador.decir(pago.frase) }, ui.margen(bottom = 8))
                }
            }
            Periodo.MES -> {
                tituloLista.text = "Por día"
                val dias = Balance.porDia(pagos)
                if (dias.isEmpty()) lista.addView(ui.vacio("No hay pagos en este mes."))
                dias.forEach { dia ->
                    val medios = Fuente.entries.filter { f -> dia.pagos.any { it.fuente == f } }
                    lista.addView(ui.fila(
                        colorBarra = medios.singleOrNull()?.let(ui.p::color) ?: ui.p.textoSuave,
                        principal = ui.mayuscula(ui.diaCorto.format(Date(dia.inicio))),
                        secundario = ui.resumen(dia.pagos, conDesglose = false),
                        colorSecundario = ui.p.textoSuave,
                        monto = dia.total,
                    ) {
                        // Tocar un día abre su detalle
                        periodo = Periodo.DIA
                        fecha = dia.inicio
                        refrescar()
                    }, ui.margen(bottom = 8))
                }
            }
        }
    }

    override fun guardar(estado: Bundle) {
        estado.putString("caja_periodo", periodo.name)
        estado.putString("caja_filtro", filtro?.name)
        estado.putLong("caja_fecha", fecha)
    }

    override fun restaurar(estado: Bundle) {
        periodo = Periodo.valueOf(estado.getString("caja_periodo", periodo.name))
        filtro = estado.getString("caja_filtro")?.let(Fuente::valueOf)
        fecha = estado.getLong("caja_fecha", fecha)
    }
}
