package com.bocchifan.app.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.KitaRed
import com.bocchifan.app.ui.theme.NijikaYellow
import com.bocchifan.app.ui.theme.RyoBlue
import com.bocchifan.app.ui.theme.StageDark
import com.bocchifan.app.ui.theme.StageGlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/** Color de cada carril, de izquierda a derecha (colores de la banda y de Kikuri). */
val LaneColors = listOf(BocchiPink, NijikaYellow, Color(0xFFC9A2E8), RyoBlue, KitaRed)

/**
 * Geometría del campo en perspectiva: los carriles se juntan hacia un punto de fuga arriba.
 * Una nota a "profundidad" 1 está al fondo y a 0 en la línea de juicio.
 */
class FieldGeometry(val width: Float, val height: Float) {
    val cx = width / 2f
    val judgeY = height * 0.84f
    private val topY = height * 0.12f
    private val minScale = 0.16f
    val judgeWidth = min(width * 0.68f, height * 1.3f)
    val laneWidth = judgeWidth / LANES
    private val vanishY = (topY - minScale * judgeY) / (1f - minScale)
    private val k = 1f / minScale - 1f

    fun scale(depth: Float): Float = 1f / (1f + depth * k)
    fun y(scale: Float): Float = vanishY + (judgeY - vanishY) * scale

    /** X de un borde de carril (0..LANES) a cierta escala. */
    fun edgeX(edge: Float, scale: Float): Float = cx + (edge * laneWidth - judgeWidth / 2f) * scale
    fun laneCenterX(lane: Int, scale: Float): Float = edgeX(lane + 0.5f, scale)

    fun laneAt(x: Float): Int = floor((x - (cx - judgeWidth / 2f)) / laneWidth).toInt().coerceIn(0, LANES - 1)
}

/**
 * Fondo de escenario: degradado, focos que se mueven y barritas de luz del público.
 * Con [overBackdrop] el degradado es semitransparente para dejar ver el GIF de la banda detrás.
 */
fun DrawScope.drawStage(nowMs: Int, overBackdrop: Boolean = false) {
    if (overBackdrop) {
        drawRect(
            Brush.verticalGradient(
                // Un poco más oscuro: los GIFs de concierto son muy coloridos y las notas deben resaltar.
                listOf(StageGlow.copy(alpha = 0.6f), StageDark.copy(alpha = 0.68f), Color(0xFF0B0A18).copy(alpha = 0.9f)),
            ),
        )
    } else {
        drawRect(Brush.verticalGradient(listOf(StageGlow, StageDark, Color(0xFF0B0A18))))
    }
    val t = nowMs / 1000f
    listOf(
        Triple(0.12f, BocchiPink, 0.0f),
        Triple(0.5f, NijikaYellow, 1.3f),
        Triple(0.88f, RyoBlue, 2.6f),
    ).forEach { (fx, color, phase) ->
        val c = Offset(size.width * fx + sin(t * 0.7f + phase) * size.width * 0.06f, -size.height * 0.05f)
        val r = size.height * 1.1f
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent), c, r), r, c)
    }
    // Público con barritas de luz (penlights) que se balancean.
    val count = 26
    for (i in 0 until count) {
        val x = size.width * (i + 0.5f) / count
        val sway = sin(t * 3.2f + i * 0.9f) * 14f
        val color = LaneColors[i % LaneColors.size]
        val base = Offset(x, size.height * 0.995f)
        rotate(sway, pivot = base) {
            drawRoundRect(
                color.copy(alpha = 0.55f),
                topLeft = Offset(x - 3.5f, base.y - size.height * 0.09f),
                size = Size(7f, size.height * 0.09f),
                cornerRadius = CornerRadius(4f),
            )
        }
    }
}

/** Dibuja el campo, las notas y los destellos en el instante [nowMs]. */
fun DrawScope.drawPlayfield(
    g: FieldGeometry,
    chart: Chart,
    engine: GameEngine,
    pressed: IntArray,
    hits: Collection<HitEvent>,
    nowMs: Int,
    approachMs: Int,
) {
    val far = g.scale(1f)
    val near = 1.06f

    // Campo.
    val field = Path().apply {
        moveTo(g.edgeX(0f, far), g.y(far))
        lineTo(g.edgeX(LANES.toFloat(), far), g.y(far))
        lineTo(g.edgeX(LANES.toFloat(), near), g.y(near))
        lineTo(g.edgeX(0f, near), g.y(near))
        close()
    }
    drawPath(field, Ink.copy(alpha = 0.66f))

    // Luz del carril pulsado.
    for (lane in 0 until LANES) {
        if (pressed[lane] == 0) continue
        val beam = Path().apply {
            moveTo(g.edgeX(lane.toFloat(), 0.45f), g.y(0.45f))
            lineTo(g.edgeX(lane + 1f, 0.45f), g.y(0.45f))
            lineTo(g.edgeX(lane + 1f, 1f), g.judgeY)
            lineTo(g.edgeX(lane.toFloat(), 1f), g.judgeY)
            close()
        }
        drawPath(
            beam,
            Brush.verticalGradient(listOf(Color.Transparent, LaneColors[lane].copy(alpha = 0.45f)), g.y(0.45f), g.judgeY),
        )
    }

    // Divisiones y bordes.
    for (e in 1 until LANES) {
        drawLine(Color.White.copy(alpha = 0.13f), Offset(g.edgeX(e.toFloat(), far), g.y(far)), Offset(g.edgeX(e.toFloat(), near), g.y(near)), 2f)
    }
    listOf(0f, LANES.toFloat()).forEach { e ->
        drawLine(BocchiPink.copy(alpha = 0.35f), Offset(g.edgeX(e, far), g.y(far)), Offset(g.edgeX(e, near), g.y(near)), 10f)
        drawLine(BocchiPink, Offset(g.edgeX(e, far), g.y(far)), Offset(g.edgeX(e, near), g.y(near)), 3f)
    }

    // Línea de juicio con brillo.
    val left = g.edgeX(0f, 1f)
    val right = g.edgeX(LANES.toFloat(), 1f)
    drawLine(BocchiPinkDeep.copy(alpha = 0.45f), Offset(left, g.judgeY), Offset(right, g.judgeY), 16f)
    drawLine(Color.White, Offset(left, g.judgeY), Offset(right, g.judgeY), 4f)

    // Receptores (dónde tocar).
    for (lane in 0 until LANES) {
        val cx = g.laneCenterX(lane, 1f)
        val w = g.laneWidth * 0.78f
        val h = g.laneWidth * 0.24f
        val tl = Offset(cx - w / 2, g.judgeY - h / 2)
        if (pressed[lane] > 0) drawOval(LaneColors[lane].copy(alpha = 0.7f), tl, Size(w, h))
        drawOval(Color.White.copy(alpha = 0.7f), tl, Size(w, h), style = Stroke(3f))
    }

    // Notas: de la más lejana a la más cercana, para que las cercanas queden encima.
    val notes = chart.notes
    for (i in notes.indices.reversed()) {
        val n = notes[i]
        val state = engine.noteState(i)
        if (state == 2) continue
        val headDepth = (n.timeMs - nowMs).toFloat() / approachMs
        val holding = engine.isHolding(i)
        if (n.endMs != null) {
            val tailDepth = (n.endMs - nowMs).toFloat() / approachMs
            if (tailDepth < -0.1f || headDepth > 1f) continue
            drawHoldBody(g, n.lane, if (holding) 0f else headDepth, tailDepth.coerceAtMost(1f), holding)
            if (tailDepth <= 1f) drawNote(g, n.lane, tailDepth, small = true)
            if (!holding && headDepth >= -0.08f) drawNote(g, n.lane, headDepth, small = false)
        } else {
            if (headDepth > 1f || headDepth < -0.08f) continue
            drawNote(g, n.lane, headDepth, small = false)
        }
    }

    // Destellos al tocar.
    hits.forEach { hit ->
        val age = nowMs - hit.atMs
        if (age < 0 || age > 380 || hit.judgment == Judgment.Miss) return@forEach
        val p = age / 380f
        val c = Offset(g.laneCenterX(hit.lane, 1f), g.judgeY)
        val color = hit.judgment.color
        drawCircle(color.copy(alpha = (1f - p) * 0.9f), radius = g.laneWidth * (0.25f + 0.5f * p), center = c, style = Stroke(6f * (1f - p) + 1f))
        for (s in 0 until 6) {
            val a = (s / 6f) * 2f * PI.toFloat() + hit.lane
            val d = g.laneWidth * (0.2f + 0.7f * p)
            val sc = Offset(c.x + cos(a) * d, c.y + sin(a) * d * 0.5f)
            drawStar(sc, g.laneWidth * 0.08f * (1f - p) + 1f, Color.White.copy(alpha = 1f - p))
        }
    }
}

private fun DrawScope.drawNote(g: FieldGeometry, lane: Int, depth: Float, small: Boolean) {
    val s = g.scale(depth.coerceAtLeast(-0.25f))
    val cx = g.laneCenterX(lane, s)
    val cy = g.y(s)
    val w = g.laneWidth * s * (if (small) 0.62f else 0.88f)
    val h = g.laneWidth * s * (if (small) 0.2f else 0.28f)
    val r = CornerRadius(h / 2, h / 2)
    val tl = Offset(cx - w / 2, cy - h / 2)
    drawRoundRect(Ink, tl + Offset(0f, h * 0.18f), Size(w, h), r) // sombra
    drawRoundRect(Color.White, tl, Size(w, h), r)
    val inset = h * 0.2f
    drawRoundRect(LaneColors[lane], tl + Offset(inset, inset), Size(w - inset * 2, h - inset * 2), CornerRadius((h - inset * 2) / 2))
    drawRoundRect(Ink, tl, Size(w, h), r, style = Stroke((h * 0.1f).coerceAtLeast(1.5f)))
}

private fun DrawScope.drawHoldBody(g: FieldGeometry, lane: Int, fromDepth: Float, toDepth: Float, holding: Boolean) {
    val s1 = g.scale(fromDepth.coerceAtLeast(0f))
    val s2 = g.scale(toDepth.coerceAtLeast(0f))
    val half = 0.3f
    val body = Path().apply {
        moveTo(g.edgeX(lane + 0.5f - half, s1), g.y(s1))
        lineTo(g.edgeX(lane + 0.5f + half, s1), g.y(s1))
        lineTo(g.edgeX(lane + 0.5f + half, s2), g.y(s2))
        lineTo(g.edgeX(lane + 0.5f - half, s2), g.y(s2))
        close()
    }
    drawPath(body, LaneColors[lane].copy(alpha = if (holding) 0.9f else 0.55f))
    drawPath(body, Color.White.copy(alpha = if (holding) 0.8f else 0.4f), style = Stroke(2f))
}

private fun DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticBezierTo(c.x, c.y, c.x + r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y + r)
        quadraticBezierTo(c.x, c.y, c.x - r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}
