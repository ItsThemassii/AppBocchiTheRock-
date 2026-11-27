package com.bocchifan.app.ui.menu

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bocchifan.app.R
import com.bocchifan.app.navigation.Screen
import com.bocchifan.app.ui.components.BandStripes
import com.bocchifan.app.ui.components.GuitarPickShape
import com.bocchifan.app.ui.components.NowPlayingBar
import com.bocchifan.app.ui.components.StickerCard
import com.bocchifan.app.ui.components.UiSound
import com.bocchifan.app.ui.components.SystemBarsAppearance
import com.bocchifan.app.ui.components.notebookPaper
import com.bocchifan.app.ui.components.rememberAssetImage
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.BocchiTheme
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.InkSoft
import com.bocchifan.app.ui.theme.KitaRed
import com.bocchifan.app.ui.theme.NijikaYellow
import com.bocchifan.app.ui.theme.RyoBlue
import kotlinx.coroutines.delay

private data class MenuEntry(
    val screen: Screen,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val icon: ImageVector,
    val color: Color,
    val contentColor: Color,
    val rotation: Float,
)

// Cada opción usa el color de una integrante: Bocchi, Nijika, Ryo y Kita.
private val entries = listOf(
    MenuEntry(Screen.Play, R.string.menu_play, R.string.menu_play_desc, Icons.Rounded.MusicNote, BocchiPink, Ink, -2.5f),
    MenuEntry(Screen.Characters, R.string.menu_characters, R.string.menu_characters_desc, Icons.Rounded.Groups, NijikaYellow, Ink, 2f),
    MenuEntry(Screen.Songs, R.string.menu_songs, R.string.menu_songs_desc, Icons.Rounded.LibraryMusic, RyoBlue, Color.White, 1.5f),
    MenuEntry(Screen.Settings, R.string.menu_settings, R.string.menu_settings_desc, Icons.Rounded.Settings, KitaRed, Color.White, -2f),
)

/** [id] es el nombre de su imagen opcional: contenido/imagenes/personajes/<id>.png */
private data class Member(val id: String, val initial: String, val name: String, val color: Color)

private val lineup = listOf(
    Member("bocchi", "B", "Hitori Gotoh", BocchiPink),
    Member("nijika", "N", "Nijika Ijichi", NijikaYellow),
    Member("ryo", "R", "Ryo Yamada", RyoBlue),
    Member("kita", "K", "Ikuyo Kita", KitaRed),
)

@Composable
fun MenuScreen(onNavigate: (Screen) -> Unit) {
    SystemBarsAppearance(lightBackground = com.bocchifan.app.ui.theme.PaperIsLight)

    Column(
        Modifier
            .fillMaxSize()
            .notebookPaper()
            .verticalScroll(rememberScrollState()),
    ) {
        MenuHeader()
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        ) {
            MenuBody(onNavigate)
        }
    }
}

@Composable
private fun MenuBody(onNavigate: (Screen) -> Unit) {
    Column {
        NowPlayingBar()
        Spacer(Modifier.height(24.dp))

        entries.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEachIndexed { col, entry ->
                    MenuCard(
                        entry = entry,
                        index = row * 2 + col,
                        onClick = { onNavigate(entry.screen) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        Spacer(Modifier.height(8.dp))
        Lineup()
    }
}


@Composable
private fun MenuCard(entry: MenuEntry, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Las tarjetas "caen" sobre el cuaderno una tras otra.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(90L * index)
        appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }

    StickerCard(
        color = entry.color,
        rotation = entry.rotation,
        // "Jugar" suena con un power chord; el resto, con una púa.
        sound = if (entry.screen == Screen.Play) UiSound.Chord else UiSound.Tap,
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                val v = appear.value
                scaleX = 0.7f + 0.3f * v
                scaleY = 0.7f + 0.3f * v
                alpha = v.coerceIn(0f, 1f)
            }
            .aspectRatio(0.9f),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(Color.White.copy(alpha = 0.85f), CircleShape)
                    .border(2.5.dp, Ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(entry.icon, contentDescription = null, tint = Ink, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(entry.title),
                style = MaterialTheme.typography.titleLarge,
                color = entry.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(entry.description),
                style = MaterialTheme.typography.bodySmall,
                color = entry.contentColor.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Pie del menú: las cuatro integrantes de Kessoku Band con su color. */
@Composable
private fun Lineup() {
    StickerCard(color = Color.White, showTape = false, shadow = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.menu_lineup),
                style = MaterialTheme.typography.labelLarge,
                color = InkSoft,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                lineup.forEach { member ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                        MemberAvatar(member)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            member.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Ink,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            BandStripes(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
        }
    }
}

/** Retrato redondo si existe la imagen del personaje; si no, su inicial en su color. */
@Composable
private fun MemberAvatar(member: Member) {
    val portrait by rememberAssetImage("personajes/${member.id}")
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(member.color)
            .border(2.5.dp, Ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val image = portrait
        if (image != null) {
            Image(
                image,
                contentDescription = member.name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(member.initial, color = Ink, fontWeight = FontWeight.Black)
        }
    }
}

@Preview(widthDp = 360, heightDp = 760)
@Composable
private fun MenuScreenPreview() {
    BocchiTheme { MenuScreen(onNavigate = {}) }
}
