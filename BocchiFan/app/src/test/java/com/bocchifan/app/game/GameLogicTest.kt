package com.bocchifan.app.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class GameLogicTest {

    @Test
    fun lrcParsing() {
        val lrc = """
            [ti:Prueba]
            [offset:+200]
            [00:01.50]uno
            [00:03.2]dos
            [00:05.123][00:09.00]tres
        """.trimIndent()
        val l = com.bocchifan.app.data.LyricsRepository.parseLrc(lrc)
        assertEquals(listOf(1300, 3000, 4923, 8800), l.lines.map { it.timeMs })
        assertEquals(listOf("uno", "dos", "tres", "tres"), l.lines.map { it.text })
        assertTrue(l.synced)
        assertEquals(-1, l.currentIndex(1000))
        assertEquals(1, l.currentIndex(3500))
        assertEquals(3, l.currentIndex(60_000))
    }

    @Test
    fun pastedLyrics() {
        val plain = com.bocchifan.app.data.LyricsRepository.parseText("\n\nlinea uno\n\nlinea dos\n\n")
        assertTrue(!plain.synced)
        assertEquals(listOf("linea uno", "", "linea dos"), plain.lines.map { it.text })
        val synced = com.bocchifan.app.data.LyricsRepository.parseText("[00:05.00] hola\n[00:07.50] adiós")
        assertTrue(synced.synced)
        assertEquals(listOf(5000, 7500), synced.lines.map { it.timeMs })
    }

    @Test
    fun judgmentWindows() {
        assertEquals(Judgment.Perfect, Windows.judge(0))
        assertEquals(Judgment.Perfect, Windows.judge(-45))
        assertEquals(Judgment.Great, Windows.judge(60))
        assertEquals(Judgment.Good, Windows.judge(-120))
        assertEquals(Judgment.Bad, Windows.judge(160))
        assertNull(Windows.judge(200))
    }

    @Test
    fun tapsHoldsAndMisses() {
        val chart = Chart(
            listOf(Note(1000, 0), Note(1500, 2), Note(2000, 1, endMs = 3000), Note(4000, 4)),
            durationMs = 5000,
        )
        val e = GameEngine(chart)
        assertEquals(Judgment.Perfect, e.press(0, 1010))
        assertNull("Tocar un carril vacío no hace nada", e.press(3, 1500))
        assertEquals(Judgment.Great, e.press(2, 1440))
        // Nota larga: se toca a tiempo y se suelta al final.
        assertEquals(Judgment.Perfect, e.press(1, 2000))
        e.update(2500)
        e.release(1, 2990)
        assertEquals(4, e.combo)
        // La última nota no se toca: MISS al pasar la ventana.
        e.update(4300)
        assertEquals(0, e.combo)
        assertTrue(e.finished)
        val r = e.result()
        assertEquals(4, r.maxCombo)
        assertEquals(1, r.counts[Judgment.Miss])
        assertTrue(!r.fullCombo)
    }

    @Test
    fun holdReleasedEarlyIsMiss() {
        val e = GameEngine(Chart(listOf(Note(1000, 0, endMs = 2000)), 3000))
        e.press(0, 1000)
        e.release(0, 1300)
        assertEquals(Judgment.Miss, e.events.last().judgment)
    }

    @Test
    fun allPerfectScoresMillion() {
        val notes = (1..50).map { Note(it * 300, it % LANES) }
        val e = GameEngine(Chart(notes, 20000))
        notes.forEach { e.press(it.lane, it.timeMs) }
        e.update(30000)
        val r = e.result()
        assertEquals(1_000_000, r.score)
        assertTrue(r.allPerfect)
        assertEquals(Rank.SS, r.rank)
    }

    /** Audio sintético: golpes en tiempos conocidos. El detector debe encontrarlos a ±25 ms. */
    @Test
    fun analyzerFindsKnownBeats() {
        val rate = 22050
        val beats = (0 until 40).map { 1000 + it * 350 + (if (it % 3 == 0) 70 else 0) }
        val samples = FloatArray(rate * 16)
        val rnd = Random(1)
        for (t in beats) {
            val start = t * rate / 1000
            val freq = 120.0 + (t % 5) * 150
            for (i in 0 until rate / 6) {
                val idx = start + i
                if (idx >= samples.size) break
                val env = exp(-i / (rate * 0.04)).toFloat()
                samples[idx] += env * (0.6f * sin(2 * PI * freq * i / rate).toFloat() + 0.3f * (rnd.nextFloat() * 2 - 1))
            }
        }
        val onsets = OnsetAnalyzer.analyze(samples, rate)
        val found = beats.count { b -> onsets.any { abs(it.timeMs - b) <= 25 } }
        val errors = beats.mapNotNull { b -> onsets.minByOrNull { abs(it.timeMs - b) }?.let { it.timeMs - b } }
        println("Golpes reales: ${beats.size}, detectados: ${onsets.size}, encontrados a 25ms: $found, error medio: ${errors.average()} ms")
        assertTrue("El error medio debe ser pequeño", abs(errors.average()) < 12)
        assertTrue("Debe encontrar casi todos los golpes", found >= beats.size * 0.9)
        assertTrue("No debe inventar muchos golpes", onsets.size <= beats.size * 1.3)
    }

    /**
     * Prueba con una canción real si existe el archivo (PCM mono float 22050 Hz).
     * Genera: ffmpeg -i cancion.mp3 -ac 1 -ar 22050 -f f32le cancion.raw
     */
    @Test
    fun realSongStats() {
        val path = System.getenv("BOCCHI_RAW") ?: return
        val bytes = File(path).readBytes()
        val buf = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val samples = FloatArray(buf.remaining()).also { buf.get(it) }
        val t0 = System.currentTimeMillis()
        val onsets = OnsetAnalyzer.analyze(samples, 22050)
        val durationMs = (samples.size * 1000L / 22050).toInt()
        println("Cancion ${durationMs / 1000}s, golpes ${onsets.size}, análisis ${System.currentTimeMillis() - t0} ms")
        Difficulty.entries.forEach { d ->
            val chart = ChartBuilder.build(onsets, durationMs, d)
            val nps = chart.notes.size * 1000f / durationMs
            val laneUse = (0 until LANES).map { l -> chart.notes.count { it.lane == l } }
            println("$d: ${chart.notes.size} notas (${"%.2f".format(nps)}/s), largas ${chart.notes.count { it.isHold }}, carriles $laneUse")
        }
    }
}
