package com.bocchifan.app.ui.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bocchifan.app.R
import com.bocchifan.app.data.Character
import com.bocchifan.app.data.CharacterGroup
import com.bocchifan.app.data.CharacterRepository
import com.bocchifan.app.data.text
import com.bocchifan.app.ui.components.BandStripes
import com.bocchifan.app.ui.components.ScreenTopBar
import com.bocchifan.app.ui.components.StickerCard
import com.bocchifan.app.ui.components.SystemBarsAppearance
import com.bocchifan.app.ui.components.notebookPaper
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.InkSoft
import com.bocchifan.app.ui.theme.NijikaYellow

/** Lista de personajes: Kessoku Band en tarjetas grandes y los secundarios en filas. */
@Composable
fun CharactersScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    SystemBarsAppearance(lightBackground = com.bocchifan.app.ui.theme.PaperIsLight)
    val context = LocalContext.current
    val characters = remember { CharacterRepository.all(context) }
    val band = characters.filter { it.group == CharacterGroup.Kessoku }
    val others = characters.filter { it.group == CharacterGroup.Secondary }

    Column(
        Modifier
            .fillMaxSize()
            .notebookPaper()
            .systemBarsPadding(),
    ) {
        ScreenTopBar(title = stringResource(R.string.menu_characters), accent = NijikaYellow, onBack = onBack)

        if (characters.isEmpty()) {
            Text(
                stringResource(R.string.characters_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = com.bocchifan.app.ui.theme.OnPaperSoft,
                modifier = Modifier.padding(24.dp),
            )
            return@Column
        }

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SectionLabel(stringResource(R.string.characters_kessoku))
            BandStripes(
                Modifier
                    .width(120.dp)
                    .height(8.dp),
            )
            Spacer(Modifier.height(18.dp))
            band.chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    pair.forEachIndexed { col, character ->
                        BandCard(
                            character = character,
                            rotation = if ((row + col) % 2 == 0) -1.5f else 1.5f,
                            onClick = { onOpen(character.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // Si la fila queda impar, deja el hueco para que la tarjeta no se estire.
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (others.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionLabel(stringResource(R.string.characters_secondary))
                Spacer(Modifier.height(14.dp))
                others.forEach { character ->
                    SecondaryRow(character, onClick = { onOpen(character.id) })
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.characters_source),
                style = MaterialTheme.typography.labelSmall,
                color = com.bocchifan.app.ui.theme.OnPaperSoft,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = com.bocchifan.app.ui.theme.OnPaper)
}

/** Tarjeta grande de una integrante de Kessoku Band, con su color. */
@Composable
private fun BandCard(character: Character, rotation: Float, onClick: () -> Unit, modifier: Modifier) {
    val content = contentColorOn(character.color)
    StickerCard(
        color = character.color,
        rotation = rotation,
        onClick = onClick,
        modifier = modifier.aspectRatio(0.8f),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(6.dp))
            CharacterPortrait(character, size = 92.dp)
            Spacer(Modifier.weight(1f))
            Text(
                character.name,
                style = MaterialTheme.typography.titleMedium,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                character.role.text(),
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Fila de un personaje secundario. */
@Composable
private fun SecondaryRow(character: Character, onClick: () -> Unit) {
    StickerCard(
        color = Color.White,
        shadow = 4.dp,
        showTape = false,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CharacterPortrait(character, size = 52.dp, borderWidth = 2.5.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    character.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    character.role.text(),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
