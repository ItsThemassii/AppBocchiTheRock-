package com.bocchifan.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.bocchifan.app.audio.cleanTrackName
import com.bocchifan.app.game.Difficulty
import com.bocchifan.app.game.GameResult
import com.bocchifan.app.game.Rank

/** Una canción jugable. [path] es su ruta dentro de assets (canciones/...). */
data class Song(val path: String, val title: String, val album: String?)

/**
 * Canciones del juego: archivos de `contenido/canciones`. Si están dentro de una subcarpeta,
 * el nombre de la subcarpeta es el álbum.
 */
object SongRepository {
    private const val DIR = "canciones"
    private val AUDIO = setOf("mp3", "ogg", "m4a", "wav", "aac", "flac")

    @Volatile
    private var cache: List<Song>? = null

    fun all(context: Context): List<Song> = cache ?: synchronized(this) {
        cache ?: scan(context).also { cache = it }
    }

    fun byPath(context: Context, path: String): Song? = all(context).firstOrNull { it.path == path }

    private fun scan(context: Context): List<Song> {
        val assets = context.assets
        val songs = ArrayList<Song>()
        for (entry in assets.list(DIR).orEmpty().sorted()) {
            if (isAudio(entry)) {
                // Algunos sistemas devuelven rutas completas ("Álbum/canción.mp3").
                val album = entry.substringBeforeLast('/', "").ifEmpty { null }
                songs += Song("$DIR/$entry", cleanTrackName(entry.substringAfterLast('/')), album)
            } else {
                val inner = assets.list("$DIR/$entry").orEmpty()
                inner.filter(::isAudio).sorted().forEach { songs += Song("$DIR/$entry/$it", cleanTrackName(it), entry) }
            }
        }
        return songs
    }

    private fun isAudio(name: String) = name.substringAfterLast('.', "").lowercase() in AUDIO
}

/** Mejor resultado guardado de una canción en una dificultad. */
data class Record(val score: Int, val rank: Rank, val fullCombo: Boolean, val allPerfect: Boolean)

object GameRecords {
    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences("records", Context.MODE_PRIVATE)

    private fun key(path: String, d: Difficulty) = "$path|${d.name}"

    fun get(context: Context, path: String, d: Difficulty): Record? {
        val p = prefs(context)
        val k = key(path, d)
        if (!p.contains("$k|score")) return null
        return Record(
            score = p.getInt("$k|score", 0),
            rank = runCatching { Rank.valueOf(p.getString("$k|rank", "D")!!) }.getOrDefault(Rank.D),
            fullCombo = p.getBoolean("$k|fc", false),
            allPerfect = p.getBoolean("$k|ap", false),
        )
    }

    /** Guarda si es mejor que el récord anterior. Devuelve true si es un récord nuevo. */
    fun save(context: Context, path: String, d: Difficulty, result: GameResult): Boolean {
        if (result.failed) return false
        val old = get(context, path, d)
        val k = key(path, d)
        val newBest = old == null || result.score > old.score
        prefs(context).edit().apply {
            if (newBest) {
                putInt("$k|score", result.score)
                putString("$k|rank", result.rank.name)
            }
            putBoolean("$k|fc", result.fullCombo || old?.fullCombo == true)
            putBoolean("$k|ap", result.allPerfect || old?.allPerfect == true)
        }.apply()
        return newBest
    }
}

/** Ajustes del juego guardados en el teléfono. */
object GameSettings {
    private lateinit var prefs: SharedPreferences

    private val _speed = mutableIntStateOf(6)
    private val _offset = mutableIntStateOf(0)
    private val _tapSound = mutableStateOf(true)
    private val _difficulty = mutableStateOf(Difficulty.Normal)
    private val _volume = mutableFloatStateOf(1f)

    /** Velocidad de las notas, 1 (lenta) a 10 (rápida). */
    val speed: State<Int> = _speed

    /** Ajuste de sincronía en ms: positivo si las notas llegan antes que el sonido. */
    val offsetMs: State<Int> = _offset
    val tapSound: State<Boolean> = _tapSound
    val difficulty: State<Difficulty> = _difficulty
    val songVolume: State<Float> = _volume

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("game", Context.MODE_PRIVATE)
        _speed.intValue = prefs.getInt("speed", 6)
        _offset.intValue = prefs.getInt("offset", 0)
        _tapSound.value = prefs.getBoolean("tap", true)
        _difficulty.value = runCatching { Difficulty.valueOf(prefs.getString("difficulty", "Normal")!!) }.getOrDefault(Difficulty.Normal)
        _volume.floatValue = prefs.getFloat("songVolume", 1f)
    }

    /** Tiempo (ms) que tarda una nota desde el fondo hasta la línea. */
    val approachMs: Int get() = 2400 - (_speed.intValue - 1) * 200

    fun setSpeed(v: Int) { _speed.intValue = v.coerceIn(1, 10); save { putInt("speed", _speed.intValue) } }
    fun setOffset(v: Int) { _offset.intValue = v.coerceIn(-300, 300); save { putInt("offset", _offset.intValue) } }
    fun setTapSound(v: Boolean) { _tapSound.value = v; save { putBoolean("tap", v) } }
    fun setDifficulty(d: Difficulty) { _difficulty.value = d; save { putString("difficulty", d.name) } }
    fun setSongVolume(v: Float) { _volume.floatValue = v; save { putFloat("songVolume", v) } }

    private fun save(block: SharedPreferences.Editor.() -> Unit) {
        if (::prefs.isInitialized) prefs.edit().apply(block).apply()
    }
}
