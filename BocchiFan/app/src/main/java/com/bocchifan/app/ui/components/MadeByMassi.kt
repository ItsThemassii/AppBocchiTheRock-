package com.bocchifan.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocchifan.app.R
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.NijikaYellow

/**
 * Firma de quien hizo la app: "hecho con ♥ por Massi", con el corazón latiendo
 * y el nombre subrayado con marcador amarillo.
 */
@Composable
fun MadeByMassi(textColor: Color, modifier: Modifier = Modifier, textSize: TextUnit = 13.sp) {
    val beat by rememberInfiniteTransition(label = "heart").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1100
                1f at 0
                1.35f at 120 using FastOutSlowInEasing
                1f at 260
                1.25f at 380 using FastOutSlowInEasing
                1f at 560
            },
            RepeatMode.Restart,
        ),
        label = "beat",
    )
    Row(
        modifier.graphicsLayer { rotationZ = -2f },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.credit_made_with), color = textColor, fontSize = textSize, fontStyle = FontStyle.Italic)
        Spacer(Modifier.width(5.dp))
        Text(
            "♥︎", // ♥ como texto (no emoji) para que tome el color rosa
            color = BocchiPinkDeep,
            fontSize = textSize * 1.25f,
            modifier = Modifier.graphicsLayer {
                scaleX = beat
                scaleY = beat
            },
        )
        Spacer(Modifier.width(5.dp))
        Text(stringResource(R.string.credit_by), color = textColor, fontSize = textSize, fontStyle = FontStyle.Italic)
        Spacer(Modifier.width(5.dp))
        Text(
            "Massi",
            color = if (textColor.red + textColor.green + textColor.blue > 1.5f) BocchiPink else BocchiPinkDeep,
            fontSize = textSize * 1.15f,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.drawBehind {
                drawRect(
                    NijikaYellow.copy(alpha = 0.55f),
                    topLeft = Offset(-2.dp.toPx(), size.height * 0.62f),
                    size = Size(size.width + 4.dp.toPx(), size.height * 0.28f),
                )
            },
        )
    }
}
