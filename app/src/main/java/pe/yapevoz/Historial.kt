package pe.yapevoz

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

data class PagoRegistrado(
    val hora: Long,
    val fuente: Fuente,
    val nombre: String,
    val centimos: Long,
    val frase: String,
)

private class BaseDatos(c: Context) : SQLiteOpenHelper(c, "tuslucas.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE pagos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hora INTEGER NOT NULL,
                fuente TEXT NOT NULL,
                nombre TEXT NOT NULL,
                centimos INTEGER NOT NULL,
                frase TEXT NOT NULL)"""
        )
        db.execSQL("CREATE INDEX idx_pagos_hora ON pagos(hora)")
    }

    override fun onUpgrade(db: SQLiteDatabase, anterior: Int, nueva: Int) = Unit
}

/** Pagos anunciados, guardados en SQLite para poder ver balances por día y por mes. */
object Historial {
    /** Preferencia que cambia con cada pago nuevo: la pantalla la escucha para refrescarse. */
    const val CLAVE = "historial_cambio"
    private const val ARCHIVO = "yapevoz"
    private const val CLAVE_ANTIGUA = "historial"

    @Volatile private var bd: BaseDatos? = null

    private fun bd(c: Context): BaseDatos = bd ?: synchronized(this) {
        bd ?: BaseDatos(c.applicationContext).also {
            bd = it
            migrarDesdePreferencias(c, it)
        }
    }

    fun agregar(c: Context, pago: PagoRegistrado) {
        insertar(bd(c).writableDatabase, pago)
        c.getSharedPreferences(ARCHIVO, 0).edit().putLong(CLAVE, System.currentTimeMillis()).apply()
    }

    /** Pagos con hora en [desde, hasta), del más reciente al más antiguo. */
    fun entre(c: Context, desde: Long, hasta: Long, fuente: Fuente? = null): List<PagoRegistrado> {
        val filtro = StringBuilder("hora >= ? AND hora < ?")
        val args = mutableListOf(desde.toString(), hasta.toString())
        if (fuente != null) {
            filtro.append(" AND fuente = ?")
            args += fuente.name
        }
        return consultar(c, filtro.toString(), args.toTypedArray(), limite = null)
    }

    fun ultimo(c: Context): PagoRegistrado? = recientes(c, 1).firstOrNull()

    /** Los [cantidad] pagos más recientes, de cualquier fecha. */
    fun recientes(c: Context, cantidad: Int): List<PagoRegistrado> =
        consultar(c, null, null, limite = cantidad.toString())

    private fun consultar(c: Context, filtro: String?, args: Array<String>?, limite: String?): List<PagoRegistrado> =
        bd(c).readableDatabase.query(
            "pagos", arrayOf("hora", "fuente", "nombre", "centimos", "frase"),
            filtro, args, null, null, "hora DESC", limite
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        PagoRegistrado(
                            hora = cursor.getLong(0),
                            fuente = runCatching { Fuente.valueOf(cursor.getString(1)) }.getOrDefault(Fuente.YAPE),
                            nombre = cursor.getString(2),
                            centimos = cursor.getLong(3),
                            frase = cursor.getString(4),
                        )
                    )
                }
            }
        }

    private fun insertar(db: SQLiteDatabase, pago: PagoRegistrado) {
        db.insert("pagos", null, ContentValues().apply {
            put("hora", pago.hora)
            put("fuente", pago.fuente.name)
            put("nombre", pago.nombre)
            put("centimos", pago.centimos)
            put("frase", pago.frase)
        })
    }

    // Versiones anteriores guardaban los pagos como JSON en SharedPreferences
    private fun migrarDesdePreferencias(c: Context, bd: BaseDatos) {
        val prefs = c.getSharedPreferences(ARCHIVO, 0)
        val texto = prefs.getString(CLAVE_ANTIGUA, null) ?: return
        val json = runCatching { JSONArray(texto) }.getOrNull()
        if (json != null) {
            val db = bd.writableDatabase
            db.beginTransaction()
            try {
                for (i in 0 until json.length()) {
                    val o = json.optJSONObject(i) ?: continue
                    insertar(
                        db, PagoRegistrado(
                            hora = o.optLong("hora"),
                            fuente = runCatching { Fuente.valueOf(o.optString("fuente")) }.getOrDefault(Fuente.YAPE),
                            nombre = o.optString("nombre"),
                            centimos = o.optLong("centimos"),
                            frase = o.optString("frase"),
                        )
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        prefs.edit().remove(CLAVE_ANTIGUA).apply()
    }
}
