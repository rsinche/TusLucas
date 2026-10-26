package pe.yapevoz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PagoParserTest {

    @Test fun yapeReal() = assertEquals(
        "Has recibido 1 sol de Moises Sin",
        PagoParser.frase(
            "Confirmación de Pago\nMoises Sin* te envió un pago por S/ 1. El cód. de seguridad es: 513",
            Fuente.YAPE
        )
    )

    @Test fun yapeMayusculas() = assertEquals(
        "Has recibido 10 soles de Juan Perez",
        PagoParser.frase("Yape! JUAN PEREZ te envió un pago por S/ 10", Fuente.YAPE)
    )

    @Test fun yapeConCentimos() = assertEquals(
        "Has recibido 12 soles con 50 céntimos de Maria L.",
        PagoParser.frase("Maria L. te yapeó S/ 12.50", Fuente.YAPE)
    )

    @Test fun yapePromocionNoSeLee() = assertNull(
        PagoParser.frase("Un vale de Starbucks puede ser tuyo\nDesde las 11 a. m., crea Yape a tu hij@.", Fuente.YAPE)
    )

    @Test fun plinTePlineo() = assertEquals(
        "Has recibido 20 soles de Juan Perez por Plin",
        PagoParser.frase("JUAN PEREZ te plineó S/ 20.00", Fuente.PLIN)
    )

    @Test fun plinRecibiste() = assertEquals(
        "Has recibido 35 soles con 90 céntimos de Ana Torres por Plin",
        PagoParser.frase("¡Recibiste un Plin!\nRecibiste S/ 35.90 de ANA TORRES", Fuente.PLIN)
    )

    @Test fun plinSinNombre() = assertEquals(
        "Has recibido 5 soles por Plin",
        PagoParser.frase("Recibiste un Plin por S/ 5.00 en tu cuenta", Fuente.PLIN)
    )

    @Test fun bancoConsumoNoSeLee() = assertNull(
        PagoParser.frase("Consumo de S/ 50.00 con tu tarjeta en WONG", Fuente.PLIN)
    )

    @Test fun plineasteNoSeLee() = assertNull(
        PagoParser.frase("Plineaste S/ 15.00 a Carlos", Fuente.PLIN)
    )

    @Test fun unSol() = assertEquals("1 sol", PagoParser.montoHablado("1.00"))

    @Test fun miles() = assertEquals("1250 soles", PagoParser.montoHablado("1,250.00"))
}
