package com.bocchifan.app.ui.songs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocchifan.app.R
import com.bocchifan.app.data.GameRecords
import com.bocchifan.app.data.GameSettings
import com.bocchifan.app.data.Song
import com.bocchifan.app.data.SongRepository
import com.bocchifan.app.game.Difficulty
import com.bocchifan.app.game.LaneColors
import com.bocchifan.app.ui.components.GuitarPickShape
import com.bocchifan.app.ui.components.ScreenTopBar
import com.bocchifan.app.ui.components.StickerCard
import com.bocchifan.app.ui.components.SystemBarsAppearance
import com.bocchifan.app.ui.components.notebookPaper
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.InkSoft
import java.text.NumberFormat

/** Elegir canción, dificultad y velocidad antes de jugar. */
@Composable
fun SongSelectScreen(title: String, accent: Color, onBack: () -> Unit, onPlay: (Song, Difficulty) -> Unit) {
    SystemBarsAppearance(lightBackground = com.bocchifan.app.ui.theme.PaperIsLight)
    val context = LocalContext.current
    val songs = remember { SongRepository.all(context) }
    val difficulty by GameSettings.difficulty
    val speed by GameSettings.speed

    Column(
        Modifier
            .fillMaxSize()
            .notebookPaper()
            .systemBarsPadding(),
    ) {
        ScreenTopBar(title = title, accent = accent, onBack = onBack)

        if (songs.isEmpty()) {
            Text(
                stringResource(R.string.songs_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = com.bocchifan.app.ui.theme.OnPaperSoft,
                modifier = Modifier.padding(24.dp),
            )
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StickerCard(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.songs_difficulty), style = MaterialTheme.typography.labelLarge, color = InkSoft)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Difficulty.entries.forEach { d ->
                                DifficultyChip(d, selected = d == difficulty, modifier = Modifier.weight(1f)) {
                                    GameSettings.setDifficulty(d)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.songs_speed), style = MaterialTheme.typography.labelLarge, color = InkSoft, modifier = Modifier.weight(1f))
                            Text(speed.toString(), fontWeight = FontWeight.Black, color = Ink, fontSize = 18.sp)
                        }
                        Slider(
                            value = speed.toFloat(),
                            onValueChange = { GameSettings.setSpeed(it.toInt()) },
                            onValueChangeFinished = { com.bocchifan.app.ui.components.UiSounds.play(com.bocchifan.app.ui.components.UiSound.Tap) },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = BocchiPinkDeep,
                                activeTrackColor = BocchiPinkDeep,
                                inactiveTrackColor = BocchiPink.copy(alpha = 0.4f),
                                activeTickColor = Color.White,
                                inactiveTickColor = BocchiPinkDeep.copy(alpha = 0.4f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            songs.groupBy { it.album }.forEach { (album, albumSongs) ->
                if (album != null) {
                    item(key = "album-$album") {
                        Text(
                            album,
                            style = MaterialTheme.typography.titleLarge,
                            color = com.bocchifan.app.ui.theme.OnPaper,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                        )
                    }
                }
                items(albumSongs, key = { it.path }) { song ->
                    SongRow(song, albumSongs.indexOf(song), difficulty) { onPlay(song, difficulty) }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DifficultyChip(d: Difficulty, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    StickerCard(
        color = if (selected) d.color else Color.White,
        shadow = if (selected) 1.dp else 3.dp,
        showTape = false,
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            stringResource(d.label),
            color = if (selected) Color.White else Ink,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun SongRow(song: Song, index: Int, difficulty: Difficulty, onClick: () -> Unit) {
    val context = LocalContext.current
    val record = remember(song.path, difficulty) { GameRecords.get(context, song.path, difficulty) }
    val color = LaneColors[index % LaneColors.size]

    StickerCard(
        color = Color.White,
        shadow = 4.dp,
        showTape = false,
        sound = com.bocchifan.app.ui.components.UiSound.Chord,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(width = 40.dp, height = 48.dp)
                    .background(color, GuitarPickShape)
                    .border(2.5.dp, Ink, GuitarPickShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("${index + 1}", color = Ink, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (record != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.songs_best, NumberFormat.getIntegerInstance().format(record.score)),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft,
                        )
                        Spacer(Modifier.width(6.dp))
                        Badge(record.rank.label, BocchiPinkDeep)
                        if (record.allPerfect) {
                            Spacer(Modifier.width(4.dp)); Badge("AP", Color(0xFF8E6FE0))
                        } else if (record.fullCombo) {
                            Spacer(Modifier.width(4.dp)); Badge("FC", Color(0xFF3FB27F))
                        }
                    }
                } else {
                    Text(stringResource(R.string.songs_no_record), style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
            }
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Ink, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text,
        color = Color.White,
        fontWeight = FontWeight.Black,
        fontSize = 10.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}
