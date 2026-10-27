package pe.yapevoz

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class YapeListenerService : NotificationListenerService() {

    companion object {
        // Plin no tiene app propia: llega como notificación de la app del banco
        val PAQUETES = mapOf(
            "com.bcp.innovacxion.yapeapp" to Fuente.YAPE,
            "pe.com.interbank.mobilebanking" to Fuente.PLIN,
            "com.bbva.nxt_peru" to Fuente.PLIN,
        )
        private const val VENTANA_DUPLICADOS_MS = 60_000L
    }

    private lateinit var hablador: Hablador
    private val recientes = LinkedHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        hablador = Hablador(this)
    }

    override fun onDestroy() {
        hablador.cerrar()
        super.onDestroy()
    }

    override fun onListenerDisconnected() {
        // Algunos fabricantes desconectan el servicio; pedimos que el sistema lo vuelva a enlazar
        requestRebind(ComponentName(this, YapeListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val fuente = PAQUETES[sbn.packageName] ?: return
        if (!Prefs.activado(this)) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val cuerpo = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val texto = "$titulo\n$cuerpo".trim()
        if (texto.isEmpty()) return

        Prefs.setUltima(this, fuente, texto)
        if (esDuplicado(texto)) return

        val pago = PagoParser.parse(texto) ?: return
        val frase = PagoParser.frase(pago, fuente)
        if (Prefs.leer(this, fuente)) hablador.decir(frase)
        Historial.agregar(
            this,
            PagoRegistrado(
                hora = System.currentTimeMillis(),
                fuente = fuente,
                nombre = pago.nombre,
                centimos = PagoParser.centimos(pago.monto) ?: 0,
                frase = frase,
            )
        )
    }

    // Yape a veces actualiza la misma notificación; evitamos repetir el anuncio
    private fun esDuplicado(texto: String): Boolean {
        val ahora = System.currentTimeMillis()
        recientes.entries.removeAll { ahora - it.value > VENTANA_DUPLICADOS_MS }
        if (recientes.containsKey(texto)) return true
        recientes[texto] = ahora
        return false
    }
}
