package com.bocchifan.app.ui.placeholder

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bocchifan.app.R
import com.bocchifan.app.ui.components.ScreenTopBar
import com.bocchifan.app.ui.components.SystemBarsAppearance
import com.bocchifan.app.ui.components.notebookPaper
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.BocchiTheme
import com.bocchifan.app.ui.theme.BoxBrown
import com.bocchifan.app.ui.theme.BoxBrownDark
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.InkSoft
import com.bocchifan.app.ui.theme.MangoOrange
import com.bocchifan.app.ui.theme.NijikaYellow

/** Pantalla provisional para las secciones que llegan en fases siguientes. */
@Composable
fun ComingSoonScreen(title: String, accent: Color, phase: String, onBack: () -> Unit) {
    SystemBarsAppearance(lightBackground = com.bocchifan.app.ui.theme.PaperIsLight)

    Column(
        Modifier
            .fillMaxSize()
            .notebookPaper()
            .systemBarsPadding(),
    ) {
        ScreenTopBar(title = title, accent = accent, onBack = onBack)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MangoBox()
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.coming_soon_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = com.bocchifan.app.ui.theme.OnPaper,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.coming_soon_body, phase),
                    style = MaterialTheme.typography.bodyLarge,
                    color = com.bocchifan.app.ui.theme.OnPaperSoft,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * La caja de mango donde se esconde Bocchi: su pelo rosa y sus ojos se asoman
 * cada pocos segundos y la caja tiembla un poco mientras mira.
 */
@Composable
fun MangoBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mangoBox")
    val peek by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3600
                0f at 0
                0f at 1000
                1f at 1400
                1f at 2600
                0f at 3000
            },
        ),
        label = "peek",
    )
    val shake by transition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(110), RepeatMode.Reverse),
        label = "shake",
    )

    Box(
        modifier
            .size(width = 220.dp, height = 190.dp)
            .graphicsLayer { translationX = if (peek > 0.9f) shake * density else 0f },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = 3.dp.toPx()
            val bodyTop = h * 0.42f
            val left = w * 0.14f
            val right = w * 0.86f

            // Bocchi (detrás de la caja): pelo rosa, ojos y sus clásicos cubos amarillo y azul.
            val headCenterY = bodyTop + 14.dp.toPx() - peek * 34.dp.toPx()
            val headW = w * 0.36f
            val headH = 64.dp.toPx()
            drawOval(
                BocchiPink,
                topLeft = Offset(w / 2f - headW / 2f, headCenterY - headH / 2f),
                size = Size(headW, headH),
            )
            drawOval(
                Ink,
                topLeft = Offset(w / 2f - headW / 2f, headCenterY - headH / 2f),
                size = Size(headW, headH),
                style = Stroke(stroke),
            )
            val eyeY = headCenterY + 2.dp.toPx()
            listOf(-1f, 1f).forEach { side ->
                val ex = w / 2f + side * headW * 0.2f
                drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(ex, eyeY))
                drawCircle(Ink, radius = 7.dp.toPx(), center = Offset(ex, eyeY), style = Stroke(2.dp.toPx()))
                drawCircle(Ink, radius = 3.5f.dp.toPx(), center = Offset(ex + side * 1.5f.dp.toPx(), eyeY + 1.dp.toPx()))
            }
            val cube = 9.dp.toPx()
            val cubeY = headCenterY - headH * 0.42f
            drawRect(NijikaYellow, Offset(w / 2f - headW * 0.38f, cubeY), Size(cube, cube))
            drawRect(Color(0xFF4A78D6), Offset(w / 2f - headW * 0.38f + cube + 2.dp.toPx(), cubeY), Size(cube, cube))

            // Solapas abiertas de la caja.
            val flapH = h * 0.16f
            val leftFlap = Path().apply {
                moveTo(left, bodyTop)
                lineTo(left - w * 0.1f, bodyTop - flapH)
                lineTo(left + w * 0.2f, bodyTop - flapH * 1.1f)
                lineTo(left + w * 0.24f, bodyTop)
                close()
            }
            val rightFlap = Path().apply {
                moveTo(right, bodyTop)
                lineTo(right + w * 0.1f, bodyTop - flapH)
                lineTo(right - w * 0.2f, bodyTop - flapH * 1.1f)
                lineTo(right - w * 0.24f, bodyTop)
                close()
            }
            listOf(leftFlap, rightFlap).forEach {
                drawPath(it, BoxBrownDark)
                drawPath(it, Ink, style = Stroke(stroke))
            }

            // Cuerpo de la caja.
            drawRect(BoxBrown, Offset(left, bodyTop), Size(right - left, h - bodyTop - stroke))
            drawRect(Ink, Offset(left, bodyTop), Size(right - left, h - bodyTop - stroke), style = Stroke(stroke))
            // Cinta de embalaje.
            drawLine(BoxBrownDark, Offset(w / 2f, bodyTop + stroke), Offset(w / 2f, h - stroke * 2), 10.dp.toPx())
        }
        Text(
            stringResource(R.string.box_label),
            color = Color.White,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .graphicsLayer { rotationZ = -4f }
                .background(MangoOrange, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun ComingSoonPreview() {
    BocchiTheme { ComingSoonScreen("Personajes", NijikaYellow, "Fase 2", onBack = {}) }
}
