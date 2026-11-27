package com.bocchifan.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.bocchifan.app.R
import com.bocchifan.app.ui.theme.BandColors
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.Paper
import com.bocchifan.app.ui.theme.PaperLine

/** Contorno de una púa de guitarra (punta hacia abajo) dentro del rectángulo dado. */
fun pickPath(left: Float, top: Float, width: Float, height: Float): Path = Path().apply {
    val w = width
    val h = height
    moveTo(left + w * 0.5f, top + h)
    cubicTo(left + w * 0.2f, top + h * 0.78f, left, top + h * 0.5f, left, top + h * 0.28f)
    cubicTo(left, top + h * 0.06f, left + w * 0.24f, top, left + w * 0.5f, top)
    cubicTo(left + w * 0.76f, top, left + w, top + h * 0.06f, left + w, top + h * 0.28f)
    cubicTo(left + w, top + h * 0.5f, left + w * 0.8f, top + h * 0.78f, left + w * 0.5f, top + h)
    close()
}

/** Forma de púa para recortar cualquier composable. */
object GuitarPickShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(pickPath(0f, 0f, size.width, size.height))
}

/** Fondo de hoja de cuaderno: cuadrícula rosa suave y margen. */
fun Modifier.notebookPaper(): Modifier = this
    .background(Paper)
    .drawBehind {
        val step = 28.dp.toPx()
        val stroke = 1.dp.toPx()
        var y = step
        while (y < size.height) {
            drawLine(PaperLine, Offset(0f, y), Offset(size.width, y), stroke)
            y += step
        }
        var x = step
        while (x < size.width) {
            drawLine(PaperLine.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height), stroke)
            x += step
        }
        val margin = 40.dp.toPx()
        drawLine(BocchiPinkDeep.copy(alpha = 0.3f), Offset(margin, 0f), Offset(margin, size.height), 2.dp.toPx())
    }

/** Cuatro franjas inclinadas con los colores de la banda. */
@Composable
fun BandStripes(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width / BandColors.size
        val skew = size.height
        BandColors.forEachIndexed { i, color ->
            val x0 = i * w
            val path = Path().apply {
                moveTo(x0, size.height)
                lineTo(x0 + skew, 0f)
                lineTo(x0 + w + skew, 0f)
                lineTo(x0 + w, size.height)
                close()
            }
            drawPath(path, color)
        }
    }
}

/**
 * Tarjeta tipo sticker: borde de tinta, sombra dura y un trozo de cinta adhesiva.
 * Al pulsarla, la tarjeta "se pega" al papel (la sombra desaparece).
 */
@Composable
fun StickerCard(
    color: Color,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    shadow: Dp = 6.dp,
    showTape: Boolean = true,
    sound: UiSound = UiSound.Tap,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateDpAsState(if (pressed) 0.dp else shadow, label = "stickerLift")
    val shape = RoundedCornerShape(18.dp)
    // "Pop" al tocar: se encoge un poco y rebota.
    val pop = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier
            .graphicsLayer {
                rotationZ = rotation
                scaleX = pop.value
                scaleY = pop.value
            }
            .padding(end = shadow, bottom = shadow),
        // La tarjeta ocupa el tamaño que pida quien la usa (ancho completo, proporción, etc.).
        propagateMinConstraints = true,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(shadow, shadow)
                .background(com.bocchifan.app.ui.theme.CardShadow, shape),
        )
        Box(
            Modifier
                .offset(shadow - lift, shadow - lift)
                .clip(shape)
                .background(color)
                .border(3.dp, Ink, shape)
                .then(
                    if (onClick != null) {
                        Modifier.bocchiClickable(sound, interaction) {
                            scope.launch {
                                pop.snapTo(0.93f)
                                pop.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow))
                            }
                            onClick()
                        }
                    } else {
                        Modifier
                    },
                ),
            content = content,
        )
        if (showTape) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    // Evita que la cinta herede el tamaño mínimo de la tarjeta.
                    .wrapContentSize(Alignment.TopCenter)
                    .offset(shadow - lift, (-10).dp + shadow - lift)
                    .graphicsLayer { rotationZ = -6f }
                    .size(width = 56.dp, height = 20.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(3.dp)),
            )
        }
    }
}

/** Barra superior de las pantallas internas: botón volver y título subrayado con marcador. */
@Composable
fun ScreenTopBar(title: String, accent: Color, onBack: () -> Unit) {
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StickerCard(color = Color.White, shadow = 4.dp, showTape = false, sound = UiSound.Back, onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Ink,
                modifier = Modifier.padding(10.dp).size(24.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = com.bocchifan.app.ui.theme.OnPaper,
            modifier = Modifier.drawBehind {
                // Marcador fluorescente bajo la mitad inferior del texto.
                drawRect(
                    accent.copy(alpha = 0.55f),
                    topLeft = Offset(-4.dp.toPx(), size.height * 0.55f),
                    size = Size(size.width + 8.dp.toPx(), size.height * 0.35f),
                )
            },
        )
    }
}

/**
 * Ajusta el color de los íconos de las barras del sistema según el fondo de la pantalla.
 * [lightBackground]: fondo claro detrás de la barra de estado (íconos oscuros).
 */
@Composable
fun SystemBarsAppearance(lightBackground: Boolean, lightNavigationBar: Boolean = lightBackground) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBackground
            isAppearanceLightNavigationBars = lightNavigationBar
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
