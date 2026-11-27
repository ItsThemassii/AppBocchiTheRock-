package com.bocchifan.app.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Detecta los golpes (onsets) de una canción a partir de su audio mono.
 *
 * Usa "flujo espectral": compara el espectro de cada trozo de ~46 ms con el anterior y marca
 * los momentos en que aparece energía nueva (un golpe de batería, una nota de guitarra...).
 * Es Kotlin puro (sin Android) para poder probarlo en el ordenador.
 */
object OnsetAnalyzer {
    private const val FRAME = 1024
    private const val HOP = 256

    fun analyze(samples: FloatArray, sampleRate: Int, onProgress: (Float) -> Unit = {}): List<Onset> {
        val frames = (samples.size - FRAME) / HOP
        if (frames < 8) return emptyList()

        val window = FloatArray(FRAME) { (0.5 - 0.5 * cos(2 * PI * it / (FRAME - 1))).toFloat() }
        val maxBin = min(FRAME / 2, (8000f * FRAME / sampleRate).toInt())
        val binHz = sampleRate.toFloat() / FRAME

        val flux = FloatArray(frames)
        val centroid = FloatArray(frames)
        val energy = FloatArray(frames)
        var prev = FloatArray(maxBin)
        val re = FloatArray(FRAME)
        val im = FloatArray(FRAME)

        for (f in 0 until frames) {
            val start = f * HOP
            var e = 0f
            for (i in 0 until FRAME) {
                val s = samples[start + i]
                re[i] = s * window[i]
                im[i] = 0f
                e += s * s
            }
            energy[f] = sqrt(e / FRAME)
            fft(re, im)

            val mag = FloatArray(maxBin)
            var sum = 0f
            var weighted = 0f
            var fl = 0f
            for (k in 1 until maxBin) {
                val m = ln(1f + 100f * sqrt(re[k] * re[k] + im[k] * im[k]))
                mag[k] = m
                val d = m - prev[k]
                if (d > 0) fl += d
                sum += m
                weighted += m * ln(1f + k * binHz)
            }
            flux[f] = fl
            centroid[f] = if (sum > 0) weighted / sum else 0f
            prev = mag
            if (f % 512 == 0) onProgress(f.toFloat() / frames)
        }

        return pickPeaks(flux, centroid, energy, sampleRate)
    }

    private fun pickPeaks(flux: FloatArray, centroid: FloatArray, energy: FloatArray, sampleRate: Int): List<Onset> {
        val n = flux.size
        val msPerFrame = HOP * 1000f / sampleRate
        val local = (300f / msPerFrame).roundToInt().coerceAtLeast(4) // ventana de ±300 ms
        val peakHalf = (45f / msPerFrame).roundToInt().coerceAtLeast(2) // máximo local en ±45 ms
        val minGap = (60f / msPerFrame).roundToInt().coerceAtLeast(1)

        // Media móvil (suma acumulada) para un umbral que se adapta al volumen de cada parte.
        val prefix = DoubleArray(n + 1)
        for (i in 0 until n) prefix[i + 1] = prefix[i] + flux[i]
        val mean = flux.average().toFloat()
        val std = sqrt(flux.map { (it - mean) * (it - mean) }.average()).toFloat().coerceAtLeast(1e-6f)

        // Rango de "tono" (centroide) real de la canción, para repartir bien entre carriles.
        val sortedCentroid = centroid.sorted()
        val cLow = sortedCentroid[(n * 0.05f).toInt()]
        val cHigh = sortedCentroid[(n * 0.95f).toInt().coerceAtMost(n - 1)].coerceAtLeast(cLow + 1e-3f)

        val onsets = ArrayList<Onset>()
        var last = -minGap
        for (i in 1 until n - 1) {
            val a = max(0, i - local)
            val b = min(n, i + local + 1)
            val localMean = ((prefix[b] - prefix[a]) / (b - a)).toFloat()
            val threshold = localMean + 0.35f * std
            if (flux[i] < threshold || i - last < minGap) continue
            var isPeak = true
            for (j in max(0, i - peakHalf)..min(n - 1, i + peakHalf)) {
                if (flux[j] > flux[i]) { isPeak = false; break }
            }
            if (!isPeak) continue

            // El golpe empieza un poco antes del pico del flujo: retrocede mientras el flujo sube.
            var startFrame = i
            while (startFrame > 0 && i - startFrame < peakHalf && flux[startFrame - 1] < flux[startFrame] &&
                flux[startFrame - 1] > localMean
            ) startFrame--
            // +16 ms: corrección medida con golpes de tiempo conocido (ver GameLogicTest).
            val timeMs = ((startFrame * HOP + FRAME / 2) * 1000f / sampleRate).roundToInt() + 16

            onsets += Onset(
                timeMs = timeMs,
                strength = (flux[i] - localMean) / std,
                pitch = ((centroid[i] - cLow) / (cHigh - cLow)).coerceIn(0f, 1f),
                sustainMs = sustain(energy, i, msPerFrame),
            )
            last = i
        }
        return onsets
    }

    /** Cuánto se mantiene la energía después del golpe (para notas largas). */
    private fun sustain(energy: FloatArray, at: Int, msPerFrame: Float): Int {
        val peakEnd = min(energy.size - 1, at + 3)
        var peak = 0f
        for (j in at..peakEnd) peak = max(peak, energy[j])
        if (peak <= 0f) return 0
        val maxFrames = (2000f / msPerFrame).toInt()
        var j = peakEnd
        while (j < energy.size - 1 && j - at < maxFrames && energy[j] > peak * 0.55f) j++
        return ((j - at) * msPerFrame).roundToInt()
    }

    /** FFT radix-2 en el sitio (tamaño potencia de 2). */
    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2 * PI / len
            val wRe = cos(ang).toFloat()
            val wIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f
                var curIm = 0f
                for (k in 0 until len / 2) {
                    val aRe = re[i + k]
                    val aIm = im[i + k]
                    val bRe = re[i + k + len / 2] * curRe - im[i + k + len / 2] * curIm
                    val bIm = re[i + k + len / 2] * curIm + im[i + k + len / 2] * curRe
                    re[i + k] = aRe + bRe
                    im[i + k] = aIm + bIm
                    re[i + k + len / 2] = aRe - bRe
                    im[i + k + len / 2] = aIm - bIm
                    val nRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}
