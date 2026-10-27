package pe.yapevoz

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/** Cabecera con estado, barra inferior Home / Caja / Reportes / Configuración y la pantalla elegida en medio. */
class MainActivity : Activity() {

    // Íconos de Flaticon (ver CREDITOS.md); se tiñen con el color del tema
    private enum class Pestana(val icono: Int, val etiqueta: String) {
        HOME(R.drawable.ic_nav_home, "Home"),
        CAJA(R.drawable.ic_nav_caja, "Caja"),
        REPORTES(R.drawable.ic_nav_reportes, "Reportes"),
        CONFIG(R.drawable.ic_nav_config, "Configuración"),
    }

    private lateinit var p: Paleta
    private lateinit var ui: Ui
    private lateinit var hablador: Hablador
    private lateinit var punto: View
    private lateinit var estado: TextView
    private lateinit var interruptor: Switch
    private lateinit var contenido: ScrollView
    private lateinit var barra: LinearLayout
    private lateinit var pantallas: Map<Pestana, Pantalla>
    private lateinit var reportes: PantallaReportes

    private var pestana = Pestana.HOME

    // El servicio marca cada pago nuevo en las preferencias: así la pantalla se actualiza sola
    private val alCambiar = SharedPreferences.OnSharedPreferenceChangeListener { _, clave ->
        if (clave == Historial.CLAVE) pantallas.getValue(pestana).refrescar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        p = Paleta.para(this)
        // El tema del sistema da el estilo a interruptor, botones y diálogos
        setTheme(
            if (p.oscuro) android.R.style.Theme_Material_NoActionBar
            else android.R.style.Theme_Material_Light_NoActionBar
        )
        super.onCreate(savedInstanceState)
        ui = Ui(this, p)
        hablador = Hablador(this)
        window.statusBarColor = p.fondo
        window.navigationBarColor = p.tarjeta
        if (!p.oscuro) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                (if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
        }

        reportes = PantallaReportes(ui)
        pantallas = mapOf(
            Pestana.HOME to PantallaHome(ui, hablador),
            Pestana.CAJA to PantallaCaja(ui, hablador),
            Pestana.REPORTES to reportes,
            Pestana.CONFIG to PantallaConfig(ui, hablador),
        )
        savedInstanceState?.let { estado ->
            pestana = Pestana.valueOf(estado.getString("pestana", pestana.name))
            pantallas.values.forEach { it.restaurar(estado) }
        }

        contenido = ScrollView(this)
        barra = LinearLayout(this).apply {
            setBackgroundColor(p.tarjeta)
            elevation = ui.dp(8).toFloat()
            setPadding(ui.dp(8), ui.dp(6), ui.dp(8), ui.dp(6))
        }
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(p.fondo)
            addView(cabecera(), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(contenido, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(barra, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            // Con targetSdk 35+ la app dibuja detrás de las barras del sistema
            setOnApplyWindowInsetsListener { _, insets ->
                @Suppress("DEPRECATION")
                setPadding(0, insets.systemWindowInsetTop, 0, 0)
                @Suppress("DEPRECATION")
                barra.setPadding(ui.dp(8), ui.dp(6), ui.dp(8), ui.dp(6) + insets.systemWindowInsetBottom)
                insets
            }
        }
        setContentView(raiz)
        mostrar(pestana)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("pestana", pestana.name)
        pantallas.values.forEach { it.guardar(outState) }
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences("yapevoz", 0).registerOnSharedPreferenceChangeListener(alCambiar)
        refrescarEstado()
        pantallas.getValue(pestana).refrescar()
    }

    override fun onPause() {
        getSharedPreferences("yapevoz", 0).unregisterOnSharedPreferenceChangeListener(alCambiar)
        super.onPause()
    }

    override fun onDestroy() {
        hablador.cerrar()
        super.onDestroy()
    }

    @Deprecated("API de Activity sin AndroidX")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode == PantallaReportes.PEDIDO_GUARDAR && resultCode == RESULT_OK && uri != null) {
            reportes.guardarEn(uri)
        }
    }

    private fun mostrar(nueva: Pestana) {
        pestana = nueva
        val pantalla = pantallas.getValue(nueva)
        contenido.removeAllViews()
        contenido.addView(pantalla.vista)
        contenido.scrollTo(0, 0)
        pantalla.refrescar()
        dibujarBarra()
    }

    private fun dibujarBarra() {
        barra.removeAllViews()
        Pestana.entries.forEach { opcion ->
            val activa = opcion == pestana
            barra.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, ui.dp(6), 0, ui.dp(6))
                if (activa) background = ui.fondoRedondeado(p.fondo, radio = 16)
                contentDescription = opcion.etiqueta
                setOnClickListener { if (!activa) mostrar(opcion) }
                addView(ImageView(this@MainActivity).apply {
                    setImageResource(opcion.icono)
                    setColorFilter(if (activa) p.acento else p.textoSuave)
                }, LinearLayout.LayoutParams(ui.dp(26), ui.dp(26)))
                addView(TextView(this@MainActivity).apply {
                    text = opcion.etiqueta
                    textSize = 12f
                    maxLines = 1
                    gravity = Gravity.CENTER
                    setTextColor(if (activa) p.acento else p.textoSuave)
                    if (activa) setTypeface(typeface, Typeface.BOLD)
                })
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginStart = ui.dp(2)
                marginEnd = ui.dp(2)
            })
        }
    }

    private fun refrescarEstado() {
        val conAcceso = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            .orEmpty().contains(packageName)
        val activo = conAcceso && interruptor.isChecked
        (punto.background as GradientDrawable).setColor(if (activo) p.verde else p.rojo)
        estado.text = when {
            !conAcceso -> "Sin acceso"
            !interruptor.isChecked -> "En pausa"
            else -> "Escuchando"
        }
    }

    private fun cabecera() = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(4))
        punto = View(this@MainActivity).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
        }
        addView(punto, LinearLayout.LayoutParams(ui.dp(12), ui.dp(12)))
        estado = ui.titulo().apply { setPadding(ui.dp(10), 0, 0, 0) }
        addView(estado, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        addView(TextView(this@MainActivity).apply {
            text = if (p.oscuro) "☀️" else "🌙"
            textSize = 22f
            contentDescription = if (p.oscuro) "Cambiar a tema claro" else "Cambiar a tema oscuro"
            setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(8))
            // Sol -> claro, luna -> oscuro. Mantener presionado ofrece "Igual que el sistema".
            setOnClickListener {
                Prefs.setTema(this@MainActivity, if (p.oscuro) Tema.CLARO else Tema.OSCURO)
                recreate()
            }
            setOnLongClickListener { elegirTema(); true }
        })
        interruptor = Switch(this@MainActivity).apply {
            isChecked = Prefs.activado(this@MainActivity)
            setOnCheckedChangeListener { _, activo ->
                Prefs.setActivado(this@MainActivity, activo)
                refrescarEstado()
            }
        }
        addView(interruptor)

        // Mantener presionado: ver el texto crudo de las últimas notificaciones (para soporte)
        setOnLongClickListener {
            val crudo = Fuente.entries.joinToString("\n\n") { f ->
                "${f.etiqueta}:\n${Prefs.ultima(this@MainActivity, f) ?: "(ninguna todavía)"}"
            }
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Últimas notificaciones")
                .setMessage(crudo)
                .setPositiveButton("Cerrar", null)
                .show()
            true
        }
    }

    private fun elegirTema() {
        val temas = Tema.entries
        AlertDialog.Builder(this)
            .setTitle("Apariencia")
            .setSingleChoiceItems(temas.map { it.etiqueta }.toTypedArray(), temas.indexOf(Prefs.tema(this))) { dialogo, i ->
                dialogo.dismiss()
                Prefs.setTema(this, temas[i])
                recreate()
            }
            .show()
    }
}
