package com.bocchifan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bocchifan.app.game.Chart
import com.bocchifan.app.game.Difficulty
import com.bocchifan.app.game.GameEngine
import com.bocchifan.app.game.GameResult
import com.bocchifan.app.game.GameView
import com.bocchifan.app.game.Hud
import com.bocchifan.app.game.HudState
import com.bocchifan.app.game.Judgment
import com.bocchifan.app.game.LANES
import com.bocchifan.app.game.Note
import com.bocchifan.app.game.Rank
import com.bocchifan.app.game.ResultsOverlay
import com.bocchifan.app.ui.songs.SongSelectScreen
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.StageDark
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.bocchifan.app.ui.characters.CharacterDetailScreen
import com.bocchifan.app.ui.characters.CharactersScreen
import com.bocchifan.app.ui.menu.MenuScreen
import com.bocchifan.app.ui.placeholder.ComingSoonScreen
import com.bocchifan.app.ui.settings.SettingsScreen
import com.bocchifan.app.ui.theme.BocchiTheme
import com.bocchifan.app.ui.theme.NijikaYellow
import com.bocchifan.app.ui.title.TitleScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas de cada pantalla sin emulador.
 * Ejecutar: ./gradlew recordRoborazziDebug  → imágenes en app/screenshots/
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "es-w360dp-h780dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String, content: @Composable () -> Unit) {
        // Reloj manual: las animaciones infinitas no dejan que la UI quede "quieta" por sí sola.
        compose.mainClock.autoAdvance = false
        compose.setContent { BocchiTheme { content() } }
        compose.mainClock.advanceTimeBy(1600)
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Test
    fun title() = shot("01_title") { TitleScreen(onStart = {}) }

    @Test
    fun menu() = shot("02_menu") { MenuScreen(onNavigate = {}) }

    @Test
    fun comingSoon() = shot("03_coming_soon") {
        ComingSoonScreen(title = "Personajes", accent = NijikaYellow, phase = "Fase 2", onBack = {})
    }

    @Test
    fun settings() = shot("04_settings") { SettingsScreen(onBack = {}) }

    @Test
    @Config(qualifiers = "+h1900dp")
    fun characters() = shot("06_characters") { CharactersScreen(onBack = {}, onOpen = {}) }

    @Test
    fun characterBocchi() = shot("07_character_bocchi") { CharacterDetailScreen(id = "bocchi", onBack = {}) }

    @Test
    fun characterKikuri() = shot("08_character_kikuri") { CharacterDetailScreen(id = "kikuri", onBack = {}) }

    private fun darkShot(name: String, content: @Composable () -> Unit) {
        com.bocchifan.app.ui.theme.Palette.setMode(com.bocchifan.app.ui.theme.ThemeMode.Dark)
        try {
            shot(name, content)
        } finally {
            com.bocchifan.app.ui.theme.Palette.setMode(com.bocchifan.app.ui.theme.ThemeMode.Light)
        }
    }

    @Test
    fun menuDark() = darkShot("13_menu_dark") { MenuScreen(onNavigate = {}) }

    @Test
    @Config(qualifiers = "+h1900dp")
    fun charactersDark() = darkShot("14_characters_dark") { CharactersScreen(onBack = {}, onOpen = {}) }

    @Test
    @Config(qualifiers = "+h1400dp")
    fun settingsDark() = darkShot("15_settings_dark") { SettingsScreen(onBack = {}) }

    @Test
    fun songSelect() = shot("10_song_select") {
        SongSelectScreen(title = "Jugar", accent = BocchiPinkDeep, onBack = {}, onPlay = { _, _ -> })
    }

    /** Partida simulada en un instante fijo: notas, nota larga sostenida, destellos y marcador. */
    @Test
    @Config(qualifiers = "es-w860dp-h400dp-xxhdpi")
    fun gameplay() = shot("11_gameplay") {
        val notes = buildList {
            for (i in 0 until 40) add(Note(1000 + i * 260, (i * 3) % LANES, if (i % 9 == 4) 1000 + i * 260 + 900 else null))
        }
        val chart = Chart(notes, 20_000)
        val engine = remember { GameEngine(chart) }
        val pressed = remember { IntArray(LANES) }
        val now = 3900
        val hud = remember { HudState() }
        remember {
            notes.filter { it.timeMs <= now - 60 }.forEach { engine.press(it.lane, it.timeMs + 20) }
            // Sostener la nota larga en curso.
            notes.firstOrNull { it.endMs != null && it.timeMs <= now && it.endMs!! > now }?.let { pressed[it.lane] = 1 }
            engine.update(now)
            hud.update(engine, now, chart.durationMs)
            hud.cutIn = com.bocchifan.app.game.CutIn("bocchi", 50, now - 700)
            0
        }
        val hits = remember { engine.events.map { it.copy(atMs = now - 120) }.takeLast(3) }
        Box(Modifier.fillMaxSize()) {
            GameView(chart, engine, pressed, hits, { now }, approachMs = 1400, modifier = Modifier.fillMaxSize())
            Hud("Guitar, Loneliness and Blue Planet", Difficulty.Hard, hud, { now - 100 }, onPause = {})
        }
    }

    /** Captura para la documentación: partida sin arte del anime (sin cut-in ni fondo). */
    @Test
    @Config(qualifiers = "es-w860dp-h400dp-xxhdpi")
    fun gameplayDocs() = shot("docs_gameplay") {
        val notes = buildList {
            for (i in 0 until 40) add(Note(1000 + i * 260, (i * 3) % LANES, if (i % 9 == 4) 1000 + i * 260 + 900 else null))
        }
        val chart = Chart(notes, 20_000)
        val engine = remember { GameEngine(chart) }
        val now = 3900
        val hud = remember {
            HudState().also { h ->
                notes.filter { it.timeMs <= now - 200 }.forEach { engine.press(it.lane, it.timeMs + 20) }
                engine.update(now - 200)
                h.update(engine, now, chart.durationMs)
            }
        }
        Box(Modifier.fillMaxSize()) {
            GameView(chart, engine, IntArray(LANES), emptyList(), { now }, approachMs = 1400, modifier = Modifier.fillMaxSize())
            Hud("Bocchi Fan", Difficulty.Hard, hud, { now }, onPause = {})
        }
    }

    @Test
    @Config(qualifiers = "es-w860dp-h400dp-xxhdpi")
    fun results() = shot("12_results") {
        val result = GameResult(
            score = 912_345,
            accuracy = 0.912f,
            rank = Rank.A,
            maxCombo = 214,
            counts = mapOf(Judgment.Perfect to 401, Judgment.Great to 120, Judgment.Good to 20, Judgment.Bad to 6, Judgment.Miss to 9),
            failed = false,
        )
        Box(Modifier.fillMaxSize().background(StageDark)) {
            ResultsOverlay("Seishun Complex", Difficulty.Expert, result, newRecord = true, onRetry = {}, onExit = {})
        }
    }

    @Test
    @Config(qualifiers = "+en")
    fun characterNijikaEnglish() = shot("09_character_nijika_en") { CharacterDetailScreen(id = "nijika", onBack = {}) }

    @Test
    @Config(qualifiers = "+en")
    fun menuEnglish() = shot("05_menu_en") { MenuScreen(onNavigate = {}) }
}
