package com.bocchifan.app.audio

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * Música de fondo (OST) de toda la app.
 *
 * Lee los audios de la carpeta `contenido/musica` del proyecto (empaquetada como assets/musica),
 * los mezcla y los reproduce uno tras otro. Se pausa cuando la app pasa a segundo plano.
 */
object MusicPlayer {
    private const val TAG = "MusicPlayer"
    private const val DIR = "musica"
    private val AUDIO_EXTENSIONS = setOf("mp3", "ogg", "m4a", "wav", "aac", "flac")

    private lateinit var assets: AssetManager
    private lateinit var prefs: SharedPreferences

    private var player: MediaPlayer? = null
    private var playlist: List<String> = emptyList()
    private var index = 0
    private var inForeground = false
    private var consecutiveFailures = 0

    private val _currentTrack = mutableStateOf<String?>(null)
    private val _enabled = mutableStateOf(true)
    private val _volume = mutableFloatStateOf(0.6f)

    /** Nombre de la pista que suena (o la última preparada); null si no hay música. */
    val currentTrack: State<String?> = _currentTrack
    val enabled: State<Boolean> = _enabled
    val volume: State<Float> = _volume

    val hasTracks: Boolean get() = playlist.isNotEmpty()

    fun init(context: Context) {
        if (::assets.isInitialized) return
        assets = context.applicationContext.assets
        prefs = context.applicationContext.getSharedPreferences("music", Context.MODE_PRIVATE)
        _enabled.value = prefs.getBoolean("enabled", true)
        _volume.floatValue = prefs.getFloat("volume", 0.6f)
        playlist = (assets.list(DIR) ?: emptyArray())
            .filter { it.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS }
        weights = playlist.map { file -> FAVORITES[cleanTrackName(file).lowercase()] ?: 1.0 }
        index = pickNext(-1)
        Log.i(TAG, "Pistas encontradas: ${playlist.size}")
    }

    /** Pistas que suenan un poco más a menudo (peso 1 = normal). */
    private val FAVORITES = mapOf("i'm not sure" to 2.0)
    private var weights: List<Double> = emptyList()

    /** Elige la siguiente pista al azar según su peso, sin repetir la actual. */
    private fun pickNext(current: Int): Int {
        if (playlist.size <= 1) return 0
        val total = weights.indices.filter { it != current }.sumOf { weights[it] }
        var r = kotlin.random.Random.nextDouble(total)
        for (i in weights.indices) {
            if (i == current) continue
            r -= weights[i]
            if (r <= 0) return i
        }
        return weights.indices.last { it != current }
    }

    fun onForeground() {
        inForeground = true
        if (_enabled.value && !suspended) resumeOrStart()
    }

    fun onBackground() {
        inForeground = false
        player?.takeIf { it.isPlaying }?.pause()
    }

    fun setEnabled(on: Boolean) {
        _enabled.value = on
        prefs.edit().putBoolean("enabled", on).apply()
        if (on && inForeground && !suspended) resumeOrStart() else player?.takeIf { it.isPlaying }?.pause()
    }

    fun setVolume(value: Float) {
        _volume.floatValue = value
        player?.setVolume(value, value)
        prefs.edit().putFloat("volume", value).apply()
    }

    fun next() {
        if (playlist.isEmpty()) return
        index = pickNext(index)
        play(index)
    }

    private fun resumeOrStart() {
        val current = player
        when {
            current != null -> if (!current.isPlaying) current.start()
            playlist.isNotEmpty() -> play(index)
        }
    }

    private fun play(i: Int) {
        player?.release()
        player = null
        val file = playlist[i]
        try {
            val mp = MediaPlayer()
            assets.openFd("$DIR/$file").use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            mp.setVolume(_volume.floatValue, _volume.floatValue)
            mp.setOnCompletionListener { next() }
            mp.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "Error al reproducir $file ($what/$extra)")
                skipAfterFailure()
                true
            }
            mp.prepare()
            player = mp
            _currentTrack.value = displayName(file)
            consecutiveFailures = 0
            if (inForeground && _enabled.value && !suspended) mp.start()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo abrir $file", e)
            skipAfterFailure()
        }
    }

    /** Pasa a la siguiente pista, sin entrar en bucle si ninguna se puede reproducir. */
    private fun skipAfterFailure() {
        consecutiveFailures++
        if (consecutiveFailures < playlist.size) {
            next()
        } else {
            player?.release()
            player = null
            _currentTrack.value = null
        }
    }

    internal fun displayName(file: String): String = cleanTrackName(file)

    /** El juego pausa la música de fondo mientras suena su canción. */
    var suspended: Boolean = false
        set(value) {
            field = value
            if (value) player?.takeIf { it.isPlaying }?.pause()
            else if (inForeground && _enabled.value) resumeOrStart()
        }
}

/**
 * Nombre bonito a partir del nombre del archivo:
 * "01 - Seishun Complex.mp3" → "Seishun Complex".
 * "Dizzy — Bocchi the Rock! [OST] [abc123].mp3" → "Dizzy".
 */
fun cleanTrackName(file: String): String = file
    .substringBeforeLast('.')
    .substringBefore(" — ")
    .replace(Regex("""\s*\[[^\]]*\]"""), "")
    .replace('_', ' ')
    .replace(Regex("""^\s*\d+\s*[-.)_]?\s*"""), "")
    .trim()
    .ifEmpty { file }
