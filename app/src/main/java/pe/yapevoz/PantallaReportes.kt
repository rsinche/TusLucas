package pe.yapevoz

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.Calendar

enum class TipoReporte(val etiqueta: String) { DIA("Día"), MES("Mes"), RANGO("Rango") }

/** Reportes: elegir día, mes o rango de fechas y plataforma, y exportar a CSV. */
class PantallaReportes(private val ui: Ui) : Pantalla {

    companion object {
        const val PEDIDO_GUARDAR = 1
    }

    private var tipo = TipoReporte.MES
    private var filtro: Fuente? = null
    private var dia = System.currentTimeMillis()
    private var mes = System.currentTimeMillis()
    private var desde = Balance.mover(Periodo.DIA, System.currentTimeMillis(), -6)
    private var hasta = System.currentTimeMillis()

    private val selectorTipo = LinearLayout(ui.act)
    private val selectorFuente = LinearLayout(ui.act)
    private val tituloFechas = ui.titulo()
    private val fechas = LinearLayout(ui.act).apply { orientation = LinearLayout.VERTICAL }
    private val tarjeta = ui.tarjetaTotal()
    private val compartir = ui.botonRedondeado("COMPARTIR", R.drawable.ic_compartir) { compartir() }
    private val guardar = ui.botonRedondeado("GUARDAR", R.drawable.ic_guardar) { pedirDondeGuardar() }

    override val vista: View = ui.columna().apply {
        addView(ui.titulo("Tipo de reporte"), ui.margen(top = 12))
        addView(selectorTipo, ui.margen(top = 8))
        addView(ui.titulo("Plataforma"), ui.margen(top = 20))
        addView(selectorFuente, ui.margen(top = 8))
        addView(tituloFechas, ui.margen(top = 20))
        addView(fechas, ui.margen(top = 8))
        addView(ui.titulo("Se exportará"), ui.margen(top = 20))
        addView(tarjeta.vista, ui.margen(top = 8))
        addView(ui.enFila(compartir, guardar), ui.margen(top = 12))
        addView(ui.texto("El archivo es CSV: se abre en Excel o Google Sheets. Compartir lo envía por WhatsApp, correo o Drive; Guardar lo deja en una carpeta del teléfono.").apply {
            gravity = Gravity.CENTER
        }, ui.margen(top = 12))
    }

    override fun refrescar() {
        ui.chips(selectorTipo, TipoReporte.entries.map { it.etiqueta to ui.p.texto }, tipo.ordinal) {
            tipo = TipoReporte.entries[it]
            refrescar()
        }
        ui.chipsFuente(selectorFuente, filtro) { filtro = it; refrescar() }

        fechas.removeAllViews()
        when (tipo) {
            TipoReporte.DIA -> {
                tituloFechas.text = "Día"
                fechas.addView(botonFecha(dia) { dia = it })
            }
            TipoReporte.MES -> {
                tituloFechas.text = "Mes"
                fechas.addView(selectorMes())
            }
            TipoReporte.RANGO -> {
                tituloFechas.text = "Rango de fechas"
                fechas.addView(ui.enFila(
                    etiquetado("Desde", botonFecha(desde) { desde = it }),
                    etiquetado("Hasta", botonFecha(hasta) { hasta = it }),
                ))
            }
        }

        val pagos = pagos()
        tarjeta.etiqueta.text = descripcion()
        tarjeta.total.text = Balance.soles(pagos.sumOf { it.centimos })
        tarjeta.resumen.text = ui.resumen(pagos, conDesglose = filtro == null)
        compartir.isEnabled = pagos.isNotEmpty()
        guardar.isEnabled = pagos.isNotEmpty()
    }

    private fun rango(): Pair<Long, Long> = when (tipo) {
        TipoReporte.DIA -> Balance.rango(Periodo.DIA, dia)
        TipoReporte.MES -> Balance.rango(Periodo.MES, mes)
        TipoReporte.RANGO -> Balance.rangoDias(desde, hasta)
    }

    private fun pagos(): List<PagoRegistrado> {
        val (inicio, fin) = rango()
        return Historial.entre(ui.act, inicio, fin, filtro)
    }

    private fun nombreArchivo() = when (tipo) {
        TipoReporte.DIA -> Balance.nombreArchivo(Periodo.DIA, dia, filtro)
        TipoReporte.MES -> Balance.nombreArchivo(Periodo.MES, mes, filtro)
        TipoReporte.RANGO -> Balance.nombreArchivoRango(desde, hasta, filtro)
    }

    private fun descripcion(): String {
        val periodo = when (tipo) {
            TipoReporte.DIA -> ui.mayuscula(ui.diaLargo.format(java.util.Date(dia)))
            TipoReporte.MES -> ui.nombreMes(mes)
            TipoReporte.RANGO -> {
                val (a, b) = minOf(desde, hasta) to maxOf(desde, hasta)
                "${ui.diaCorto.format(java.util.Date(a))} – ${ui.diaLargo.format(java.util.Date(b))}"
            }
        }
        return "$periodo · ${filtro?.etiqueta ?: "Yape y Plin"}"
    }

    // ---------- Selectores de fecha ----------

    private fun botonFecha(fecha: Long, alElegir: (Long) -> Unit) =
        ui.botonRedondeado(ui.mayuscula(ui.diaLargo.format(java.util.Date(fecha)))) {
            val cal = Calendar.getInstance().apply { timeInMillis = fecha }
            DatePickerDialog(ui.act, { _, anio, mesElegido, diaElegido ->
                alElegir(Calendar.getInstance().apply { clear(); set(anio, mesElegido, diaElegido) }.timeInMillis)
                refrescar()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }

    private fun etiquetado(etiqueta: String, vista: View) = LinearLayout(ui.act).apply {
        orientation = LinearLayout.VERTICAL
        addView(ui.texto(etiqueta))
        addView(vista, ui.margen(top = 4))
    }

    private fun selectorMes() = LinearLayout(ui.act).apply {
        gravity = Gravity.CENTER_VERTICAL
        background = ui.fondoRedondeado(ui.p.tarjeta)
        val actual = Balance.esActual(Periodo.MES, mes)
        fun flecha(simbolo: String, pasos: Int, habilitada: Boolean) = ui.titulo(simbolo).apply {
            textSize = 28f
            setPadding(ui.dp(16), ui.dp(4), ui.dp(16), ui.dp(4))
            isEnabled = habilitada
            alpha = if (habilitada) 1f else 0.25f
            setOnClickListener { mes = Balance.mover(Periodo.MES, mes, pasos); refrescar() }
        }
        addView(flecha("‹", -1, true))
        addView(ui.titulo(ui.nombreMes(mes)).apply {
            textSize = 16f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(flecha("›", 1, !actual))
    }

    // ---------- Exportación ----------

    private fun compartir() {
        val carpeta = File(ui.act.cacheDir, "exportaciones").apply { mkdirs() }
        val archivo = File(carpeta, nombreArchivo())
        archivo.writeText(Balance.csv(pagos()))
        val uri = FileProvider.getUriForFile(ui.act, "${ui.act.packageName}.archivos", archivo)
        val envio = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "Tus Lucas · ${descripcion()}")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ui.act.startActivity(Intent.createChooser(envio, "Compartir reporte"))
    }

    private fun pedirDondeGuardar() {
        ui.act.startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("text/csv")
                .putExtra(Intent.EXTRA_TITLE, nombreArchivo()),
            PEDIDO_GUARDAR
        )
    }

    /** Lo llama la actividad cuando el usuario eligió dónde guardar. */
    fun guardarEn(uri: Uri) {
        val ok = runCatching {
            ui.act.contentResolver.openOutputStream(uri)!!.use { it.write(Balance.csv(pagos()).toByteArray()) }
        }.isSuccess
        Toast.makeText(ui.act, if (ok) "Reporte guardado" else "No se pudo guardar el archivo", Toast.LENGTH_SHORT).show()
    }

    override fun guardar(estado: Bundle) {
        estado.putString("rep_tipo", tipo.name)
        estado.putString("rep_filtro", filtro?.name)
        estado.putLong("rep_dia", dia)
        estado.putLong("rep_mes", mes)
        estado.putLong("rep_desde", desde)
        estado.putLong("rep_hasta", hasta)
    }

    override fun restaurar(estado: Bundle) {
        tipo = TipoReporte.valueOf(estado.getString("rep_tipo", tipo.name))
        filtro = estado.getString("rep_filtro")?.let(Fuente::valueOf)
        dia = estado.getLong("rep_dia", dia)
        mes = estado.getLong("rep_mes", mes)
        desde = estado.getLong("rep_desde", desde)
        hasta = estado.getLong("rep_hasta", hasta)
    }
}
