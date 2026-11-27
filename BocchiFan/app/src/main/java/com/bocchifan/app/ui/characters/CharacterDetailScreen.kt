package com.bocchifan.app.ui.characters

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocchifan.app.R
import com.bocchifan.app.data.Character
import com.bocchifan.app.data.CharacterRepository
import com.bocchifan.app.data.text
import com.bocchifan.app.data.texts
import com.bocchifan.app.ui.components.GuitarPickShape
import com.bocchifan.app.ui.components.StickerCard
import com.bocchifan.app.ui.components.SystemBarsAppearance
import com.bocchifan.app.ui.components.notebookPaper
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.InkSoft

/** Ficha completa de un personaje: retrato, datos, biografía y curiosidades. */
@Composable
fun CharacterDetailScreen(id: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val character = remember(id) { CharacterRepository.byId(context, id) }
    if (character == null) {
        // El personaje ya no existe en el JSON (por ejemplo, tras editarlo): volver a la lista.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    SystemBarsAppearance(lightBackground = contentColorOn(character.color) == Ink, lightNavigationBar = com.bocchifan.app.ui.theme.PaperIsLight)

    Column(
        Modifier
            .fillMaxSize()
            .notebookPaper()
            .verticalScroll(rememberScrollState()),
    ) {
        DetailHeader(character, onBack)
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ProfileCard(character)

            val bio = character.bio.texts()
            if (bio.isNotEmpty()) {
                InfoCard(Icons.Rounded.MenuBook, stringResource(R.string.section_bio), character.color) {
                    bio.forEachIndexed { i, paragraph ->
                        if (i > 0) Spacer(Modifier.height(10.dp))
                        Text(paragraph, style = MaterialTheme.typography.bodyLarge, color = Ink)
                    }
                }
            }

            val facts = character.facts.texts()
            if (facts.isNotEmpty()) {
                InfoCard(Icons.Rounded.AutoAwesome, stringResource(R.string.section_facts), character.color) {
                    facts.forEachIndexed { i, fact ->
                        if (i > 0) Spacer(Modifier.height(10.dp))
                        Row {
                            Box(
                                Modifier
                                    .padding(top = 3.dp)
                                    .size(width = 14.dp, height = 17.dp)
                                    .background(character.color, GuitarPickShape)
                                    .border(1.5.dp, Ink, GuitarPickShape),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(fact, style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.characters_source),
                style = MaterialTheme.typography.labelSmall,
                color = com.bocchifan.app.ui.theme.OnPaperSoft,
            )
        }
    }
}

/** Cabecera con el color del personaje, rayas diagonales, retrato grande y nombres. */
@Composable
private fun DetailHeader(character: Character, onBack: () -> Unit) {
    val content = contentColorOn(character.color)
    val shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(character.color)
            .drawBehind {
                // Rayas diagonales suaves, como un póster.
                val step = 22.dp.toPx()
                val stroke = 8.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        Color.White.copy(alpha = 0.14f),
                        Offset(x, size.height),
                        Offset(x + size.height, 0f),
                        stroke,
                    )
                    x += step
                }
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                StickerCard(
                    color = Color.White,
                    shadow = 4.dp,
                    showTape = false,
                    sound = com.bocchifan.app.ui.components.UiSound.Back,
                    onClick = onBack,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Ink,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp),
                    )
                }
            }
            CharacterPortrait(character, size = 156.dp, borderWidth = 4.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                character.name,
                color = content,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = 32.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center,
            )
            character.nameJp?.let {
                Text(it, color = content.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(character.role.text(), filled = true)
            }
            character.nickname?.let {
                Spacer(Modifier.height(8.dp))
                Chip("“${it.text()}”", filled = false)
            }
        }
    }
}

@Composable
private fun Chip(text: String, filled: Boolean) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Ink,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(shape)
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.6f))
            .border(2.dp, Ink, shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

/** Datos en una cuadrícula de dos columnas (solo los que existen). */
@Composable
private fun ProfileCard(character: Character) {
    val stats = buildList {
        character.age?.let { add(R.string.stat_age to it.text()) }
        character.birthday?.let { add(R.string.stat_birthday to it.text()) }
        character.height?.let { add(R.string.stat_height to it) }
        character.bloodType?.let { add(R.string.stat_blood to it) }
        character.band?.let { add(R.string.stat_band to it) }
        character.voice?.let { add(R.string.stat_voice to it) }
        character.school?.let { add(R.string.stat_school to it.text()) }
    }
    if (stats.isEmpty()) return

    InfoCard(Icons.Rounded.Badge, stringResource(R.string.section_profile), character.color) {
        stats.chunked(2).forEachIndexed { i, pair ->
            if (i > 0) Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (label, value) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(character.color.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = InkSoft)
                        Spacer(Modifier.height(2.dp))
                        Text(value, style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                }
            }
        }
    }
}

/** Tarjeta blanca con cinta, ícono y título subrayado con el color del personaje. */
@Composable
private fun InfoCard(icon: ImageVector, title: String, accent: Color, content: @Composable () -> Unit) {
    StickerCard(color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    modifier = Modifier.drawBehind {
                        drawRect(
                            accent.copy(alpha = 0.6f),
                            topLeft = Offset(-3.dp.toPx(), size.height * 0.6f),
                            size = androidx.compose.ui.geometry.Size(size.width + 6.dp.toPx(), size.height * 0.3f),
                        )
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
