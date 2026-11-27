package com.bocchifan.app.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.bocchifan.app.R
import kotlin.math.abs

/**
 * Reloj de la canción: sabe en qué milisegundo de la canción estamos.
 *
 * Empieza en negativo (cuenta atrás antes de la música), arranca el audio al llegar a 0 y
 * suaviza la posición que da el reproductor (que avanza a saltos) para que las notas se muevan fluidas.
 */
class SongClock(private val player: MediaPlayer, leadInMs: Int) {
    private var estimate = -leadInMs.toDouble()
    private var lastNanos = System.nanoTime()
    private var musicStarted = false
    var paused = false
        private set

    fun tick(): Int {
        val now = System.nanoTime()
        if (!paused) estimate += (now - lastNanos) / 1_000_000.0
        lastNanos = now
        if (!paused) {
            if (!musicStarted && estimate >= 0) {
                player.start()
                musicStarted = true
            } else if (musicStarted && player.isPlaying) {
                val error = player.currentPosition - estimate
                if (abs(error) > 120) estimate = player.currentPosition.toDouble() else estimate += error * 0.06
            }
        }
        return estimate.toInt()
    }

    fun pause() {
        if (paused) return
        tick()
        paused = true
        if (musicStarted && player.isPlaying) player.pause()
    }

    fun resume() {
        if (!paused) return
        paused = false
        lastNanos = System.nanoTime()
        if (musicStarted) player.start()
    }
}

/** Todo lo de una partida: chart, reglas, audio, reloj y sonido de toque. */
class GameSession(
    context: Context,
    val chart: Chart,
    assetPath: String,
    songVolume: Float,
    private val tapSoundEnabled: Boolean,
) {
    val engine = GameEngine(chart)
    val player = MediaPlayer()
    val clock: SongClock
    var songCompleted = false
        private set

    /** Dedos apoyados en cada carril (para iluminar el carril pulsado). */
    val pressed = IntArray(LANES)

    /** Últimos juicios, para dibujar destellos. */
    val recentHits = ArrayDeque<HitEvent>()

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val tapId = soundPool.load(context, R.raw.tap, 1)

    init {
        context.assets.openFd(assetPath).use { afd ->
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        }
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        player.setVolume(songVolume, songVolume)
        player.setOnCompletionListener { songCompleted = true }
        player.prepare()
        clock = SongClock(player, leadInMs = 1500)
    }

    /** Fin de la última nota (incluidas las largas). */
    val lastNoteEndMs: Int = chart.notes.maxOfOrNull { it.endMs ?: it.timeMs } ?: 0

    fun press(lane: Int, nowMs: Int) {
        pressed[lane]++
        val j = engine.press(lane, nowMs)
        if (j != null && j != Judgment.Miss && tapSoundEnabled) soundPool.play(tapId, 0.7f, 0.7f, 1, 0, 1f)
    }

    fun release(lane: Int, nowMs: Int) {
        pressed[lane] = (pressed[lane] - 1).coerceAtLeast(0)
        if (pressed[lane] == 0) engine.release(lane, nowMs)
    }

    /** Pasa los eventos nuevos del motor a la lista de destellos (máximo 24). */
    fun collectHits() {
        if (engine.events.isEmpty()) return
        recentHits.addAll(engine.events)
        engine.events.clear()
        while (recentHits.size > 24) recentHits.removeFirst()
    }

    fun stopMusic() {
        runCatching { if (player.isPlaying) player.pause() }
    }

    fun release() {
        runCatching { player.release() }
        soundPool.release()
    }
}
