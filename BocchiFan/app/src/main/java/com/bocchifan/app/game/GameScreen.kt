package com.bocchifan.app.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bocchifan.app.R
import com.bocchifan.app.audio.MusicPlayer
import com.bocchifan.app.data.GameRecords
import com.bocchifan.app.data.GameSettings
import com.bocchifan.app.data.SongRepository
import com.bocchifan.app.ui.components.AnimatedAssetImage
import com.bocchifan.app.ui.components.UiSound
import com.bocchifan.app.ui.components.UiSounds
import com.bocchifan.app.ui.components.bocchiClickable
import com.bocchifan.app.ui.components.StickerCard
import com.bocchifan.app.ui.components.rememberEdgeColor
import com.bocchifan.app.ui.theme.Paper
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.luminance
import com.bocchifan.app.ui.placeholder.MangoBox
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.InkSoft
import com.bocchifan.app.ui.theme.KitaRed
import com.bocchifan.app.ui.theme.NijikaYellow
import com.bocchifan.app.ui.theme.StageDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.text.NumberFormat

private sealed interface Phase {
    data class Loading(val progress: Float) : Phase
    data object Playing : Phase
    data object Paused : Phase
    data class Finished(val result: GameResult, val newRecord: Boolean) : Phase
    /** Festejo de FULL COMBO / ALL PERFECT antes de los resultados. */
    data class Celebrating(val result: GameResult) : Phase
    data object Error : Phase
}

/** Partida completa: carga/analiza la canción, juega en horizontal y muestra los resultados. */
@Composable
fun GameScreen(songPath: String, difficulty: Difficulty, onExit: () -> Unit) {
    val context = LocalContext.current
    val song = remember(songPath) { SongRepository.byPath(context, songPath) }
    val title = song?.title ?: songPath.substringAfterLast('/')

    LandscapeImmersive()
    DisposableEffect(Unit) {
        MusicPlayer.suspended = true
        onDispose { MusicPlayer.suspended = false }
    }

    var attempt by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf<Phase>(Phase.Loading(0f)) }
    var session by remember { mutableStateOf<GameSession?>(null) }
    val time = remember { mutableIntStateOf(-1500) }
    val hud = remember { HudState() }
    val approach = GameSettings.approachMs
    val offset = GameSettings.offsetMs.value

    // Carga (o recarga al reintentar) y bucle de la partida.
    LaunchedEffect(songPath, difficulty, attempt) {
        phase = Phase.Loading(0f)
        hud.reset()
        val loadStart = System.currentTimeMillis()
        val newSession = try {
            val analysis = withContext(Dispatchers.Default) {
                SongAnalysis.load(context, songPath) { p -> phase = Phase.Loading(p) }
            }
            val chart = ChartBuilder.build(analysis.onsets, analysis.durationMs, difficulty)
            withContext(Dispatchers.IO) {
                GameSession(context, chart, songPath, GameSettings.songVolume.value, GameSettings.tapSound.value)
            }
        } catch (e: Exception) {
            Log.e("GameScreen", "No se pudo cargar $songPath", e)
            phase = Phase.Error
            return@LaunchedEffect
        }
        // La pantalla de carga se ve al menos 1,8 s (si no, con la canción ya analizada solo parpadea).
        val shown = System.currentTimeMillis() - loadStart
        if (shown < 1800) {
            phase = Phase.Loading(1f)
            try {
                kotlinx.coroutines.delay(1800 - shown)
            } catch (e: kotlinx.coroutines.CancellationException) {
                newSession.release() // se salió durante la carga
                throw e
            }
        }
        session = newSession
        phase = Phase.Playing

        while (isActive) {
            withFrameNanos { }
            if (phase != Phase.Playing) continue
            val s = newSession
            val now = s.clock.tick() - offset
            s.engine.update(now)
            hud.update(s.engine, now, s.chart.durationMs) // antes de vaciar los eventos
            s.collectHits()
            time.intValue = now

            val done = s.engine.failed ||
                (s.engine.finished && now > s.lastNoteEndMs + 1500) ||
                s.songCompleted
            if (done) {
                s.stopMusic()
                val result = s.engine.result()
                val record = GameRecords.save(context, songPath, difficulty, result)
                if (result.fullCombo) {
                    // ¡Sin fallos! Un momento de festejo antes de los resultados.
                    phase = Phase.Celebrating(result)
                    UiSounds.play(UiSound.Chord)
                    kotlinx.coroutines.delay(2200)
                }
                phase = Phase.Finished(result, record)
            }
        }
    }

    // El OST de fondo vuelve a sonar en la pantalla de resultados (y se calla al reintentar).
    val showingResults = phase is Phase.Finished
    LaunchedEffect(showingResults) { MusicPlayer.suspended = !showingResults }

    // Libera el audio al salir o al reintentar.
    DisposableEffect(session) {
        val s = session
        onDispose { s?.release() }
    }

    // Si la app pasa a segundo plano, pausa.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && phase == Phase.Playing) {
                session?.clock?.pause()
                phase = Phase.Paused
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    BackHandler {
        when (phase) {
            Phase.Playing -> { session?.clock?.pause(); phase = Phase.Paused }
            Phase.Paused -> { session?.clock?.resume(); phase = Phase.Playing }
            else -> onExit()
        }
    }

    // Fondo: una escena de contenido/imagenes/escenario al azar (distinta de la partida anterior).
    val backdrop = remember { BackdropScenes.pick(context) }

    Box(Modifier.fillMaxSize().background(StageDark)) {
        val s = session
        if (s != null) {
            if (backdrop != null) {
                GameBackdrop(backdrop) { time.intValue }
            }
            GameView(
                chart = s.chart,
                engine = s.engine,
                pressed = s.pressed,
                hits = s.recentHits,
                nowMs = { time.intValue },
                approachMs = approach,
                overBackdrop = backdrop != null,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(s) {
                        val lanes = HashMap<PointerId, Int>()
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val g = FieldGeometry(size.width.toFloat(), size.height.toFloat())
                                event.changes.forEach { c ->
                                    if (phase != Phase.Playing) return@forEach
                                    if (c.changedToDownIgnoreConsumed()) {
                                        val lane = g.laneAt(c.position.x)
                                        lanes[c.id] = lane
                                        s.press(lane, s.clock.tick() - offset)
                                        c.consume()
                                    } else if (c.changedToUpIgnoreConsumed()) {
                                        lanes.remove(c.id)?.let { s.release(it, s.clock.tick() - offset) }
                                        c.consume()
                                    }
                                }
                            }
                        }
                    },
            )
            Hud(
                title = title,
                difficulty = difficulty,
                hud = hud,
                nowMs = { time.intValue },
                onPause = {
                    if (phase == Phase.Playing) {
                        s.clock.pause()
                        phase = Phase.Paused
                    }
                },
            )
        }

        when (val p = phase) {
            is Phase.Loading -> LoadingOverlay(title, p.progress)
            Phase.Paused -> PauseOverlay(
                onResume = { session?.clock?.resume(); phase = Phase.Playing },
                onRestart = { attempt++ },
                onQuit = onExit,
            )
            is Phase.Finished -> ResultsOverlay(
                title = title,
                difficulty = difficulty,
                result = p.result,
                newRecord = p.newRecord,
                onRetry = { attempt++ },
                onExit = onExit,
            )
            is Phase.Celebrating -> CelebrationOverlay(p.result)
            Phase.Error -> ErrorOverlay(onExit)
            Phase.Playing -> Unit
        }
    }
}

/**
 * Escenas de fondo de la partida (`contenido/imagenes/escenario`):
 *  - cada subcarpeta es una escena (1 o 2 GIFs del mismo lugar);
 *  - un GIF suelto en la carpeta principal es una escena de un solo GIF.
 */
object BackdropScenes {
    private const val DIR = "imagenes/escenario"
    private val IMAGE = setOf("gif", "webp", "png", "jpg", "jpeg")
    private var lastScene: String? = null

    fun all(context: Context): List<List<String>> {
        val assets = context.assets
        return assets.list(DIR).orEmpty().sorted().mapNotNull { entry ->
            if (entry.substringAfterLast('.').lowercase() in IMAGE) {
                listOf("$DIR/$entry")
            } else {
                assets.list("$DIR/$entry").orEmpty()
                    .filter { it.substringAfterLast('.').lowercase() in IMAGE }
                    .sorted()
                    .map { "$DIR/$entry/$it" }
                    .takeIf { it.isNotEmpty() }
            }
        }
    }

    /** Escena al azar, sin repetir la de la partida anterior (si hay más de una). */
    fun pick(context: Context): List<String>? {
        val scenes = all(context)
        if (scenes.isEmpty()) return null
        val options = scenes.filter { it.first() != lastScene }.ifEmpty { scenes }
        return options.random().also { lastScene = it.first() }
    }
}

/** Fondo animado; si la escena tiene 2 GIFs, se alternan cada 20 s con un fundido. */
@Composable
private fun GameBackdrop(scene: List<String>, nowMs: () -> Int) {
    if (scene.size == 1) {
        AnimatedAssetImage(scene[0], Modifier.fillMaxSize(), crop = true)
        return
    }
    val index by remember(scene) { androidx.compose.runtime.derivedStateOf { (nowMs().coerceAtLeast(0) / 20_000) % scene.size } }
    androidx.compose.animation.Crossfade(targetState = scene[index], animationSpec = tween(900), label = "backdrop") { path ->
        AnimatedAssetImage(path, Modifier.fillMaxSize(), crop = true)
    }
}

/** Campo de juego completo (escenario + carriles + notas). Se redibuja en cada cuadro. */
@Composable
fun GameView(
    chart: Chart,
    engine: GameEngine,
    pressed: IntArray,
    hits: Collection<HitEvent>,
    nowMs: () -> Int,
    approachMs: Int,
    modifier: Modifier = Modifier,
    overBackdrop: Boolean = false,
) {
    Canvas(modifier) {
        val now = nowMs()
        drawStage(now, overBackdrop)
        drawPlayfield(FieldGeometry(size.width, size.height), chart, engine, pressed, hits, now, approachMs)
    }
}

/** Datos del marcador que cambian poco (para no recomponer todo en cada cuadro). */
class HudState {
    var score by mutableIntStateOf(0)
    var combo by mutableIntStateOf(0)
    var life by mutableFloatStateOf(100f)
    var progress by mutableFloatStateOf(0f)
    var lastJudgment by mutableStateOf<HitEvent?>(null)

    /** Cut-in de una integrante de la banda al llegar a un combo redondo. */
    var cutIn by mutableStateOf<CutIn?>(null)
    private var lastCutInCombo = 0

    fun reset() {
        score = 0; combo = 0; life = 100f; progress = 0f; lastJudgment = null; cutIn = null; lastCutInCombo = 0
    }

    fun update(engine: GameEngine, nowMs: Int, durationMs: Int) {
        if (score != engine.score) score = engine.score
        if (combo != engine.combo) combo = engine.combo
        if (life != engine.life) life = engine.life
        val p = (nowMs.toFloat() / durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)
        if ((p * 200).toInt() != (progress * 200).toInt()) progress = p
        engine.events.lastOrNull()?.let { lastJudgment = it }
        // Cada 50 de combo entra una integrante distinta de Kessoku Band.
        if (combo >= 50 && combo % 50 == 0 && combo != lastCutInCombo) {
            lastCutInCombo = combo
            cutIn = CutIn(CUT_IN_MEMBERS[(combo / 50 - 1) % CUT_IN_MEMBERS.size], combo, nowMs)
        }
    }
}

data class CutIn(val memberId: String, val combo: Int, val atMs: Int)

private val CUT_IN_MEMBERS = listOf("bocchi", "nijika", "ryo", "kita")

private val numberFormat = NumberFormat.getIntegerInstance()

private val hudShadowStyle = TextStyle(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)

@Composable
private fun OutlinedNumber(text: String, size: Int, color: Color, modifier: Modifier = Modifier) {
    Box(modifier) {
        Text(text, style = hudShadowStyle, fontSize = size.sp, color = Ink, modifier = Modifier.offset(3.dp, 3.dp))
        Text(text, style = hudShadowStyle, fontSize = size.sp, color = color)
    }
}

@Composable
fun BoxScope.Hud(title: String, difficulty: Difficulty, hud: HudState, nowMs: () -> Int, onPause: () -> Unit) {
    // Progreso de la canción.
    Box(
        Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth(hud.progress)
            .height(4.dp)
            .background(BocchiPink),
    )
    Row(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text("SCORE", color = BocchiPink, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 2.sp)
            OutlinedNumber("%07d".format(hud.score), 26, Color.White)
        }
        Column(
            Modifier.weight(1.4f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(difficulty.label).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(difficulty.color)
                    .padding(horizontal = 10.dp, vertical = 1.dp),
            )
        }
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LifeBar(hud.life, Modifier.width(150.dp))
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.5.dp, Ink, CircleShape)
                    .bocchiClickable(sound = UiSound.Back, onClick = onPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Pause, contentDescription = stringResource(R.string.game_paused), tint = Ink)
            }
        }
    }

    // Cut-in de la banda a la izquierda (fuera de los carriles).
    hud.cutIn?.let { CutInPanel(it, nowMs) }

    // Combo a la derecha, como en el video de referencia.
    if (hud.combo >= 3) {
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedNumber(hud.combo.toString(), 52, Color.White)
            Text("COMBO", color = NijikaYellow, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        }
    }

    // Texto del último juicio, con un pequeño "pop", justo encima de la línea de juicio
    // (la línea está al 84 % del alto) para no tapar las notas que vienen.
    hud.lastJudgment?.let { hit ->
        BoxWithConstraints(Modifier.matchParentSize()) {
            JudgmentText(hit, nowMs, Modifier.align(Alignment.TopCenter).offset(y = maxHeight * 0.84f - 78.dp))
        }
    }
}

@Composable
private fun JudgmentText(hit: HitEvent, nowMs: () -> Int, modifier: Modifier) {
    run {
        Box(
            modifier
                .graphicsLayer {
                    val age = nowMs() - hit.atMs
                    val p = (age / 450f).coerceIn(0f, 1f)
                    alpha = if (age < 0) 0f else 1f - p * p
                    val pop = if (age < 90) 0.7f + age / 90f * 0.45f else 1.15f - ((age - 90) / 360f).coerceIn(0f, 1f) * 0.15f
                    scaleX = pop
                    scaleY = pop
                },
        ) {
            OutlinedNumber(hit.judgment.label, 30, hit.judgment.color)
        }
    }
}

/**
 * Cut-in estilo BanG Dream: el retrato de una integrante entra deslizándose por la izquierda
 * con el combo, se queda un momento y vuelve a salir.
 */
@Composable
private fun BoxScope.CutInPanel(cut: CutIn, nowMs: () -> Int) {
    val portrait by com.bocchifan.app.ui.components.rememberAssetImage("personajes/${cut.memberId}")
    val color = when (cut.memberId) {
        "nijika" -> NijikaYellow
        "ryo" -> com.bocchifan.app.ui.theme.RyoBlue
        "kita" -> KitaRed
        else -> BocchiPink
    }
    Box(
        Modifier
            .align(Alignment.CenterStart)
            .padding(start = 14.dp)
            .graphicsLayer {
                val age = nowMs() - cut.atMs
                val enter = (age / 260f).coerceIn(0f, 1f)
                val exit = ((age - 1500) / 260f).coerceIn(0f, 1f)
                val slide = 1f - (1f - enter) * (1f - enter) // entra frenando
                translationX = (slide - 1f - exit) * 280.dp.toPx()
                alpha = if (age < 0 || age > 1760) 0f else 1f
                rotationZ = -5f
            },
    ) {
        StickerCard(color = color, shadow = 5.dp) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val shape = RoundedCornerShape(12.dp)
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(shape)
                        .background(Color.White)
                        .border(2.5.dp, Ink, shape),
                ) {
                    portrait?.let {
                        androidx.compose.foundation.Image(
                            it,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
                Column(Modifier.padding(start = 10.dp, end = 8.dp)) {
                    OutlinedNumber(cut.combo.toString(), 34, Color.White)
                    Text("COMBO!", color = Ink, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
            }
        }
    }
}

/** Festejo al terminar sin fallos: "FULL COMBO!" o "ALL PERFECT!" con rebote y ráfagas. */
@Composable
private fun BoxScope.CelebrationOverlay(result: GameResult) {
    val pop = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.matchParentSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        LaunchedEffect(Unit) {
            pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 300f))
        }
        LaunchedEffect(Unit) {
            // Ráfagas de estrellitas y púas por la pantalla.
            repeat(8) {
                com.bocchifan.app.ui.components.TapFx.spawn(
                    androidx.compose.ui.geometry.Offset(w * (0.15f + 0.7f * kotlin.random.Random.nextFloat()), h * (0.25f + 0.5f * kotlin.random.Random.nextFloat())),
                    big = true,
                )
                kotlinx.coroutines.delay(220)
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    scaleX = 0.4f + 0.6f * pop.value
                    scaleY = 0.4f + 0.6f * pop.value
                    rotationZ = -6f
                },
            ) {
                OutlinedNumber(if (result.allPerfect) "ALL PERFECT!" else "FULL COMBO!", 54, if (result.allPerfect) NijikaYellow else BocchiPink)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.game_celebration),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun LifeBar(life: Float, modifier: Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .height(16.dp)
            .clip(shape)
            .background(Ink.copy(alpha = 0.8f))
            .border(2.dp, Color.White, shape),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(life / 100f)
                .background(
                    Brush.horizontalGradient(
                        if (life > 30f) listOf(Color(0xFF3FB27F), Color(0xFF9BE15D)) else listOf(KitaRed, BocchiPinkDeep),
                    ),
                ),
        )
    }
}

@Composable
private fun BoxScope.Dim(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Pantalla de carga: uno de los GIFs de `contenido/imagenes/carga` al azar, enmarcado como sticker,
 * sobre un fondo del mismo color que el GIF con notas musicales flotando.
 */
@Composable
private fun BoxScope.LoadingOverlay(title: String, progress: Float) {
    val context = LocalContext.current
    val gif = remember {
        context.assets.list("imagenes/carga").orEmpty()
            .filter { it.substringAfterLast('.').lowercase() in setOf("gif", "webp", "png", "jpg") }
            .randomOrNull()
            ?.let { "imagenes/carga/$it" }
    }
    val background by if (gif != null) rememberEdgeColor(gif, Paper) else remember { mutableStateOf(Paper) }
    val text = if (background.luminance() > 0.5f) Ink else Color.White

    Box(
        Modifier
            .matchParentSize()
            .background(background)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
        contentAlignment = Alignment.Center,
    ) {
        FloatingNotes(Modifier.matchParentSize(), darken(background).copy(alpha = 0.45f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(24.dp)) {
            if (gif != null) {
                val bob by rememberInfiniteTransition(label = "gifBob").animateFloat(
                    initialValue = -4f,
                    targetValue = 4f,
                    animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "bob",
                )
                StickerCard(
                    color = Color.White,
                    rotation = -3f,
                    modifier = Modifier.graphicsLayer { translationY = bob * density },
                ) {
                    AnimatedAssetImage(
                        gif,
                        crop = false,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(width = 300.dp, height = 210.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(background),
                    )
                }
            } else {
                MangoBox(Modifier.graphicsLayer { scaleX = 0.8f; scaleY = 0.8f })
            }
            Spacer(Modifier.width(32.dp))
            Column(Modifier.widthIn(max = 320.dp)) {
                Text(title, color = text, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.game_loading), color = BocchiPinkDeep, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                val shape = RoundedCornerShape(50)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(shape)
                        .background(Color.White)
                        .border(2.dp, Ink, shape),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0.04f, 1f))
                            .background(BocchiPinkDeep),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.game_loading_hint), color = text.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Notas musicales que suben despacio balanceándose (animación de fondo de la carga). */
@Composable
private fun FloatingNotes(modifier: Modifier, color: Color) {
    val t by rememberInfiniteTransition(label = "notes").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "t",
    )
    val notes = remember {
        val r = kotlin.random.Random(3)
        List(16) { Triple(r.nextFloat(), r.nextFloat(), 0.7f + r.nextFloat() * 0.8f) }
    }
    Canvas(modifier) {
        notes.forEachIndexed { i, (x, phase, scale) ->
            val p = (phase + t * (1 + i % 2)) % 1f
            val cx = size.width * x + kotlin.math.sin(p * 12.6f) * 16.dp.toPx()
            val cy = size.height * (1.1f - p * 1.2f)
            val s = 10.dp.toPx() * scale
            val a = kotlin.math.sin(p * Math.PI.toFloat()) // aparece y desaparece
            val c = color.copy(alpha = color.alpha * a)
            // Cabeza, plica y bandera de una corchea.
            drawOval(c, androidx.compose.ui.geometry.Offset(cx - s, cy - s * 0.7f), androidx.compose.ui.geometry.Size(s * 2, s * 1.4f))
            drawLine(c, androidx.compose.ui.geometry.Offset(cx + s * 0.9f, cy), androidx.compose.ui.geometry.Offset(cx + s * 0.9f, cy - s * 3.2f), s * 0.35f)
            drawLine(c, androidx.compose.ui.geometry.Offset(cx + s * 0.9f, cy - s * 3.2f), androidx.compose.ui.geometry.Offset(cx + s * 2f, cy - s * 2.4f), s * 0.35f)
        }
    }
}

@Composable
private fun OverlayButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    StickerCard(color = color, shadow = 4.dp, showTape = false, onClick = onClick, modifier = modifier) {
        Text(
            text,
            color = Ink,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun BoxScope.PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onQuit: () -> Unit) {
    Dim {
        StickerCard(color = Color.White, modifier = Modifier.width(320.dp)) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.game_paused), style = MaterialTheme.typography.headlineMedium, color = Ink)
                Spacer(Modifier.height(16.dp))
                OverlayButton(stringResource(R.string.game_resume), BocchiPink, onResume, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OverlayButton(stringResource(R.string.game_restart), NijikaYellow, onRestart, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OverlayButton(stringResource(R.string.game_quit), Color.White, onQuit, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BoxScope.ErrorOverlay(onExit: () -> Unit) {
    Dim {
        StickerCard(color = Color.White, modifier = Modifier.width(360.dp)) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.game_error), style = MaterialTheme.typography.titleLarge, color = Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                OverlayButton(stringResource(R.string.game_quit), BocchiPink, onExit, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Pantalla de resultados: rango, puntuación, combo y conteo de juicios. */
@Composable
fun BoxScope.ResultsOverlay(
    title: String,
    difficulty: Difficulty,
    result: GameResult,
    newRecord: Boolean,
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    Dim {
        // Compacta para que quepa en la altura de un teléfono en horizontal (~360 dp),
        // y con desplazamiento por si la pantalla es aún más baja.
        StickerCard(
            color = Color.White,
            modifier = Modifier
                .widthIn(max = 680.dp)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Row(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Rango.
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(150.dp)) {
                    Text(
                        stringResource(if (result.failed) R.string.game_failed else R.string.game_clear),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (result.failed) KitaRed else BocchiPinkDeep,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(if (result.failed) Color(0xFFDDD8E8) else BocchiPink)
                            .border(4.dp, Ink, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        OutlinedNumber(if (result.failed) "—" else result.rank.label, 44, Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    val badge = when {
                        result.allPerfect -> "ALL PERFECT"
                        result.fullCombo -> "FULL COMBO"
                        else -> null
                    }
                    badge?.let {
                        Text(
                            it,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(BocchiPinkDeep, Color(0xFF8E6FE0))))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                    if (newRecord) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.game_new_record), color = KitaRed, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    if (result.failed) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.game_failed_hint),
                            color = InkSoft,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(difficulty.label).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(difficulty.color)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(numberFormat.format(result.score), fontSize = 30.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, color = Ink)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "%.1f%%".format(result.accuracy * 100) + " · " + stringResource(R.string.game_max_combo) + " " + result.maxCombo,
                            color = InkSoft,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Juicios en una sola fila.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Judgment.entries.forEach { j ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(j.color.copy(alpha = 0.18f))
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    j.label,
                                    color = if (j == Judgment.Perfect) BocchiPinkDeep else darken(j.color),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                )
                                Text("${result.counts[j] ?: 0}", color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OverlayButton(stringResource(R.string.game_restart), NijikaYellow, onRetry, Modifier.weight(1f))
                        OverlayButton(stringResource(R.string.game_choose_song), BocchiPink, onExit, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun darken(c: Color) = Color(c.red * 0.7f, c.green * 0.7f, c.blue * 0.7f)

/** Gira la pantalla a horizontal y oculta las barras del sistema mientras se juega. */
@Composable
private fun LandscapeImmersive() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = view.context.findActivity()
        val window = activity?.window
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = previous ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
