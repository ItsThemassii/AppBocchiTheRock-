package com.bocchifan.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

// Colores de cada integrante de Kessoku Band.
val BocchiPink = Color(0xFFF4A7C3)
val BocchiPinkDeep = Color(0xFFE5578C)
val NijikaYellow = Color(0xFFFFD34E)
val RyoBlue = Color(0xFF4A78D6)
val KitaRed = Color(0xFFE5483F)

// Tinta: bordes y texto sobre tarjetas (igual en los dos temas).
val Ink = Color(0xFF1C1A2E)
val InkSoft = Color(0xFF4A4566)

/** Tema elegido en Ajustes. */
enum class ThemeMode { Light, Dark, System }

/**
 * Tema claro ("cuaderno de día") u oscuro ("cuaderno de noche").
 * Los colores de abajo leen este estado, así que la app se repinta sola al cambiarlo.
 */
object Palette {
    private lateinit var prefs: SharedPreferences
    private val _mode = mutableStateOf(ThemeMode.Light)
    internal val systemDark = mutableStateOf(false)

    val mode: ThemeMode get() = _mode.value
    val dark: Boolean get() = _mode.value == ThemeMode.Dark || (_mode.value == ThemeMode.System && systemDark.value)

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("ui", Context.MODE_PRIVATE)
        _mode.value = runCatching { ThemeMode.valueOf(prefs.getString("theme", "Light")!!) }.getOrDefault(ThemeMode.Light)
    }

    fun setMode(m: ThemeMode) {
        _mode.value = m
        if (::prefs.isInitialized) prefs.edit().putString("theme", m.name).apply()
    }
}

/** Fondo de cuaderno: crema de día, azul noche de noche. */
val Paper: Color get() = if (Palette.dark) Color(0xFF1A1830) else Color(0xFFFFF6EE)
val PaperLine: Color get() = if (Palette.dark) Color(0xFF2F2A4D) else Color(0xFFF3D3DF)

/** Texto escrito directamente sobre el cuaderno (no sobre una tarjeta). */
val OnPaper: Color get() = if (Palette.dark) Color(0xFFF6EEF8) else Ink
val OnPaperSoft: Color get() = if (Palette.dark) Color(0xFFB8AED2) else InkSoft

/** Sombra dura de los stickers: tinta de día, rosa "luz de escenario" de noche. */
val CardShadow: Color get() = if (Palette.dark) Color(0xFF8E3F72) else Ink

/** true si detrás de la barra de estado hay cuaderno claro (íconos oscuros). */
val PaperIsLight: Boolean get() = !Palette.dark

// Escenario (portada).
val StageDark = Color(0xFF14122A)
val StageGlow = Color(0xFF2E2559)

// Caja de mango donde se esconde Bocchi.
val BoxBrown = Color(0xFFD9A066)
val BoxBrownDark = Color(0xFFB57F45)
val MangoOrange = Color(0xFFEF7B2B)

/** Los cuatro colores de la banda, en orden: Bocchi, Nijika, Ryo, Kita. */
val BandColors = listOf(BocchiPink, NijikaYellow, RyoBlue, KitaRed)

private fun bocchiColors() = lightColorScheme(
    primary = BocchiPinkDeep,
    onPrimary = Color.White,
    secondary = NijikaYellow,
    onSecondary = Ink,
    tertiary = RyoBlue,
    onTertiary = Color.White,
    background = Paper,
    onBackground = OnPaper,
    surface = Paper,
    onSurface = OnPaper,
    error = KitaRed,
)

private val BocchiTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Black),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Black),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
fun BocchiTheme(content: @Composable () -> Unit) {
    // Para el modo "Sistema": sigue el tema claro/oscuro del teléfono.
    val systemDark = isSystemInDarkTheme()
    SideEffect { Palette.systemDark.value = systemDark }
    MaterialTheme(
        colorScheme = bocchiColors(),
        typography = BocchiTypography,
        content = content,
    )
}
