package com.bocchifan.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.bocchifan.app.game.Chart
import com.bocchifan.app.game.CutIn
import com.bocchifan.app.game.Difficulty
import com.bocchifan.app.game.GameEngine
import com.bocchifan.app.game.GameView
import com.bocchifan.app.game.Hud
import com.bocchifan.app.game.HudState
import com.bocchifan.app.game.LANES
import com.bocchifan.app.game.Note
import com.bocchifan.app.ui.theme.BocchiTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Vista previa de posibles fondos de la partida (solo para comparar, no forma parte de la app).
 * Ejecutar con la variable BOCCHI_BACKDROPS apuntando a una carpeta con fondo1.png, fondo2.png...
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "es-w860dp-h400dp-xxhdpi")
class BackdropPreviewTest {
    @get:Rule
    val compose = createComposeRule()

    private fun preview(i: Int) {
        val dir = System.getenv("BOCCHI_BACKDROPS")
        assumeTrue(dir != null)
        val file = File(dir, "fondo$i.png")
        assumeTrue(file.exists())
        val bitmap = BitmapFactory.decodeFile(file.absolutePath).asImageBitmap()

        compose.mainClock.autoAdvance = false
        compose.setContent {
            BocchiTheme {
                val notes = remember { List(40) { k -> Note(1000 + k * 260, (k * 3) % LANES, if (k % 9 == 4) 1000 + k * 260 + 900 else null) } }
                val chart = remember { Chart(notes, 20_000) }
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
                    Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    GameView(chart, engine, IntArray(LANES), emptyList(), { now }, approachMs = 1400, modifier = Modifier.fillMaxSize(), overBackdrop = true)
                    Hud("Fondo $i", Difficulty.Normal, hud, { now }, onPause = {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("${System.getenv("BOCCHI_BACKDROPS")}/preview$i.png")
    }

    @Test fun fondo1() = preview(1)
    @Test fun fondo2() = preview(2)
    @Test fun fondo3() = preview(3)
    @Test fun fondo4() = preview(4)
    @Test fun fondo5() = preview(5)
    @Test fun fondo6() = preview(6)
    @Test fun fondo7() = preview(7)
}
