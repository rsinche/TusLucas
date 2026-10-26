package pe.yapevoz

enum class Fuente(val etiqueta: String) { YAPE("Yape"), PLIN("Plin") }

/**
 * Convierte el texto de una notificación de Yape o Plin en la frase que se dirá en voz alta.
 *
 * Formatos conocidos:
 *   Yape (real):  "Confirmación de Pago\nMoises Sin* te envió un pago por S/ 1. El cód. de seguridad es: 513"
 *   Plin (banco): "Juan Perez te plineó S/ 20.00" / "Recibiste un Plin de S/ 20.00 de Juan Perez"
 */
object PagoParser {

    data class Pago(val nombre: String, val monto: String)

    private val MONTO = Regex("""S/\.?\s*(\d[\d.,]*)""")

    // "<nombre> te envió/plineó/yapeó ..."
    private val REMITENTE_ANTES = Regex(
        """(?:Yape!?\s*)?([^\n!:]+?)\s+te\s+(?:envi[oó]|yape[oó]|pline[oó]|transfiri[oó]|ha\s+(?:enviado|yapeado|plineado|transferido))""",
        RegexOption.IGNORE_CASE
    )

    // "... S/ 20.00 de <nombre>"
    private val REMITENTE_DESPUES = Regex("""S/\.?\s*\d[\d.,]*\s+de\s+([^\n.,!]+)""", RegexOption.IGNORE_CASE)

    // Movimientos de salida que no deben anunciarse como dinero recibido
    private val SALIENTE = Regex(
        """enviaste|pagaste|yapeaste|plineaste|transferiste|consumo|compra|retiro|cargo""",
        RegexOption.IGNORE_CASE
    )

    fun parse(texto: String): Pago? {
        val monto = MONTO.find(texto)?.groupValues?.get(1)?.trimEnd('.', ',') ?: return null

        val antes = REMITENTE_ANTES.find(texto)?.groupValues?.get(1)
        if (antes != null) return Pago(limpiarNombre(antes), monto)

        // Sin un "te envió" explícito, solo aceptamos textos que hablen de recibir dinero
        if (SALIENTE.containsMatchIn(texto) || !texto.contains("recib", ignoreCase = true)) return null

        val despues = REMITENTE_DESPUES.find(texto)?.groupValues?.get(1)
            ?.takeUnless { Regex("""^(tu|su|la|el)\s""", RegexOption.IGNORE_CASE).containsMatchIn(it) }
        return Pago(despues?.let(::limpiarNombre).orEmpty(), monto)
    }

    /** Frase final, o null si el texto no parece dinero recibido. */
    fun frase(texto: String, fuente: Fuente): String? = parse(texto)?.let { frase(it, fuente) }

    fun frase(pago: Pago, fuente: Fuente): String {
        val cantidad = montoHablado(pago.monto)
        return buildString {
            append("Has recibido ").append(cantidad)
            if (pago.nombre.isNotEmpty()) append(" de ").append(pago.nombre)
            if (fuente == Fuente.PLIN || pago.nombre.isEmpty()) append(" por ").append(fuente.etiqueta)
        }
    }

    /** "1,250.50" -> 125050 */
    fun centimos(monto: String): Long? {
        val normalizado = when {
            monto.contains('.') && monto.contains(',') -> monto.replace(",", "")
            Regex(""",\d{1,2}$""").containsMatchIn(monto) -> monto.replace(",", ".")
            else -> monto.replace(",", "")
        }
        val partes = normalizado.split('.')
        val soles = partes[0].toLongOrNull() ?: return null
        val centimos = partes.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toIntOrNull() ?: 0
        return soles * 100 + centimos
    }

    /** "10" -> "10 soles", "1" -> "1 sol", "12.50" -> "12 soles con 50 céntimos" */
    fun montoHablado(monto: String): String {
        val total = centimos(monto) ?: return "$monto soles"
        val soles = total / 100
        val centimos = total % 100

        val textoSoles = if (soles == 1L) "1 sol" else "$soles soles"
        return when {
            centimos == 0L -> textoSoles
            soles == 0L -> "$centimos céntimos"
            else -> "$textoSoles con $centimos céntimos"
        }
    }

    // Yape oculta apellidos con "*" ("Moises Sin*"), que el TTS leería como "asterisco".
    // "JUAN PEREZ" se lee mejor como "Juan Perez" (evita que el TTS deletree).
    private fun limpiarNombre(nombre: String): String =
        nombre.replace("*", "").trim().lowercase().split(Regex("\\s+")).joinToString(" ") { palabra ->
            palabra.replaceFirstChar { it.titlecase() }
        }
}
