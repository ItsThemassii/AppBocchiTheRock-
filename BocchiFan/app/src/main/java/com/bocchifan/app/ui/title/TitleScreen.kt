package com.bocchifan.app.ui.title

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocchifan.app.R
import com.bocchifan.app.ui.components.BandStripes
import com.bocchifan.app.ui.components.MadeByMassi
import com.bocchifan.app.ui.components.SystemBarsAppearance
import com.bocchifan.app.ui.components.UiSound
import com.bocchifan.app.ui.components.bocchiClickable
import com.bocchifan.app.ui.components.pickPath
import com.bocchifan.app.ui.components.rememberAssetImage
import com.bocchifan.app.ui.theme.BandColors
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.BocchiTheme
import com.bocchifan.app.ui.theme.KitaRed
import com.bocchifan.app.ui.theme.NijikaYellow
import com.bocchifan.app.ui.theme.RyoBlue
import com.bocchifan.app.ui.theme.StageDark
import com.bocchifan.app.ui.theme.StageGlow
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Portada: escenario oscuro con focos, púas flotando y el título con un "glitch"
 * de vez en cuando (los nervios de Bocchi). Tocar en cualquier parte abre el menú.
 */
@Composable
fun TitleScreen(onStart: () -> Unit) {
    SystemBarsAppearance(lightBackground = false)

    val infinite = rememberInfiniteTransition(label = "title")
    val tapAlpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "tapAlpha",
    )
    val bob by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )

    var glitch by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            repeat(6) {
                glitch = Offset(Random.nextInt(-14, 15).toFloat(), Random.nextInt(-4, 5).toFloat())
                delay(45)
            }
            glitch = Offset.Zero
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(StageGlow, StageDark)))
            .bocchiClickable(sound = UiSound.Chord, onClick = onStart),
    ) {
        // Fondo opcional: contenido/imagenes/portada.(png|jpg|webp), oscurecido abajo para leer el texto.
        val cover by rememberAssetImage("portada")
        cover?.let { image ->
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(StageDark.copy(alpha = 0.2f), StageDark.copy(alpha = 0.5f), StageDark.copy(alpha = 0.92f)),
                        ),
                    ),
            )
        }
        Spotlights(Modifier.fillMaxSize())
        FloatingPicks(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.title_kicker),
                color = BocchiPink,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(18.dp))
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationZ = -4f
                        translationY = bob * density
                    },
                contentAlignment = Alignment.Center,
            ) {
                val big = (maxWidth.value * 0.2f).coerceAtMost(120f).sp
                val small = (maxWidth.value * 0.13f).coerceAtMost(80f).sp
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TitleLine(stringResource(R.string.title_line1), big, glitch)
                    TitleLine(stringResource(R.string.title_line2), small, glitch)
                }
            }
            Spacer(Modifier.weight(1.2f))
            Text(
                stringResource(R.string.title_tap),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.alpha(tapAlpha),
            )
            Spacer(Modifier.height(28.dp))
            BandStripes(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.title_disclaimer),
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
            MadeByMassi(textColor = Color.White.copy(alpha = 0.8f))
        }
    }
}

/** Una línea del título: sombra rosa dura y, durante el glitch, copias azul/roja desplazadas. */
@Composable
private fun TitleLine(text: String, size: TextUnit, glitch: Offset) {
    val style = TextStyle(
        fontSize = size,
        lineHeight = size * 1.05f,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        letterSpacing = 1.sp,
    )
    Box {
        if (glitch != Offset.Zero) {
            Text(
                text,
                style = style,
                color = RyoBlue.copy(alpha = 0.85f),
                modifier = Modifier.offset { IntOffset(glitch.x.roundToInt(), glitch.y.roundToInt()) },
            )
            Text(
                text,
                style = style,
                color = KitaRed.copy(alpha = 0.85f),
                modifier = Modifier.offset { IntOffset(-glitch.x.roundToInt(), -glitch.y.roundToInt()) },
            )
        }
        Text(text, style = style, color = BocchiPinkDeep, modifier = Modifier.offset(5.dp, 5.dp))
        Text(text, style = style, color = Color.White)
    }
}

/** Focos de escenario de colores desde arriba. */
@Composable
private fun Spotlights(modifier: Modifier) {
    val sway by rememberInfiniteTransition(label = "lights").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    Canvas(modifier) {
        val r = size.width * 0.9f
        listOf(
            Triple(0.15f, BocchiPink, 1f),
            Triple(0.5f, NijikaYellow, -0.6f),
            Triple(0.85f, RyoBlue, -1f),
        ).forEach { (fx, color, dir) ->
            val center = Offset(size.width * fx + sway * dir * size.width * 0.08f, size.height * 0.05f)
            drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent), center, r),
                radius = r,
                center = center,
            )
        }
    }
}

private data class Pick(
    val x: Float,
    val phase: Float,
    val speed: Int,
    val sizeDp: Float,
    val colorIndex: Int,
    val spin: Float,
)

/** Púas de guitarra subiendo despacio por el escenario. */
@Composable
private fun FloatingPicks(modifier: Modifier) {
    val picks = remember {
        List(14) { i ->
            val r = Random(i * 31 + 7)
            Pick(
                x = r.nextFloat(),
                phase = r.nextFloat(),
                // Velocidades enteras: el ciclo se repite sin saltos.
                speed = r.nextInt(1, 3),
                sizeDp = 12f + r.nextFloat() * 18f,
                colorIndex = i % BandColors.size,
                spin = r.nextFloat() * 360f,
            )
        }
    }
    val t by rememberInfiniteTransition(label = "picks").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "t",
    )
    Canvas(modifier) {
        picks.forEach { p ->
            val progress = (p.phase + t * p.speed) % 1f
            val s = p.sizeDp.dp.toPx()
            val y = size.height * (1.1f - progress * 1.2f)
            val x = size.width * p.x + sin(progress * 12.566f) * 18.dp.toPx()
            rotate(p.spin + progress * 360f, pivot = Offset(x, y)) {
                drawPath(
                    pickPath(x - s / 2f, y - s * 0.6f, s, s * 1.2f),
                    BandColors[p.colorIndex].copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun TitleScreenPreview() {
    BocchiTheme { TitleScreen(onStart = {}) }
}
