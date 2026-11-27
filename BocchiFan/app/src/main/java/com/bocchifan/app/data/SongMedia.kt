package com.bocchifan.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Portadas incrustadas en los MP3 (la imagen que trae cada canción). */
object SongCovers {
    private val cache = ConcurrentHashMap<String, Result<ImageBitmap?>>()

    fun load(context: Context, path: String): ImageBitmap? = cache.getOrPut(path) {
        runCatching {
            val r = MediaMetadataRetriever()
            try {
                context.assets.openFd(path).use { afd -> r.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length) }
                r.embeddedPicture
                    ?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                    ?.let(::trimBars)
                    ?.asImageBitmap()
            } finally {
                r.release()
            }
        }.onFailure { Log.w("SongCovers", "Sin portada: $path", it) }
    }.getOrNull()
}

/**
 * Quita las franjas de color liso de los bordes (muchas portadas son miniaturas de video
 * con barras oscuras a los lados).
 */
internal fun trimBars(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    if (w < 16 || h < 16) return src
    val ref = src.getPixel(0, h / 2)
    fun differs(p: Int): Boolean {
        val dr = ((p shr 16) and 0xFF) - ((ref shr 16) and 0xFF)
        val dg = ((p shr 8) and 0xFF) - ((ref shr 8) and 0xFF)
        val db = (p and 0xFF) - (ref and 0xFF)
        return dr * dr + dg * dg + db * db > 30 * 30
    }
    // Una columna/fila es "contenido" si más del 8 % de sus píxeles difieren del color del borde.
    fun columnHasContent(x: Int) = (0 until h step 3).count { differs(src.getPixel(x, it)) } > h / 3 * 0.08f
    fun rowHasContent(y: Int) = (0 until w step 3).count { differs(src.getPixel(it, y)) } > w / 3 * 0.08f
    var left = 0
    while (left < w / 3 && !columnHasContent(left)) left++
    var right = w - 1
    while (right > w * 2 / 3 && !columnHasContent(right)) right--
    var top = 0
    while (top < h / 3 && !rowHasContent(top)) top++
    var bottom = h - 1
    while (bottom > h * 2 / 3 && !rowHasContent(bottom)) bottom--
    val cw = right - left + 1
    val ch = bottom - top + 1
    // Solo recorta si de verdad había barras (más de un 3 % del tamaño).
    if (cw >= w * 0.97f && ch >= h * 0.97f) return src
    return Bitmap.createBitmap(src, left, top, cw, ch)
}

@Composable
fun rememberSongCover(path: String): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { SongCovers.load(context, path) }
    }
}

/** Una línea de la letra; [timeMs] es null si la letra no está sincronizada. */
data class LyricLine(val timeMs: Int?, val text: String)

data class SongLyrics(val lines: List<LyricLine>) {
    val synced: Boolean get() = lines.any { it.timeMs != null }

    /** Índice de la línea que suena en [positionMs] (-1 antes de la primera). */
    fun currentIndex(positionMs: Int): Int {
        if (!synced) return -1
        var current = -1
        for (i in lines.indices) {
            val t = lines[i].timeMs ?: continue
            if (t <= positionMs) current = i else break
        }
        return current
    }
}

/**
 * Letras: un archivo con el mismo nombre que la canción, en su misma carpeta:
 *   - `Canción.lrc` → letra sincronizada (formato LRC: `[mm:ss.xx] texto`)
 *   - `Canción.txt` → letra sin sincronizar
 */
object LyricsRepository {
    private val cache = ConcurrentHashMap<String, Result<SongLyrics?>>()
    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val offsetTag = Regex("""\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)
    private val metaTag = Regex("""^\[[a-zA-Z]+:.*]$""")

    fun has(context: Context, songPath: String): Boolean = load(context, songPath) != null

    /** Archivo donde se guarda la letra que pega el usuario (memoria interna de la app). */
    private fun userFile(context: Context, songPath: String): java.io.File {
        val safe = songPath.substringAfterLast('/').substringBeforeLast('.').replace(Regex("""[^\p{L}\p{N} ._-]"""), "_")
        return java.io.File(java.io.File(context.filesDir, "letras").apply { mkdirs() }, "$safe.txt")
    }

    /** Texto guardado por el usuario para esta canción (para editarlo), o null. */
    fun userText(context: Context, songPath: String): String? =
        userFile(context, songPath).takeIf { it.exists() }?.readText()

    /** Guarda la letra pegada. Si trae tiempos [mm:ss.xx] se usará como letra sincronizada. */
    fun save(context: Context, songPath: String, text: String) {
        val file = userFile(context, songPath)
        if (text.isBlank()) file.delete() else file.writeText(text.trim())
        cache.remove(songPath)
    }

    fun delete(context: Context, songPath: String) = save(context, songPath, "")

    /** Convierte texto pegado en letra: sincronizada si tiene tiempos, si no, líneas simples. */
    internal fun parseText(text: String): SongLyrics =
        if (timeTag.containsMatchIn(text)) parseLrc(text)
        else SongLyrics(text.lines().map { LyricLine(null, it.trim()) }.dropWhile { it.text.isEmpty() }.dropLastWhile { it.text.isEmpty() })

    fun load(context: Context, songPath: String): SongLyrics? = cache.getOrPut(songPath) {
        runCatching {
            // 1) Letra pegada por el usuario en la app.
            userText(context, songPath)?.let { return@runCatching parseText(it) }
            // 2) Archivo .lrc / .txt incluido junto a la canción.
            val base = songPath.substringBeforeLast('.')
            val folder = base.substringBeforeLast('/', "")
            val name = base.substringAfterLast('/')
            val files = context.assets.list(folder).orEmpty()
            val lrc = files.firstOrNull { it.equals("$name.lrc", ignoreCase = true) }
            val txt = files.firstOrNull { it.equals("$name.txt", ignoreCase = true) }
            when {
                lrc != null -> parseLrc(read(context, "$folder/$lrc"))
                txt != null -> SongLyrics(read(context, "$folder/$txt").lines().map { LyricLine(null, it.trim()) })
                else -> null
            }
        }
    }.getOrNull()

    private fun read(context: Context, path: String) =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }.removePrefix("﻿")

    internal fun parseLrc(text: String): SongLyrics {
        var offset = 0
        val lines = ArrayList<LyricLine>()
        for (raw in text.lines()) {
            val line = raw.trim()
            offsetTag.find(line)?.let { offset = it.groupValues[1].toInt(); return@let }
            val stamps = timeTag.findAll(line).toList()
            if (stamps.isEmpty()) {
                if (line.isNotEmpty() && !metaTag.matches(line)) lines += LyricLine(null, line)
                continue
            }
            val content = line.substring(stamps.last().range.last + 1).trim()
            for (m in stamps) {
                val (min, sec, frac) = m.destructured
                val fracMs = when (frac.length) { 0 -> 0; 1 -> frac.toInt() * 100; 2 -> frac.toInt() * 10; else -> frac.take(3).toInt() }
                // En LRC, [offset:+N] adelanta la letra N ms.
                lines += LyricLine(min.toInt() * 60_000 + sec.toInt() * 1000 + fracMs - offset, content)
            }
        }
        return SongLyrics(lines.sortedBy { it.timeMs ?: Int.MAX_VALUE })
    }
}
