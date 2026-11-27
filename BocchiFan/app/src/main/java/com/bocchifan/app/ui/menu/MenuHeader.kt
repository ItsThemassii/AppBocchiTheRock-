package com.bocchifan.app.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.bocchifan.app.ui.components.AnimatedAssetImage
import com.bocchifan.app.ui.components.bocchiClickable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocchifan.app.R
import com.bocchifan.app.ui.components.StickerCard
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.KitaRed
import com.bocchifan.app.ui.theme.NijikaYellow
import com.bocchifan.app.ui.theme.RyoBlue

private val FlyerPaper = Color(0xFFFFD9E6)

/**
 * Encabezado del menú: un cartel de concierto rosa pegado al cuaderno con cinta,
 * puntos de trama, el sello de la sala STARRY y Bocchi diciendo el saludo.
 */
@Composable
fun MenuHeader(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 8.dp),
    ) {
        StickerCard(color = FlyerPaper, rotation = -1.5f, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .halftone(BocchiPinkDeep.copy(alpha = 0.22f), origin = Offset(1f, 0f)),
            ) {
                Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "LIVE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            modifier = Modifier
                                .background(Ink, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.menu_kicker),
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ShadowTitle(stringResource(R.string.title_line1), 50.sp)
                    Text(
                        stringResource(R.string.title_line2),
                        style = titleStyle(30.sp),
                        color = Ink,
                        // Subrayado con marcador amarillo.
                        modifier = Modifier.drawBehind {
                            drawRect(
                                NijikaYellow,
                                topLeft = Offset(-6.dp.toPx(), size.height * 0.45f),
                                size = Size(size.width + 10.dp.toPx(), size.height * 0.45f),
                            )
                        },
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CuteBocchi()
                        Spacer(Modifier.width(6.dp))
                        SpeechBubble(stringResource(R.string.menu_greeting))
                    }
                }
                StarryStamp(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 54.dp, end = 14.dp),
                )
            }
        }
    }
}

private fun titleStyle(size: TextUnit) = TextStyle(
    fontSize = size,
    lineHeight = size * 1.05f,
    fontWeight = FontWeight.Black,
    fontStyle = FontStyle.Italic,
)

/** Título en tinta con sombra rosa dura. */
@Composable
private fun ShadowTitle(text: String, size: TextUnit) {
    val style = titleStyle(size)
    Box {
        Text(text, style = style, color = BocchiPinkDeep, modifier = Modifier.offset(4.dp, 4.dp))
        Text(text, style = style, color = Ink)
    }
}

/** Sello de tinta roja de la sala de conciertos STARRY. */
@Composable
private fun StarryStamp(modifier: Modifier) {
    Box(
        modifier
            .graphicsLayer {
                rotationZ = 14f
                alpha = 0.85f
            }
            .size(84.dp)
            .border(3.dp, KitaRed, CircleShape)
            .padding(5.dp)
            .border(1.5.dp, KitaRed, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("★", color = KitaRed, fontSize = 12.sp)
            Text("STARRY", color = KitaRed, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
            Text("SHIMOKITA", color = KitaRed, fontWeight = FontWeight.Bold, fontSize = 7.sp, letterSpacing = 1.sp)
        }
    }
}

/** Puntos de trama (halftone) que se hacen pequeños al alejarse de [origin] (fracciones 0..1). */
private fun Modifier.halftone(color: Color, origin: Offset): Modifier = drawBehind {
    val step = 10.dp.toPx()
    val maxR = step * 0.42f
    val o = Offset(origin.x * size.width, origin.y * size.height)
    val reach = size.maxDimension * 0.8f
    var row = 0
    var y = step / 2f
    while (y < size.height) {
        var x = step / 2f + if (row % 2 == 1) step / 2f else 0f
        while (x < size.width) {
            val r = maxR * (1f - (Offset(x, y) - o).getDistance() / reach)
            if (r > 0.6f) drawCircle(color, r, Offset(x, y))
            x += step
        }
        y += step
        row++
    }
}

/** Globo de diálogo blanco con borde de tinta y la cola hacia la izquierda. */
@Composable
private fun SpeechBubble(text: String) {
    val tailWidth = 10.dp
    val shape = RoundedCornerShape(18.dp)
    Box(contentAlignment = Alignment.CenterStart) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier
                .padding(start = tailWidth)
                .clip(shape)
                .background(Color.White)
                .border(2.5.dp, Ink, shape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        Canvas(Modifier.size(width = tailWidth + 4.dp, height = 16.dp)) {
            val baseX = tailWidth.toPx() + 3.dp.toPx()
            val tip = Offset(0f, size.height / 2f)
            val top = Offset(baseX, 1.dp.toPx())
            val bottom = Offset(baseX, size.height - 1.dp.toPx())
            val fill = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(top.x, top.y)
                lineTo(bottom.x, bottom.y)
                close()
            }
            drawPath(fill, Color.White)
            val stroke = 2.5.dp.toPx()
            drawLine(Ink, tip, top, stroke)
            drawLine(Ink, tip, bottom, stroke)
        }
    }
}

/**
 * Bocchi cute (imágenes de `contenido/imagenes/bocchi`, en orden de nombre). Al tocarla cambia
 * a la siguiente con un rebote. Si no hay imágenes, se dibuja la carita de siempre.
 */
@Composable
private fun CuteBocchi() {
    val context = LocalContext.current
    val images = remember {
        context.assets.list("imagenes/bocchi").orEmpty()
            .filter { it.substringAfterLast('.').lowercase() in setOf("gif", "webp", "png", "jpg", "jpeg") }
            .sorted()
            .map { "imagenes/bocchi/$it" }
    }
    if (images.isEmpty()) {
        BocchiFace(Modifier.size(48.dp))
        return
    }
    var index by remember { mutableIntStateOf(0) }
    val jump = remember { Animatable(0f) } // 0 = en reposo, 1 = en lo alto del salto
    val scope = rememberCoroutineScope()
    // Balanceo suave, como si respirara (o temblara de nervios).
    val sway by rememberInfiniteTransition(label = "sway").animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "swayAngle",
    )
    // Sticker recortado (sin marco): se balancea y, al tocarlo, salta, se aplasta y cambia de pose.
    Box(
        Modifier
            .size(width = 92.dp, height = 80.dp)
            .graphicsLayer {
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                rotationZ = sway
                translationY = -jump.value * 18.dp.toPx()
                scaleX = 1f + jump.value * 0.08f
                scaleY = 1f - jump.value * 0.06f
            }
            .bocchiClickable {
                index = (index + 1) % images.size
                scope.launch {
                    jump.snapTo(0f)
                    jump.animateTo(1f, tween(120))
                    jump.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
                }
            },
    ) {
        AnimatedAssetImage(images[index], Modifier.matchParentSize(), crop = false)
    }
}

/** Carita de Bocchi: pelo rosa, rubor, ojos que parpadean y sus cubos amarillo y azul. */
@Composable
private fun BocchiFace(modifier: Modifier) {
    val blink by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                1f at 0
                1f at 2900
                0.1f at 3000
                1f at 3100
            },
        ),
        label = "blinkValue",
    )
    Canvas(modifier) {
        val stroke = 2.5.dp.toPx()
        val r = size.minDimension / 2f - stroke / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(BocchiPink, r, c)
        drawCircle(Ink, r, c, style = Stroke(stroke))
        val bangs = Path().apply {
            moveTo(c.x - r * 0.75f, c.y - r * 0.15f)
            quadraticBezierTo(c.x - r * 0.35f, c.y - r * 0.05f, c.x - r * 0.25f, c.y - r * 0.5f)
            quadraticBezierTo(c.x, c.y - r * 0.05f, c.x + r * 0.25f, c.y - r * 0.5f)
            quadraticBezierTo(c.x + r * 0.35f, c.y - r * 0.05f, c.x + r * 0.75f, c.y - r * 0.15f)
        }
        drawPath(bangs, BocchiPinkDeep, style = Stroke(stroke))
        listOf(-1f, 1f).forEach { side ->
            val eye = Offset(c.x + side * r * 0.35f, c.y + r * 0.22f)
            val eyeW = r * 0.34f
            val eyeH = r * 0.38f * blink
            drawOval(Color.White, Offset(eye.x - eyeW / 2f, eye.y - eyeH / 2f), Size(eyeW, eyeH))
            drawOval(Ink, Offset(eye.x - eyeW * 0.22f, eye.y - eyeH * 0.25f), Size(eyeW * 0.44f, eyeH * 0.5f))
            drawOval(
                BocchiPinkDeep.copy(alpha = 0.35f),
                Offset(c.x + side * r * 0.6f - r * 0.14f, c.y + r * 0.45f),
                Size(r * 0.28f, r * 0.14f),
            )
        }
        val cube = r * 0.32f
        drawRect(NijikaYellow, Offset(c.x - r * 0.72f, c.y - r * 0.8f), Size(cube, cube))
        drawRect(RyoBlue, Offset(c.x - r * 0.72f + cube * 1.1f, c.y - r * 0.8f), Size(cube, cube))
    }
}
