package com.bocchifan.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocchifan.app.R
import com.bocchifan.app.audio.MusicPlayer
import com.bocchifan.app.ui.theme.BandColors
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.InkSoft

/** Barra "Sonando ahora": pista actual, ecualizador animado, siguiente y silenciar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingBar(modifier: Modifier = Modifier) {
    if (!MusicPlayer.hasTracks) return
    val track by MusicPlayer.currentTrack
    val enabled by MusicPlayer.enabled

    StickerCard(color = Color.White, shadow = 4.dp, showTape = false, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Equalizer(playing = enabled, modifier = Modifier.size(width = 26.dp, height = 22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.now_playing).uppercase(),
                    color = BocchiPinkDeep,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                )
                Text(
                    track ?: if (enabled) "—" else stringResource(R.string.music_muted),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Ink else InkSoft,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
            }
            Box(
                Modifier
                    .size(48.dp)
                    .bocchiClickable { MusicPlayer.next() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.music_next), tint = Ink)
            }
            Box(
                Modifier
                    .size(48.dp)
                    .bocchiClickable(sound = if (enabled) UiSound.Back else UiSound.Tap) { MusicPlayer.setEnabled(!enabled) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (enabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    contentDescription = stringResource(if (enabled) R.string.music_mute else R.string.music_unmute),
                    tint = Ink,
                )
            }
        }
    }
}

/** Cuatro barras con los colores de la banda que suben y bajan mientras suena la música. */
@Composable
private fun Equalizer(playing: Boolean, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val levels = listOf(420, 300, 520, 360).map { duration ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(duration), RepeatMode.Reverse),
            label = "eqBar",
        )
    }
    Canvas(modifier) {
        val gap = 2.dp.toPx()
        val barW = (size.width - gap * 3) / 4
        levels.forEachIndexed { i, level ->
            val h = size.height * (if (playing) level.value else 0.25f)
            drawRoundRect(
                BandColors[i],
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2),
            )
        }
    }
}
