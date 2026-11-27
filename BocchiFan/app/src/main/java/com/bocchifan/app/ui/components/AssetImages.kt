package com.bocchifan.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Imágenes opcionales que pone el usuario en `contenido/imagenes` (assets/imagenes).
 * Si una imagen no existe, se devuelve null y la pantalla usa su diseño sin imagen.
 */
object AssetImages {
    private const val DIR = "imagenes"
    private val EXTENSIONS = listOf("webp", "png", "jpg", "jpeg")
    private const val MAX_SIDE = 1600

    private val cache = ConcurrentHashMap<String, Result<ImageBitmap?>>()

    /** [name] sin extensión, por ejemplo "portada" o "personajes/bocchi". */
    fun load(context: Context, name: String): ImageBitmap? =
        cache.getOrPut(name) { runCatching { decode(context, name) } }.getOrNull()

    private fun decode(context: Context, name: String): ImageBitmap? {
        val assets = context.assets
        val folder = "$DIR/${name.substringBeforeLast('/', "")}".trimEnd('/')
        val base = name.substringAfterLast('/')
        val files = assets.list(folder) ?: return null
        val file = EXTENSIONS.firstNotNullOfOrNull { ext ->
            files.firstOrNull { it.equals("$base.$ext", ignoreCase = true) }
        } ?: return null
        val path = "$folder/$file"

        // Primero solo medidas, para reducir imágenes muy grandes y no gastar memoria.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
    }
}

/** Carga la imagen en segundo plano; mientras tanto (o si no existe) el valor es null. */
@Composable
fun rememberAssetImage(name: String): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, name) {
        value = withContext(Dispatchers.IO) { AssetImages.load(context, name) }
    }
}
