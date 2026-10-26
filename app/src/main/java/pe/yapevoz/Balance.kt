package pe.yapevoz

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class Periodo { DIA, MES }

/** Cálculos de balance y exportación, sin dependencias de Android para poder probarlos. */
object Balance {

    val ESPANOL: Locale = Locale.forLanguageTag("es-PE")

    data class Dia(val inicio: Long, val pagos: List<PagoRegistrado>) {
        val total get() = pagos.sumOf { it.centimos }
    }

    /** Inicio (incluido) y fin (excluido) del día o mes que contiene [fecha]. */
    fun rango(periodo: Periodo, fecha: Long): Pair<Long, Long> {
        val inicio = inicioDe(periodo, fecha)
        val fin = Calendar.getInstance().apply {
            timeInMillis = inicio
            add(if (periodo == Periodo.DIA) Calendar.DAY_OF_MONTH else Calendar.MONTH, 1)
        }.timeInMillis
        return inicio to fin
    }

    /** Desde el inicio del primer día hasta el final del último (acepta las fechas en cualquier orden). */
    fun rangoDias(desde: Long, hasta: Long): Pair<Long, Long> =
        rango(Periodo.DIA, minOf(desde, hasta)).first to rango(Periodo.DIA, maxOf(desde, hasta)).second

    fun mover(periodo: Periodo, fecha: Long, pasos: Int): Long = Calendar.getInstance().apply {
        timeInMillis = inicioDe(periodo, fecha)
        add(if (periodo == Periodo.DIA) Calendar.DAY_OF_MONTH else Calendar.MONTH, pasos)
    }.timeInMillis

    fun esActual(periodo: Periodo, fecha: Long, ahora: Long = System.currentTimeMillis()) =
        inicioDe(periodo, fecha) == inicioDe(periodo, ahora)

    /** Agrupa pagos por día, del día más reciente al más antiguo. */
    fun porDia(pagos: List<PagoRegistrado>): List<Dia> =
        pagos.groupBy { inicioDe(Periodo.DIA, it.hora) }
            .map { (inicio, delDia) -> Dia(inicio, delDia.sortedByDescending { it.hora }) }
            .sortedByDescending { it.inicio }

    fun soles(centimos: Long) = "S/ %d.%02d".format(centimos / 100, centimos % 100)

    /** CSV con BOM UTF-8 (para que Excel muestre bien las tildes), del pago más antiguo al más reciente. */
    fun csv(pagos: List<PagoRegistrado>): String {
        val fecha = SimpleDateFormat("yyyy-MM-dd", ESPANOL)
        val hora = SimpleDateFormat("HH:mm:ss", ESPANOL)
        return buildString {
            append('﻿')
            append("Fecha,Hora,Medio,Nombre,Monto (S/)\r\n")
            pagos.sortedBy { it.hora }.forEach {
                val d = Date(it.hora)
                append(fecha.format(d)).append(',')
                append(hora.format(d)).append(',')
                append(it.fuente.etiqueta).append(',')
                append(celda(it.nombre)).append(',')
                append(monto(it.centimos)).append("\r\n")
            }
            append("TOTAL,,,,").append(monto(pagos.sumOf { it.centimos })).append("\r\n")
        }
    }

    fun nombreArchivo(periodo: Periodo, fecha: Long, fuente: Fuente?): String {
        val patron = if (periodo == Periodo.DIA) "yyyy-MM-dd" else "yyyy-MM"
        return archivo(SimpleDateFormat(patron, ESPANOL).format(Date(fecha)), fuente)
    }

    fun nombreArchivoRango(desde: Long, hasta: Long, fuente: Fuente?): String {
        val dia = SimpleDateFormat("yyyy-MM-dd", ESPANOL)
        return archivo("${dia.format(Date(minOf(desde, hasta)))}_a_${dia.format(Date(maxOf(desde, hasta)))}", fuente)
    }

    private fun archivo(fechas: String, fuente: Fuente?) =
        "tus-lucas_${fechas}_${fuente?.etiqueta?.lowercase() ?: "todos"}.csv"

    private fun monto(centimos: Long) = "%d.%02d".format(centimos / 100, centimos % 100)

    private fun celda(texto: String) =
        if (texto.any { it == ',' || it == '"' || it == '\n' }) "\"" + texto.replace("\"", "\"\"") + "\"" else texto

    private fun inicioDe(periodo: Periodo, fecha: Long): Long = Calendar.getInstance().apply {
        timeInMillis = fecha
        if (periodo == Periodo.MES) set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
