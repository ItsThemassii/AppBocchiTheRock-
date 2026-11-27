package com.bocchifan.app.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocchifan.app.R
import com.bocchifan.app.data.LyricsRepository
import com.bocchifan.app.data.Song
import com.bocchifan.app.data.SongRepository
import com.bocchifan.app.data.rememberSongCover
import com.bocchifan.app.ui.components.ScreenTopBar
import com.bocchifan.app.ui.components.StickerCard
import com.bocchifan.app.ui.components.SystemBarsAppearance
import com.bocchifan.app.ui.components.notebookPaper
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.InkSoft
import com.bocchifan.app.ui.theme.RyoBlue

/** Lista de canciones para escuchar (con portada y aviso de si tiene letra). */
@Composable
fun SongListScreen(onBack: () -> Unit, onOpen: (Song) -> Unit) {
    SystemBarsAppearance(lightBackground = com.bocchifan.app.ui.theme.PaperIsLight)
    val context = LocalContext.current
    val songs = remember { SongRepository.all(context) }

    Column(
        Modifier
            .fillMaxSize()
            .notebookPaper()
            .systemBarsPadding(),
    ) {
        ScreenTopBar(title = stringResource(R.string.menu_songs), accent = RyoBlue, onBack = onBack)
        if (songs.isEmpty()) {
            Text(stringResource(R.string.songs_empty), style = MaterialTheme.typography.bodyLarge, color = com.bocchifan.app.ui.theme.OnPaperSoft, modifier = Modifier.padding(24.dp))
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            songs.groupBy { it.album }.forEach { (album, albumSongs) ->
                if (album != null) {
                    item(key = "album-$album") {
                        Text(album, style = MaterialTheme.typography.titleLarge, color = com.bocchifan.app.ui.theme.OnPaper, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                items(albumSongs, key = { it.path }) { song -> SongItem(song) { onOpen(song) } }
            }
            item { Spacer(Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun SongItem(song: Song, onClick: () -> Unit) {
    val context = LocalContext.current
    val cover by rememberSongCover(song.path)
    val hasLyrics = remember(song.path) { LyricsRepository.has(context, song.path) }
    StickerCard(color = Color.White, shadow = 4.dp, showTape = false, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val shape = RoundedCornerShape(10.dp)
            Box(
                Modifier
                    .size(width = 84.dp, height = 48.dp)
                    .clip(shape)
                    .background(RyoBlue)
                    .border(2.dp, Ink, shape),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = cover
                if (bmp != null) {
                    Image(bmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Rounded.Headphones, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Kessoku Band", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                    if (hasLyrics) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.player_lyrics).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(BocchiPinkDeep)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Icon(Icons.Rounded.Headphones, contentDescription = null, tint = Ink, modifier = Modifier.size(26.dp))
        }
    }
}
