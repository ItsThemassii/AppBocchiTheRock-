package com.bocchifan.app.game

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Analiza una canción del juego (decodifica el audio y detecta los golpes) y guarda el
 * resultado en la memoria del teléfono, para que la próxima vez cargue al instante.
 */
object SongAnalysis {
    private const val TAG = "SongAnalysis"
    private const val TARGET_RATE = 22050
    private const val VERSION = 1 // súbelo si cambia el analizador, para regenerar los charts

    data class Result(val onsets: List<Onset>, val durationMs: Int)

    fun load(context: Context, assetPath: String, onProgress: (Float) -> Unit): Result {
        val cacheFile = cacheFile(context, assetPath)
        readCache(cacheFile)?.let { return it }

        val (samples, rate) = decode(context, assetPath) { onProgress(it * 0.6f) }
        val onsets = OnsetAnalyzer.analyze(samples, rate) { onProgress(0.6f + it * 0.4f) }
        val result = Result(onsets, (samples.size * 1000L / rate).toInt())
        writeCache(cacheFile, result)
        Log.i(TAG, "$assetPath: ${onsets.size} golpes, ${result.durationMs} ms")
        return result
    }

    private fun cacheFile(context: Context, assetPath: String): File {
        val size = runCatching { context.assets.openFd(assetPath).use { it.length } }.getOrDefault(0L)
        val digest = MessageDigest.getInstance("SHA-1").digest("$VERSION|$assetPath|$size".toByteArray())
        val name = digest.joinToString("") { "%02x".format(it) }
        return File(File(context.filesDir, "analisis").apply { mkdirs() }, "$name.json")
    }

    private fun readCache(file: File): Result? = try {
        if (!file.exists()) null else {
            val o = JSONObject(file.readText())
            val arr = o.getJSONArray("o")
            val onsets = (0 until arr.length()).map {
                val a = arr.getJSONArray(it)
                Onset(a.getInt(0), a.getDouble(1).toFloat(), a.getDouble(2).toFloat(), a.getInt(3))
            }
            Result(onsets, o.getInt("d"))
        }
    } catch (e: Exception) {
        Log.w(TAG, "Caché inválida, se vuelve a analizar", e)
        null
    }

    private fun writeCache(file: File, result: Result) {
        val arr = JSONArray()
        result.onsets.forEach {
            arr.put(JSONArray().put(it.timeMs).put(it.strength.toDouble()).put(it.pitch.toDouble()).put(it.sustainMs))
        }
        file.writeText(JSONObject().put("d", result.durationMs).put("o", arr).toString())
    }

    /** Decodifica el audio a mono, reduciendo la frecuencia a ~22 kHz (promediando muestras). */
    private fun decode(context: Context, assetPath: String, onProgress: (Float) -> Unit): Pair<FloatArray, Int> {
        val extractor = MediaExtractor()
        context.assets.openFd(assetPath).use { afd ->
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        }
        val track = (0 until extractor.trackCount).first {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var floatPcm = false
        var step = maxOf(1, sampleRate / TARGET_RATE)

        var out = FloatArray(1 shl 20)
        var count = 0
        var acc = 0f
        var accN = 0

        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                            if (durationUs > 0) onProgress((extractor.sampleTime.toFloat() / durationUs).coerceIn(0f, 1f))
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val f = codec.outputFormat
                    sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    floatPcm = f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        f.getInteger(MediaFormat.KEY_PCM_ENCODING) == android.media.AudioFormat.ENCODING_PCM_FLOAT
                    step = maxOf(1, sampleRate / TARGET_RATE)
                } else if (outIdx >= 0) {
                    val buf = codec.getOutputBuffer(outIdx)!!
                    buf.position(info.offset)
                    buf.limit(info.offset + info.size)
                    buf.order(ByteOrder.LITTLE_ENDIAN)
                    if (floatPcm) {
                        val fb = buf.asFloatBuffer()
                        while (fb.remaining() >= channels) {
                            var s = 0f
                            repeat(channels) { s += fb.get() }
                            acc += s / channels
                            if (++accN == step) {
                                if (count == out.size) out = out.copyOf(out.size * 2)
                                out[count++] = acc / step; acc = 0f; accN = 0
                            }
                        }
                    } else {
                        val sb = buf.asShortBuffer()
                        while (sb.remaining() >= channels) {
                            var s = 0f
                            repeat(channels) { s += sb.get() / 32768f }
                            acc += s / channels
                            if (++accN == step) {
                                if (count == out.size) out = out.copyOf(out.size * 2)
                                out[count++] = acc / step; acc = 0f; accN = 0
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }
        onProgress(1f)
        return out.copyOf(count) to sampleRate / step
    }
}
