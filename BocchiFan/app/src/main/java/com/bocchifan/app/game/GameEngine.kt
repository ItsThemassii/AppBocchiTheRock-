package com.bocchifan.app.game

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.roundToInt

enum class Judgment(val label: String, val weight: Float, val life: Float, val color: Color, val breaksCombo: Boolean) {
    Perfect("PERFECT", 1f, 0.6f, Color(0xFFFF8FC0), false),
    Great("GREAT", 0.75f, 0.3f, Color(0xFFFFD34E), false),
    Good("GOOD", 0.4f, 0f, Color(0xFF6FD3FF), true),
    Bad("BAD", 0.1f, -4f, Color(0xFFB0A8C8), true),
    Miss("MISS", 0f, -8f, Color(0xFFE5483F), true),
}

/** Ventanas de tiempo (± ms) para cada juicio. */
object Windows {
    const val PERFECT = 45
    const val GREAT = 90
    const val GOOD = 130
    const val BAD = 170

    fun judge(deltaMs: Int): Judgment? {
        val d = abs(deltaMs)
        return when {
            d <= PERFECT -> Judgment.Perfect
            d <= GREAT -> Judgment.Great
            d <= GOOD -> Judgment.Good
            d <= BAD -> Judgment.Bad
            else -> null
        }
    }
}

/** Algo que acaba de pasar, para dibujar efectos y el texto del juicio. */
data class HitEvent(val lane: Int, val judgment: Judgment, val atMs: Int, val early: Boolean)

enum class Rank(val label: String, val minAccuracy: Float) {
    SS("SS", 0.97f), S("S", 0.93f), A("A", 0.85f), B("B", 0.75f), C("C", 0.6f), D("D", 0f);

    companion object {
        fun of(accuracy: Float): Rank = entries.first { accuracy >= it.minAccuracy }
    }
}

data class GameResult(
    val score: Int,
    val accuracy: Float,
    val rank: Rank,
    val maxCombo: Int,
    val counts: Map<Judgment, Int>,
    val failed: Boolean,
) {
    val fullCombo: Boolean get() = !failed && (counts[Judgment.Good] ?: 0) + (counts[Judgment.Bad] ?: 0) + (counts[Judgment.Miss] ?: 0) == 0
    val allPerfect: Boolean get() = fullCombo && (counts[Judgment.Great] ?: 0) == 0
}

/**
 * Reglas del juego (sin dibujo ni sonido, para poder probarlas):
 * recibe toques/sueltas por carril con su tiempo y calcula juicios, combo, vida y puntos.
 */
class GameEngine(val chart: Chart) {
    private enum class State { Waiting, Holding, Done }

    private val notes = chart.notes
    private val state = Array(notes.size) { State.Waiting }
    private var nextIndex = 0 // primera nota que podría seguir pendiente

    private val counts = Judgment.entries.associateWith { 0 }.toMutableMap()
    private var weightSum = 0f
    private var judged = 0

    var combo = 0; private set
    var maxCombo = 0; private set
    var life = 100f; private set
    val failed: Boolean get() = life <= 0f

    /** Eventos nuevos desde la última vez que se leyeron (para efectos visuales y sonido). */
    val events = ArrayList<HitEvent>()

    val score: Int get() = if (chart.judgmentCount == 0) 0 else (1_000_000f * weightSum / chart.judgmentCount).roundToInt()
    val accuracy: Float get() = if (judged == 0) 1f else weightSum / judged

    /** Está terminado cuando todas las notas tienen juicio. */
    val finished: Boolean get() = judged >= chart.judgmentCount

    fun noteState(i: Int): Int = state[i].ordinal // 0 esperando, 1 sostenida, 2 terminada
    fun isHolding(i: Int): Boolean = state[i] == State.Holding

    /** Toque en un carril. Devuelve el juicio si tocó una nota. */
    fun press(lane: Int, nowMs: Int): Judgment? {
        var best = -1
        var bestDelta = Int.MAX_VALUE
        for (i in nextIndex until notes.size) {
            val n = notes[i]
            if (n.timeMs - nowMs > Windows.BAD) break
            if (n.lane != lane || state[i] != State.Waiting) continue
            val d = abs(nowMs - n.timeMs)
            if (d <= Windows.BAD && d < bestDelta) { best = i; bestDelta = d }
        }
        if (best < 0) return null
        val n = notes[best]
        val j = Windows.judge(nowMs - n.timeMs)!!
        record(j, lane, nowMs, early = nowMs < n.timeMs)
        state[best] = if (n.isHold && j != Judgment.Miss) State.Holding else State.Done
        if (n.isHold && state[best] == State.Done) record(Judgment.Miss, lane, nowMs, false)
        return j
    }

    /** Se levantó el dedo de un carril: si había una nota larga sostenida, se juzga su final. */
    fun release(lane: Int, nowMs: Int) {
        for (i in nextIndex until notes.size) {
            if (state[i] != State.Holding || notes[i].lane != lane) continue
            val end = notes[i].endMs!!
            val j = Windows.judge(nowMs - end) ?: Judgment.Miss
            record(j, lane, nowMs, early = nowMs < end)
            state[i] = State.Done
        }
    }

    /** Avanza el tiempo: notas que pasaron sin tocar = MISS; notas largas sostenidas hasta el final = PERFECT. */
    fun update(nowMs: Int) {
        for (i in nextIndex until notes.size) {
            val n = notes[i]
            if (n.timeMs - nowMs > Windows.BAD) break
            when (state[i]) {
                State.Waiting -> if (nowMs - n.timeMs > Windows.BAD) {
                    record(Judgment.Miss, n.lane, nowMs, false)
                    if (n.isHold) record(Judgment.Miss, n.lane, nowMs, false)
                    state[i] = State.Done
                }
                State.Holding -> if (nowMs >= n.endMs!!) {
                    record(Judgment.Perfect, n.lane, nowMs, false)
                    state[i] = State.Done
                }
                State.Done -> Unit
            }
        }
        while (nextIndex < notes.size && state[nextIndex] == State.Done) nextIndex++
    }

    private fun record(j: Judgment, lane: Int, nowMs: Int, early: Boolean) {
        counts[j] = counts.getValue(j) + 1
        weightSum += j.weight
        judged++
        combo = if (j.breaksCombo) 0 else combo + 1
        if (combo > maxCombo) maxCombo = combo
        life = (life + j.life).coerceIn(0f, 100f)
        events += HitEvent(lane, j, nowMs, early)
    }

    fun result(): GameResult = GameResult(
        score = score,
        accuracy = if (chart.judgmentCount == 0) 0f else weightSum / chart.judgmentCount,
        rank = Rank.of(if (chart.judgmentCount == 0) 0f else weightSum / chart.judgmentCount),
        maxCombo = maxCombo,
        counts = counts.toMap(),
        failed = failed,
    )
}
