package com.bocchifan.app.ui.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.bocchifan.app.R
import com.bocchifan.app.audio.MusicPlayer
import com.bocchifan.app.data.LyricsRepository
import com.bocchifan.app.data.Song
import com.bocchifan.app.data.SongLyrics
import com.bocchifan.app.data.SongRepository
import com.bocchifan.app.data.rememberSongCover
import com.bocchifan.app.ui.components.GuitarPickShape
import com.bocchifan.app.ui.components.StickerCard
import com.bocchifan.app.ui.components.SystemBarsAppearance
import com.bocchifan.app.ui.components.UiSound
import com.bocchifan.app.ui.components.UiSounds
import com.bocchifan.app.ui.components.bocchiClickable
import com.bocchifan.app.ui.components.rememberAssetImage
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.NijikaYellow
import com.bocchifan.app.ui.theme.RyoBlue
import com.bocchifan.app.ui.theme.StageDark
import com.bocchifan.app.ui.theme.KitaRed
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reproductor sencillo de una canción a la vez (MediaPlayer). */
private class SongPlayback(private val context: Context) {
    private var player: MediaPlayer? = null
    var onCompletion: () -> Unit = {}

    fun load(path: String, autoplay: Boolean) {
        release()
        val mp = MediaPlayer()
        context.assets.openFd(path).use { afd -> mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length) }
        mp.setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
        )
        mp.setOnCompletionListener { onCompletion() }
        mp.prepare()
        if (autoplay) mp.start()
        player = mp
    }

    val isPlaying: Boolean get() = runCatching { player?.isPlaying == true }.getOrDefault(false)
    val position: Int get() = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
    val duration: Int get() = runCatching { player?.duration ?: 0 }.getOrDefault(0)

    fun toggle() { player?.let { if (it.isPlaying) it.pause() else it.start() } }
    fun pause() { runCatching { player?.takeIf { it.isPlaying }?.pause() } }
    fun seek(ms: Int) { player?.seekTo(ms) }
    fun release() { runCatching { player?.release() }; player = null }
}

private enum class RepeatMode { Off, All, One }

/**
 * Reproductor de las canciones del álbum: portada del MP3, controles, barra de progreso
 * y la opción "Letra" (sincronizada si hay archivo .lrc).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(startPath: String, onBack: () -> Unit) {
    SystemBarsAppearance(lightBackground = false)
    val context = LocalContext.current
    val songs = remember { SongRepository.all(context) }
    var index by remember { mutableIntStateOf(songs.indexOfFirst { it.path == startPath }.coerceAtLeast(0)) }
    val song = songs.getOrNull(index) ?: run { LaunchedEffect(Unit) { onBack() }; return }

    val playback = remember { SongPlayback(context) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var showLyrics by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(RepeatMode.Off) }
    var seeking by remember { mutableStateOf<Float?>(null) }

    fun go(newIndex: Int) {
        index = (newIndex + songs.size) % songs.size
    }
    fun nextIndex(): Int = if (shuffle && songs.size > 1) {
        var n: Int
        do { n = songs.indices.random() } while (n == index)
        n
    } else index + 1

    // El OST de fondo se calla mientras está abierto el reproductor.
    DisposableEffect(Unit) {
        MusicPlayer.suspended = true
        onDispose {
            playback.release()
            MusicPlayer.suspended = false
        }
    }

    // Cargar la canción actual (y reproducirla).
    LaunchedEffect(song.path) {
        withContext(Dispatchers.IO) { playback.load(song.path, autoplay = true) }
        duration = playback.duration
        playback.onCompletion = {
            when {
                repeat == RepeatMode.One -> { playback.seek(0); playback.toggle() }
                repeat == RepeatMode.All || index + 1 < songs.size || shuffle -> go(nextIndex())
                else -> playing = false
            }
        }
    }

    // Posición en cada cuadro (para que la letra avance fluida).
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            playing = playback.isPlaying
            position = playback.position
        }
    }

    var lyricsVersion by remember { mutableIntStateOf(0) } // sube al guardar/borrar una letra
    var editingLyrics by remember { mutableStateOf(false) }
    val lyrics = remember(song.path, lyricsVersion) { LyricsRepository.load(context, song.path) }
    androidx.activity.compose.BackHandler(enabled = editingLyrics) { editingLyrics = false }
    val cover by rememberSongCover(song.path)
    val tint = remember(cover) { cover?.averageColor() ?: RyoBlue }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tint.darken(0.55f), StageDark, StageDark))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Barra superior: volver, "Reproduciendo" y el botón Letra.
            Row(verticalAlignment = Alignment.CenterVertically) {
                StickerCard(color = Color.White, shadow = 4.dp, showTape = false, sound = UiSound.Back, onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Ink,
                        modifier = Modifier.padding(10.dp).size(24.dp),
                    )
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(stringResource(R.string.player_now_playing).uppercase(), color = BocchiPink, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 2.sp)
                    Text(song.album ?: "", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                StickerCard(
                    color = if (showLyrics) BocchiPink else Color.White,
                    shadow = if (showLyrics) 1.dp else 4.dp,
                    showTape = false,
                    onClick = { showLyrics = !showLyrics },
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lyrics, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.player_lyrics), color = Ink, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Portada o letra.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedContent(showLyrics, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "coverLyrics") { lyricsMode ->
                    if (lyricsMode) {
                        LyricsView(
                            lyrics = lyrics,
                            positionMs = position,
                            onSeek = { ms -> playback.seek(ms); seeking = null },
                            onSearch = { openLyricsSearch(context, song.title, synced = false) },
                            onEdit = { editingLyrics = true },
                        )
                    } else {
                        CoverArt(cover, playing)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(song.title, color = Color.White, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Kessoku Band", color = tint.lighten(), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Progreso.
            val shown = seeking ?: if (duration > 0) position.toFloat() / duration else 0f
            val sliderColors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = BocchiPink,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
            )
            Slider(
                value = shown.coerceIn(0f, 1f),
                onValueChange = { seeking = it },
                onValueChangeFinished = {
                    seeking?.let { playback.seek((it * duration).toInt()) }
                    seeking = null
                    UiSounds.play(UiSound.Tap)
                },
                colors = sliderColors,
                // En lugar de la barrita: una mini Bocchi gusano que se arrastra hacia la derecha.
                thumb = { CrawlingBocchi(crawling = playing && seeking == null) },
                // Barra continua (sin el hueco alrededor de Bocchi), para que se vea lo ya recorrido.
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        colors = sliderColors,
                        thumbTrackGapSize = 0.dp,
                        drawStopIndicator = null,
                    )
                },
            )
            Row {
                Text(formatTime((shown * duration).toInt()), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(formatTime(duration), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(10.dp))

            // Controles clásicos: aleatorio, anterior, reproducir (grande), siguiente y repetir.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ControlIcon(Icons.Rounded.Shuffle, stringResource(R.string.player_shuffle), active = shuffle, size = 26.dp) { shuffle = !shuffle }
                ControlIcon(Icons.Rounded.SkipPrevious, stringResource(R.string.player_previous), size = 40.dp) {
                    if (position > 3000) playback.seek(0) else go(index - 1)
                }
                PlayCircle(playing) { playback.toggle() }
                ControlIcon(Icons.Rounded.SkipNext, stringResource(R.string.player_next), size = 40.dp) { go(nextIndex()) }
                ControlIcon(
                    if (repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    stringResource(R.string.player_repeat),
                    active = repeat != RepeatMode.Off,
                    size = 26.dp,
                ) { repeat = RepeatMode.entries[(repeat.ordinal + 1) % RepeatMode.entries.size] }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (editingLyrics) {
            LyricsEditor(
                songTitle = song.title,
                initial = remember(song.path) { LyricsRepository.userText(context, song.path) ?: "" },
                onSearch = { synced -> openLyricsSearch(context, song.title, synced) },
                onSave = { text ->
                    LyricsRepository.save(context, song.path, text)
                    lyricsVersion++
                    editingLyrics = false
                    showLyrics = true
                },
                onCancel = { editingLyrics = false },
            )
        }
    }
}

/** Abre el navegador buscando la letra de la canción (solo se envía el título). */
private fun openLyricsSearch(context: Context, title: String, synced: Boolean) {
    val query = if (synced) "$title Kessoku Band lrc lyrics" else "$title Kessoku Band letra lyrics"
    val url = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(query, "UTF-8")
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/**
 * Editor de letra: se pega el texto copiado del navegador y se guarda en la app.
 * Si trae tiempos como [00:12.34], se sincroniza con la canción.
 */
@Composable
private fun LyricsEditor(
    songTitle: String,
    initial: String,
    onSearch: (synced: Boolean) -> Unit,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .systemBarsPadding()
            .padding(16.dp),
    ) {
        StickerCard(color = Color.White, showTape = true, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(stringResource(R.string.lyrics_editor_title, songTitle), style = MaterialTheme.typography.titleLarge, color = Ink, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.lyrics_editor_hint), style = MaterialTheme.typography.bodySmall, color = com.bocchifan.app.ui.theme.InkSoft)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction(stringResource(R.string.lyrics_search), NijikaYellow, Modifier.weight(1f)) { onSearch(false) }
                    SmallAction(stringResource(R.string.lyrics_search_synced), RyoBlue.copy(alpha = 0.35f), Modifier.weight(1f)) { onSearch(true) }
                    SmallAction(stringResource(R.string.lyrics_paste_clipboard), BocchiPink, Modifier.weight(1f)) {
                        clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let { text = it }
                    }
                }
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.lyrics_placeholder), color = com.bocchifan.app.ui.theme.InkSoft) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BocchiPinkDeep,
                        unfocusedBorderColor = Ink,
                        cursorColor = BocchiPinkDeep,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallAction(stringResource(R.string.lyrics_cancel), Color.White, Modifier.weight(1f), UiSound.Back, onCancel)
                    if (initial.isNotBlank()) {
                        SmallAction(stringResource(R.string.lyrics_delete), KitaRed.copy(alpha = 0.35f), Modifier.weight(1f), UiSound.Back) { onSave("") }
                    }
                    SmallAction(stringResource(R.string.lyrics_save), BocchiPink, Modifier.weight(1.3f), UiSound.Chord) { onSave(text) }
                }
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, color: Color, modifier: Modifier, sound: UiSound = UiSound.Tap, onClick: () -> Unit) {
    StickerCard(color = color, shadow = 3.dp, showTape = false, sound = sound, onClick = onClick, modifier = modifier) {
        Text(
            label,
            color = Ink,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun CoverArt(cover: ImageBitmap?, playing: Boolean) {
    // La portada "respira" un poco cuando suena.
    val scale by animateFloatAsState(if (playing) 1f else 0.94f, label = "coverScale")
    // Solo la imagen, con su propia forma (cuadrada o panorámica) y esquinas redondeadas.
    if (cover != null) {
        Image(
            cover,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(cover.width.toFloat() / cover.height, matchHeightConstraintsFirst = true)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(16.dp)),
        )
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(16.dp))
                .background(Ink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
        }
    }
}

/**
 * Mini Bocchi gusano (contenido/imagenes/barra_gusano) mirando a la derecha.
 * Mientras suena avanza "como oruga": se estira, se encoge y da saltitos.
 */
@Composable
private fun CrawlingBocchi(crawling: Boolean) {
    // Versión sin gotitas (solo el gusano), recortada con herramientas/BocchiTools.java sticker-main.
    val image by rememberAssetImage("barra_gusano")
    val cycle by rememberInfiniteTransition(label = "crawl").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing)),
        label = "crawlCycle",
    )
    // Fase 0→1: estirarse (más largo y bajito) y volver a encogerse.
    val wave = if (crawling) kotlin.math.sin(cycle * Math.PI.toFloat()) else 0f
    Box(
        Modifier.size(width = 44.dp, height = 28.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val bmp = image
        if (bmp != null) {
            Image(
                bmp,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.85f)
                        // La imagen original mira a la izquierda: se voltea para que avance hacia la derecha.
                        scaleX = -(1f + wave * 0.14f)
                        scaleY = 1f - wave * 0.12f
                        translationY = -wave * 2.dp.toPx() - 4.dp.toPx()
                        rotationZ = -wave * 4f
                    },
            )
        } else {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
private fun LyricsView(
    lyrics: SongLyrics?,
    positionMs: Int,
    onSeek: (Int) -> Unit,
    onSearch: () -> Unit,
    onEdit: () -> Unit,
) {
    if (lyrics == null || lyrics.lines.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.lyrics_none), color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.lyrics_how), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallAction(stringResource(R.string.lyrics_search), NijikaYellow, Modifier.width(150.dp), onClick = onSearch)
                SmallAction(stringResource(R.string.lyrics_paste), BocchiPink, Modifier.width(150.dp), onClick = onEdit)
            }
        }
        return
    }
    val current = lyrics.currentIndex(positionMs)
    val listState = rememberLazyListState()
    // Mantiene la línea actual cerca del centro.
    LaunchedEffect(current) {
        if (current >= 0) listState.animateScrollToItem((current - 2).coerceAtLeast(0))
    }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(lyrics.lines) { i, line ->
            val isCurrent = i == current
            val past = lyrics.synced && i < current
            Text(
                line.text.ifEmpty { "♪" },
                color = when {
                    isCurrent -> Color.White
                    past -> Color.White.copy(alpha = 0.35f)
                    lyrics.synced -> Color.White.copy(alpha = 0.6f)
                    else -> Color.White.copy(alpha = 0.9f)
                },
                fontSize = if (isCurrent) 24.sp else 19.sp,
                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (line.timeMs != null) Modifier.bocchiClickable(sound = UiSound.None) { onSeek(line.timeMs) } else Modifier,
                    ),
            )
        }
    }
        // Botón para editar o cambiar la letra.
        SmallAction(
            stringResource(R.string.lyrics_edit),
            Color.White,
            Modifier.align(Alignment.BottomEnd).width(120.dp),
            onClick = onEdit,
        )
    }
}


private fun formatTime(ms: Int): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun ImageBitmap.averageColor(): Color {
    val bmp = asAndroidBitmapSafe() ?: return RyoBlue
    var r = 0L; var g = 0L; var b = 0L; var n = 0
    val stepX = (bmp.width / 24).coerceAtLeast(1)
    val stepY = (bmp.height / 24).coerceAtLeast(1)
    for (y in 0 until bmp.height step stepY) for (x in 0 until bmp.width step stepX) {
        val p = bmp.getPixel(x, y)
        r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++
    }
    return if (n == 0) RyoBlue else Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
}

private fun ImageBitmap.asAndroidBitmapSafe() = runCatching { asAndroidBitmap() }.getOrNull()

private fun Color.darken(f: Float) = Color(red * f, green * f, blue * f)
private fun Color.lighten() = Color(red + (1 - red) * 0.5f, green + (1 - green) * 0.5f, blue + (1 - blue) * 0.5f)

/** Botón de ícono del reproductor: blanco, o rosa con un puntito debajo si está activo. */
@Composable
private fun ControlIcon(icon: ImageVector, description: String, size: Dp, active: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .bocchiClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (active) BocchiPink else Color.White, modifier = Modifier.size(size))
        if (active) {
            Spacer(Modifier.height(3.dp))
            Box(Modifier.size(5.dp).clip(CircleShape).background(BocchiPink))
        }
    }
}

/** Botón grande y redondo de reproducir/pausar. */
@Composable
private fun PlayCircle(playing: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (playing) 1f else 1.06f, label = "playScale")
    Box(
        Modifier
            .size(76.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(BocchiPink)
            .bocchiClickable(sound = if (playing) UiSound.Back else UiSound.Chord, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = stringResource(if (playing) R.string.player_pause else R.string.player_play),
            tint = Ink,
            modifier = Modifier.size(42.dp),
        )
    }
}
