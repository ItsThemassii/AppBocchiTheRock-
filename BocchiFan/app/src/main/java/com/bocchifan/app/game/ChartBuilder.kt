package com.bocchifan.app.game

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Convierte los golpes detectados en un chart jugable para cada dificultad:
 * elige cuántas notas hay, en qué carril va cada una, notas largas y acordes.
 */
object ChartBuilder {

    private data class Params(
        val minGapMs: Int, // separación mínima entre notas
        val keepFraction: Float, // fracción de golpes más fuertes que se usan
        val holdMinMs: Int?, // duración mínima para nota larga (null = sin notas largas)
        val chordFraction: Float, // fracción de notas (las más fuertes) que llevan acorde
        val maxJump: Int, // salto máximo de carril entre notas seguidas
        val holdOverlap: Boolean = false, // notas en otros carriles mientras se sostiene una larga
    )

    private fun params(d: Difficulty) = when (d) {
        Difficulty.Easy -> Params(minGapMs = 520, keepFraction = 0.45f, holdMinMs = null, chordFraction = 0f, maxJump = 2)
        Difficulty.Normal -> Params(minGapMs = 330, keepFraction = 0.65f, holdMinMs = 550, chordFraction = 0f, maxJump = 3)
        Difficulty.Hard -> Params(minGapMs = 200, keepFraction = 0.85f, holdMinMs = 650, chordFraction = 0.08f, maxJump = 4, holdOverlap = true)
        Difficulty.Expert -> Params(minGapMs = 125, keepFraction = 1f, holdMinMs = 600, chordFraction = 0.16f, maxJump = 4, holdOverlap = true)
    }

    fun build(onsets: List<Onset>, durationMs: Int, difficulty: Difficulty, seed: Long = 7L): Chart {
        if (onsets.isEmpty()) return Chart(emptyList(), durationMs)
        val p = params(difficulty)
        val random = Random(seed + difficulty.ordinal * 101)

        // 1) Los golpes más fuertes, respetando la separación mínima (los fuertes tienen prioridad).
        val byStrength = onsets.sortedByDescending { it.strength }
        val candidates = byStrength.take((onsets.size * p.keepFraction).roundToInt().coerceAtLeast(1))
        val taken = java.util.TreeMap<Int, Onset>()
        for (o in candidates) {
            val before = taken.floorKey(o.timeMs)
            val after = taken.ceilingKey(o.timeMs)
            if (before != null && o.timeMs - before < p.minGapMs) continue
            if (after != null && after - o.timeMs < p.minGapMs) continue
            // Deja 1,5 s libres al principio para que dé tiempo a prepararse.
            if (o.timeMs < 1500) continue
            taken[o.timeMs] = o
        }
        val picked = taken.values.toList()
        if (picked.isEmpty()) return Chart(emptyList(), durationMs)

        // El "tono" se convierte en percentil entre las notas elegidas: así los 5 carriles se usan
        // por igual (si no, los golpes de batería, que son agudos, cargan todo a la derecha).
        val pitchRank = HashMap<Onset, Float>()
        picked.sortedBy { it.pitch }.forEachIndexed { i, o -> pitchRank[o] = i / (picked.size - 1).coerceAtLeast(1).toFloat() }

        // Solo los sonidos más sostenidos de la canción (~12 %) pueden ser notas largas.
        val sustainCut = picked.map { it.sustainMs }.sortedDescending()[(picked.size * 0.12f).toInt().coerceAtMost(picked.size - 1)]
        var lastHoldEnd = -10_000

        val chordCut = if (p.chordFraction > 0f) {
            picked.map { it.strength }.sortedDescending()[(picked.size * p.chordFraction).toInt().coerceAtMost(picked.size - 1)]
        } else Float.MAX_VALUE

        // 2) Carril según el "tono" (graves a la izquierda, agudos a la derecha), sin saltos imposibles.
        val notes = ArrayList<Note>()
        val laneFreeAt = IntArray(LANES) // ms en que termina una nota larga en cada carril
        var prevLane = LANES / 2
        var prevTime = -10_000
        picked.forEachIndexed { index, o ->
            val pitch = pitchRank.getValue(o)
            var lane = (pitch * LANES + (random.nextFloat() - 0.5f) * 1.6f).toInt().coerceIn(0, LANES - 1)
            if (abs(lane - prevLane) > p.maxJump) lane = prevLane + p.maxJump * if (lane > prevLane) 1 else -1
            // Evita repetir el mismo carril muy rápido (los "jacks" son incómodos).
            if (lane == prevLane && o.timeMs - prevTime < 260) lane = stepAside(lane, random)
            lane = firstFreeLane(lane, o.timeMs, laneFreeAt) ?: return@forEachIndexed

            val next = picked.getOrNull(index + 1)?.timeMs ?: durationMs
            val holdEnd = p.holdMinMs?.let { min ->
                // En Fácil/Normal nada más llega mientras se sostiene; en Difícil/Experto pueden
                // llegar notas en otros carriles a la vez (el carril de la nota larga queda reservado).
                val room = if (p.holdOverlap) 1600 else next - o.timeMs - 180
                val eligible = o.sustainMs >= min && o.sustainMs >= sustainCut && o.timeMs - lastHoldEnd > 1200
                if (eligible && room >= min) o.timeMs + minOf(o.sustainMs, room, 2000) else null
            }
            if (holdEnd != null) lastHoldEnd = holdEnd
            notes += Note(o.timeMs, lane, holdEnd)
            laneFreeAt[lane] = (holdEnd ?: o.timeMs) + 60

            if (holdEnd == null && o.strength >= chordCut) {
                val partner = if (lane <= 1) lane + 2 else lane - 2
                if (laneFreeAt[partner] <= o.timeMs) {
                    notes += Note(o.timeMs, partner)
                    laneFreeAt[partner] = o.timeMs + 60
                }
            }
            prevLane = lane
            prevTime = o.timeMs
        }
        return Chart(notes.sortedWith(compareBy({ it.timeMs }, { it.lane })), durationMs)
    }

    private fun stepAside(lane: Int, random: Random): Int = when (lane) {
        0 -> 1
        LANES - 1 -> LANES - 2
        else -> if (random.nextBoolean()) lane + 1 else lane - 1
    }

    /** El carril pedido si está libre; si no, el libre más cercano. */
    private fun firstFreeLane(wanted: Int, timeMs: Int, laneFreeAt: IntArray): Int? {
        for (d in 0 until LANES) {
            for (l in intArrayOf(wanted - d, wanted + d)) {
                if (l in 0 until LANES && laneFreeAt[l] <= timeMs) return l
            }
        }
        return null
    }
}
