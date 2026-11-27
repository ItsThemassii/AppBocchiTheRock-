package com.bocchifan.app.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SportsEsports
import com.bocchifan.app.data.GameSettings
import kotlin.math.roundToInt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.bocchifan.app.R
import com.bocchifan.app.audio.MusicPlayer
import com.bocchifan.app.ui.components.ScreenTopBar
import com.bocchifan.app.ui.components.StickerCard
import com.bocchifan.app.ui.components.UiSounds
import com.bocchifan.app.ui.components.UiSound
import com.bocchifan.app.ui.components.BocchiSwitch
import com.bocchifan.app.ui.components.MadeByMassi
import com.bocchifan.app.ui.theme.Palette
import com.bocchifan.app.ui.theme.ThemeMode
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.ui.unit.sp
import com.bocchifan.app.ui.components.SystemBarsAppearance
import com.bocchifan.app.ui.components.notebookPaper
import com.bocchifan.app.ui.theme.BocchiPink
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.Ink
import com.bocchifan.app.ui.theme.InkSoft
import com.bocchifan.app.ui.theme.KitaRed

/** Idiomas que ofrece la app. Una etiqueta vacía significa "seguir al sistema". */
private enum class AppLanguage(val tag: String) { System(""), Spanish("es"), English("en") }

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    SystemBarsAppearance(lightBackground = com.bocchifan.app.ui.theme.PaperIsLight)

    // Al cambiar el idioma la pantalla se vuelve a crear, así que basta con leerlo una vez.
    val current = remember {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        AppLanguage.entries.firstOrNull { it.tag.isNotEmpty() && tags.startsWith(it.tag) } ?: AppLanguage.System
    }

    Column(
        Modifier
            .fillMaxSize()
            .notebookPaper()
            .systemBarsPadding(),
    ) {
        ScreenTopBar(title = stringResource(R.string.menu_settings), accent = KitaRed, onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section(Icons.Rounded.DarkMode, stringResource(R.string.settings_theme)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.Light -> stringResource(R.string.theme_light)
                            ThemeMode.Dark -> stringResource(R.string.theme_dark)
                            ThemeMode.System -> stringResource(R.string.lang_system)
                        }
                        LanguageChip(
                            label = label,
                            selected = Palette.mode == mode,
                            modifier = Modifier.weight(1f),
                            onClick = { Palette.setMode(mode) },
                        )
                    }
                }
            }

            Section(Icons.Rounded.Language, stringResource(R.string.settings_language)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AppLanguage.entries.forEach { language ->
                        val label = when (language) {
                            AppLanguage.System -> stringResource(R.string.lang_system)
                            AppLanguage.Spanish -> stringResource(R.string.lang_es)
                            AppLanguage.English -> stringResource(R.string.lang_en)
                        }
                        LanguageChip(
                            label = label,
                            selected = language == current,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (language != current) {
                                    val locales = if (language.tag.isEmpty()) {
                                        LocaleListCompat.getEmptyLocaleList()
                                    } else {
                                        LocaleListCompat.forLanguageTags(language.tag)
                                    }
                                    AppCompatDelegate.setApplicationLocales(locales)
                                }
                            },
                        )
                    }
                }
            }

            Section(Icons.Rounded.MusicNote, stringResource(R.string.settings_music)) {
                MusicSettings()
            }

            Section(Icons.Rounded.SportsEsports, stringResource(R.string.settings_game)) {
                GameSettingsSection()
            }

            Section(Icons.Rounded.Info, stringResource(R.string.settings_about)) {
                Text(
                    stringResource(R.string.settings_about_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(12.dp))
                MadeByMassi(textColor = Ink, textSize = 15.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.settings_version, appVersion()),
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
private fun Section(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    StickerCard(color = Color.White, showTape = true, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun LanguageChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    StickerCard(
        color = if (selected) BocchiPink else Color.White,
        shadow = if (selected) 1.dp else 4.dp,
        showTape = false,
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
        )
    }
}

@Composable
private fun MusicSettings() {
    val uiSounds by UiSounds.enabled
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.settings_ui_sounds),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        BocchiSwitch(checked = uiSounds, onCheckedChange = UiSounds::setEnabled)
    }
    Spacer(Modifier.height(10.dp))
    if (!MusicPlayer.hasTracks) {
        Text(
            stringResource(R.string.settings_music_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )
        return
    }
    val enabled by MusicPlayer.enabled
    val volume by MusicPlayer.volume
    val sliderColors = SliderDefaults.colors(
        thumbColor = BocchiPinkDeep,
        activeTrackColor = BocchiPinkDeep,
        inactiveTrackColor = BocchiPink.copy(alpha = 0.4f),
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.settings_music_enabled),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        BocchiSwitch(checked = enabled, onCheckedChange = MusicPlayer::setEnabled)
    }
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.settings_music_volume), style = MaterialTheme.typography.labelLarge, color = InkSoft)
    Slider(
        value = volume,
        onValueChange = MusicPlayer::setVolume,
        onValueChangeFinished = { UiSounds.play(UiSound.Tap) },
        enabled = enabled,
        colors = sliderColors,
    )
}

@Composable
private fun GameSettingsSection() {
    val offset by GameSettings.offsetMs
    val tap by GameSettings.tapSound
    val songVolume by GameSettings.songVolume
    val sliderColors = SliderDefaults.colors(
        thumbColor = BocchiPinkDeep,
        activeTrackColor = BocchiPinkDeep,
        inactiveTrackColor = BocchiPink.copy(alpha = 0.4f),
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.settings_offset), style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
        Text("${if (offset > 0) "+" else ""}$offset ms", style = MaterialTheme.typography.titleMedium, color = Ink)
    }
    Slider(
        value = offset.toFloat(),
        onValueChange = { GameSettings.setOffset((it / 10).roundToInt() * 10) },
        onValueChangeFinished = { UiSounds.play(UiSound.Tap) },
        valueRange = -300f..300f,
        colors = sliderColors,
    )
    Text(stringResource(R.string.settings_offset_hint), style = MaterialTheme.typography.bodySmall, color = InkSoft)
    Spacer(Modifier.height(12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.settings_tap_sound), style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
        BocchiSwitch(checked = tap, onCheckedChange = GameSettings::setTapSound)
    }
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.settings_song_volume), style = MaterialTheme.typography.labelLarge, color = InkSoft)
    Slider(
        value = songVolume,
        onValueChange = GameSettings::setSongVolume,
        onValueChangeFinished = { UiSounds.play(UiSound.Tap) },
        colors = sliderColors,
    )
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "?"
    }
}
