package com.bocchifan.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Muestra una imagen de assets que puede estar animada (GIF / WebP animado), en bucle.
 * En Android 9+ se anima; en versiones anteriores se ve el primer cuadro.
 */
@Composable
fun AnimatedAssetImage(path: String, modifier: Modifier = Modifier, crop: Boolean = true) {
    val context = LocalContext.current
    val drawable by produceState<Drawable?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { runCatching { loadDrawable(context, path) }.getOrNull() }
    }
    AndroidView(
        factory = { ImageView(it).apply { scaleType = if (crop) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER } },
        update = { view ->
            if (view.drawable !== drawable) {
                view.setImageDrawable(drawable)
                (drawable as? AnimatedImageDrawable)?.apply { repeatCount = AnimatedImageDrawable.REPEAT_INFINITE; start() }
            }
        },
        modifier = modifier,
    )
    DisposableEffect(drawable) {
        onDispose { (drawable as? AnimatedImageDrawable)?.stop() }
    }
}

private fun loadDrawable(context: android.content.Context, path: String): Drawable? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.assets, path))
    } else {
        context.assets.open(path).use { BitmapFactory.decodeStream(it) }
            ?.let { android.graphics.drawable.BitmapDrawable(context.resources, it) }
    }

/**
 * Color de fondo de una imagen: el promedio de sus bordes (primer cuadro).
 * Sirve para que la pantalla alrededor del GIF tenga su mismo tono.
 */
@Composable
fun rememberEdgeColor(path: String, fallback: Color): State<Color> {
    val context = LocalContext.current
    return produceState(initialValue = fallback, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bmp: Bitmap = context.assets.open(path).use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 })
                } ?: return@runCatching fallback
                var r = 0L; var g = 0L; var b = 0L; var n = 0
                fun add(x: Int, y: Int) {
                    val p = bmp.getPixel(x, y)
                    r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++
                }
                val w = bmp.width; val h = bmp.height
                for (x in 0 until w step 2) { add(x, 0); add(x, h - 1) }
                for (y in 0 until h step 2) { add(0, y); add(w - 1, y) }
                bmp.recycle()
                Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
            }.getOrDefault(fallback)
        }
    }
}
