package com.bocchifan.app.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.bocchifan.app.R
import com.bocchifan.app.ui.theme.BandColors
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.NijikaYellow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Tipos de sonido al tocar: púa de guitarra, nota grave apagada (volver) o power chord (jugar). */
enum class UiSound { Tap, Back, Chord, None }

/** Sonidos de guitarra de la interfaz (generados con herramientas/BocchiTools.java). */
object UiSounds {
    private var pool: SoundPool? = null
    private val taps = IntArray(5)
    private var back = 0
    private var chord = 0
    private var lastTap = -1
    private lateinit var prefs: SharedPreferences
    private val _enabled = mutableStateOf(true)
    val enabled: State<Boolean> = _enabled

    fun init(context: Context) {
        if (pool != null) return
        val app = context.applicationContext
        prefs = app.getSharedPreferences("ui", Context.MODE_PRIVATE)
        _enabled.value = prefs.getBoolean("sounds", true)
        pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
            .also { p ->
                listOf(R.raw.ui_tap1, R.raw.ui_tap2, R.raw.ui_tap3, R.raw.ui_tap4, R.raw.ui_tap5)
                    .forEachIndexed { i, res -> taps[i] = p.load(app, res, 1) }
                back = p.load(app, R.raw.ui_back, 1)
                chord = p.load(app, R.raw.ui_chord, 1)
            }
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        if (::prefs.isInitialized) prefs.edit().putBoolean("sounds", on).apply()
    }

    fun play(sound: UiSound) {
        val p = pool ?: return
        if (!_enabled.value) return
        when (sound) {
            UiSound.Tap -> {
                // Una nota distinta cada vez (pentatónica de Mi menor), nunca la misma dos veces seguidas.
                var i = Random.nextInt(taps.size)
                if (i == lastTap) i = (i + 1) % taps.size
                lastTap = i
                p.play(taps[i], 0.55f, 0.55f, 1, 0, 1f)
            }
            UiSound.Back -> p.play(back, 0.6f, 0.6f, 1, 0, 1f)
            UiSound.Chord -> p.play(chord, 0.6f, 0.6f, 1, 0, 1f)
            UiSound.None -> Unit
        }
    }
}

/** Ráfagas de estrellitas, púas y notas en el punto donde se toca. */
object TapFx {
    data class Burst(val at: Offset, val startNanos: Long, val seed: Int, val big: Boolean)

    val bursts = mutableStateListOf<Burst>()
    const val LIFE_NANOS = 750_000_000L

    fun spawn(at: Offset, big: Boolean = false) {
        bursts += Burst(at, System.nanoTime(), Random.nextInt(), big)
        while (bursts.size > 10) bursts.removeAt(0)
    }
}

/** Capa (encima de toda la app) que dibuja las ráfagas mientras haya alguna activa. */
@Composable
fun TapFxOverlay(modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.nanoTime()) }
    val active = TapFx.bursts.isNotEmpty()
    LaunchedEffect(active) {
        while (TapFx.bursts.isNotEmpty()) {
            withFrameNanos { }
            now = System.nanoTime()
            TapFx.bursts.removeAll { now - it.startNanos > TapFx.LIFE_NANOS }
        }
    }
    if (!active) return
    Canvas(modifier) {
        TapFx.bursts.forEach { drawBurst(it, now) }
    }
}

private fun DrawScope.drawBurst(b: TapFx.Burst, now: Long) {
    val t = ((now - b.startNanos) / 1e9f).coerceAtLeast(0f)
    val life = TapFx.LIFE_NANOS / 1e9f
    val p = (t / life).coerceIn(0f, 1f)

    // Anillo que se expande.
    val ringP = (t / 0.32f).coerceIn(0f, 1f)
    if (ringP < 1f) {
        drawCircle(
            BocchiPink.copy(alpha = 1f - ringP),
            radius = (if (b.big) 46.dp else 30.dp).toPx() * (0.3f + ringP),
            center = b.at,
            style = Stroke((4.dp.toPx()) * (1f - ringP) + 1f),
        )
    }

    val r = Random(b.seed)
    val count = if (b.big) 14 else 9
    val gravity = 900.dp.toPx() // px/s²: las piezas caen un poco, como confeti
    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 2f * PI.toFloat() + r.nextFloat() * 0.5f
        val speed = (220f + r.nextFloat() * 260f) * (if (b.big) 1.3f else 1f) * density
        val x = b.at.x + cos(angle) * speed * t
        val y = b.at.y + sin(angle) * speed * t + 0.5f * gravity * t * t
        val size = (5f + r.nextFloat() * 6f).dp.toPx() * (1f - p * 0.4f)
        val alpha = if (p < 0.6f) 1f else 1f - (p - 0.6f) / 0.4f
        val spin = (r.nextFloat() - 0.5f) * 720f * t
        val c = Offset(x, y)
        when (i % 3) {
            0 -> drawSparkle(c, size, (if (r.nextBoolean()) Color.White else NijikaYellow).copy(alpha = alpha))
            1 -> rotate(spin + angle * 57f, pivot = c) {
                val color = BandColors[r.nextInt(BandColors.size)]
                val path = pickPath(c.x - size * 0.6f, c.y - size * 0.7f, size * 1.2f, size * 1.4f)
                drawPath(path, color.copy(alpha = alpha))
                drawPath(path, Ink.copy(alpha = alpha), style = Stroke(1.5f.dp.toPx()))
            }
            else -> rotate(spin * 0.3f, pivot = c) { drawEighthNote(c, size * 0.6f, Ink.copy(alpha = alpha * 0.85f)) }
        }
    }
}

private fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticBezierTo(c.x, c.y, c.x + r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y + r)
        quadraticBezierTo(c.x, c.y, c.x - r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawEighthNote(c: Offset, s: Float, color: Color) {
    drawOval(color, Offset(c.x - s, c.y - s * 0.7f), androidx.compose.ui.geometry.Size(s * 2, s * 1.4f))
    drawLine(color, Offset(c.x + s * 0.9f, c.y), Offset(c.x + s * 0.9f, c.y - s * 3f), s * 0.4f)
    drawLine(color, Offset(c.x + s * 0.9f, c.y - s * 3f), Offset(c.x + s * 2f, c.y - s * 2.2f), s * 0.4f)
}

/** Interruptor con el estilo de la app, sonido de guitarra y ráfaga al cambiarlo. */
@Composable
fun BocchiSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val anchor = remember { TapAnchor() }
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = { on ->
            onCheckedChange(on) // primero: así el interruptor de "sonidos" suena al encenderse
            UiSounds.play(if (on) UiSound.Tap else UiSound.Back)
            anchor.coords?.takeIf { it.isAttached }?.let { c ->
                TapFx.spawn(c.localToRoot(Offset(c.size.width / 2f, c.size.height / 2f)))
            }
        },
        modifier = Modifier.onGloballyPositioned { anchor.coords = it },
        colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = com.bocchifan.app.ui.theme.BocchiPinkDeep,
            checkedBorderColor = Ink,
            uncheckedBorderColor = Ink,
        ),
    )
}

private class TapAnchor {
    var coords: LayoutCoordinates? = null
    var lastDown: Offset = Offset.Zero
}

/**
 * Como `clickable`, pero con sonido de guitarra y una ráfaga de estrellitas/púas/notas
 * donde se tocó.
 */
fun Modifier.bocchiClickable(
    sound: UiSound = UiSound.Tap,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val anchor = remember { TapAnchor() }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    this
        .onGloballyPositioned { anchor.coords = it }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.firstOrNull { it.changedToDownIgnoreConsumed() }?.let { anchor.lastDown = it.position }
                }
            }
        }
        .clickable(interactionSource = source, indication = null) {
            UiSounds.play(sound)
            anchor.coords?.takeIf { it.isAttached }?.let {
                TapFx.spawn(it.localToRoot(anchor.lastDown), big = sound == UiSound.Chord)
            }
            onClick()
        }
}
