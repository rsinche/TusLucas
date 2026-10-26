package pe.yapevoz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BalanceTest {

    private fun fecha(anio: Int, mes: Int, dia: Int, hora: Int = 0, minuto: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(anio, mes - 1, dia, hora, minuto)
        }.timeInMillis

    private fun pago(hora: Long, fuente: Fuente, nombre: String, centimos: Long) =
        PagoRegistrado(hora, fuente, nombre, centimos, "")

    @Test fun rangoDelDia() = assertEquals(
        fecha(2026, 10, 25) to fecha(2026, 10, 26),
        Balance.rango(Periodo.DIA, fecha(2026, 10, 25, 15, 30))
    )

    @Test fun rangoDelMes() = assertEquals(
        fecha(2026, 10, 1) to fecha(2026, 11, 1),
        Balance.rango(Periodo.MES, fecha(2026, 10, 25, 15, 30))
    )

    @Test fun moverMesCruzaElAnio() = assertEquals(
        fecha(2027, 1, 1),
        Balance.mover(Periodo.MES, fecha(2026, 12, 15), 1)
    )

    @Test fun esActual() {
        val ahora = fecha(2026, 10, 25, 22, 0)
        assertTrue(Balance.esActual(Periodo.DIA, fecha(2026, 10, 25, 8, 0), ahora))
        assertFalse(Balance.esActual(Periodo.DIA, fecha(2026, 10, 24, 8, 0), ahora))
        assertTrue(Balance.esActual(Periodo.MES, fecha(2026, 10, 1), ahora))
    }

    @Test fun agrupaPorDia() {
        val dias = Balance.porDia(
            listOf(
                pago(fecha(2026, 10, 24, 9, 0), Fuente.YAPE, "A", 1000),
                pago(fecha(2026, 10, 25, 10, 0), Fuente.PLIN, "B", 2050),
                pago(fecha(2026, 10, 25, 18, 0), Fuente.YAPE, "C", 100),
            )
        )
        assertEquals(listOf(fecha(2026, 10, 25), fecha(2026, 10, 24)), dias.map { it.inicio })
        assertEquals(listOf(2150L, 1000L), dias.map { it.total })
    }

    @Test fun csvConTotalYComillas() {
        val csv = Balance.csv(
            listOf(
                pago(fecha(2026, 10, 25, 18, 5), Fuente.PLIN, "Torres, Ana", 2050),
                pago(fecha(2026, 10, 25, 9, 0), Fuente.YAPE, "Juan Perez", 1000),
            )
        )
        assertEquals(
            "﻿Fecha,Hora,Medio,Nombre,Monto (S/)\r\n" +
                "2026-10-25,09:00:00,Yape,Juan Perez,10.00\r\n" +
                "2026-10-25,18:05:00,Plin,\"Torres, Ana\",20.50\r\n" +
                "TOTAL,,,,30.50\r\n",
            csv
        )
    }

    @Test fun rangoDeDiasEnCualquierOrden() = assertEquals(
        fecha(2026, 10, 1) to fecha(2026, 10, 26),
        Balance.rangoDias(fecha(2026, 10, 25, 20, 0), fecha(2026, 10, 1, 8, 0))
    )

    @Test fun nombreDeArchivoRango() = assertEquals(
        "tus-lucas_2026-10-01_a_2026-10-25_plin.csv",
        Balance.nombreArchivoRango(fecha(2026, 10, 25), fecha(2026, 10, 1), Fuente.PLIN)
    )

    @Test fun nombreDeArchivo() {
        assertEquals("tus-lucas_2026-10_yape.csv", Balance.nombreArchivo(Periodo.MES, fecha(2026, 10, 25), Fuente.YAPE))
        assertEquals("tus-lucas_2026-10-25_todos.csv", Balance.nombreArchivo(Periodo.DIA, fecha(2026, 10, 25), null))
    }
}
