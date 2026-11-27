package com.bocchifan.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.res.stringResource
import com.bocchifan.app.R
import com.bocchifan.app.game.Difficulty
import com.bocchifan.app.game.GameScreen
import com.bocchifan.app.ui.characters.CharacterDetailScreen
import com.bocchifan.app.ui.characters.CharactersScreen
import com.bocchifan.app.ui.menu.MenuScreen
import com.bocchifan.app.ui.settings.SettingsScreen
import com.bocchifan.app.ui.songs.SongSelectScreen
import com.bocchifan.app.ui.player.PlayerScreen
import com.bocchifan.app.ui.player.SongListScreen
import com.bocchifan.app.ui.theme.BocchiPinkDeep
import com.bocchifan.app.ui.theme.RyoBlue
import com.bocchifan.app.ui.title.TitleScreen

/** Pantallas de la app. [depth] define el sentido de la animación (avanzar o volver). */
sealed interface Screen {
    val depth: Int

    data object Title : Screen { override val depth = 0 }
    data object Menu : Screen { override val depth = 1 }
    data object Play : Screen { override val depth = 2 }
    data object Characters : Screen { override val depth = 2 }
    data object Songs : Screen { override val depth = 2 }
    data object Settings : Screen { override val depth = 2 }
    data class CharacterDetail(val id: String) : Screen { override val depth = 3 }
    data class Game(val songPath: String, val difficulty: Difficulty) : Screen { override val depth = 3 }
    data class Player(val songPath: String) : Screen { override val depth = 3 }
}

private fun Screen.toKey(): String = when (this) {
    is Screen.CharacterDetail -> "CharacterDetail:$id"
    is Screen.Game -> "Game:${difficulty.name}:$songPath"
    is Screen.Player -> "Player:$songPath"
    Screen.Title -> "Title"
    Screen.Menu -> "Menu"
    Screen.Play -> "Play"
    Screen.Characters -> "Characters"
    Screen.Songs -> "Songs"
    Screen.Settings -> "Settings"
}

private fun screenFromKey(key: String): Screen = when {
    key.startsWith("CharacterDetail:") -> Screen.CharacterDetail(key.removePrefix("CharacterDetail:"))
    key.startsWith("Player:") -> Screen.Player(key.removePrefix("Player:"))
    key.startsWith("Game:") -> key.removePrefix("Game:").let { rest ->
        val d = rest.substringBefore(':')
        Screen.Game(rest.substringAfter(':'), runCatching { Difficulty.valueOf(d) }.getOrDefault(Difficulty.Normal))
    }
    key == "Menu" -> Screen.Menu
    key == "Play" -> Screen.Play
    key == "Characters" -> Screen.Characters
    key == "Songs" -> Screen.Songs
    key == "Settings" -> Screen.Settings
    else -> Screen.Title
}

private val backStackSaver = listSaver<SnapshotStateList<Screen>, String>(
    save = { stack -> stack.map { it.toKey() } },
    restore = { keys -> keys.map(::screenFromKey).toMutableStateList() },
)

/** Raíz de la app: pila de pantallas simple con botón atrás y transiciones. */
@Composable
fun BocchiApp() {
    val backStack = rememberSaveable(saver = backStackSaver) { mutableStateListOf<Screen>(Screen.Title) }
    val current = backStack.last()

    fun push(screen: Screen) {
        backStack.add(screen)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    AnimatedContent(
        targetState = current,
        transitionSpec = {
            if (initialState == Screen.Title) {
                fadeIn(tween(500)) togetherWith fadeOut(tween(400))
            } else {
                val forward = targetState.depth > initialState.depth
                (slideInHorizontally(tween(320)) { if (forward) it else -it / 3 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(320)) { if (forward) -it / 3 else it } + fadeOut(tween(320)))
            }
        },
        label = "navigation",
    ) { screen ->
        when (screen) {
            Screen.Title -> TitleScreen(
                onStart = {
                    // La portada no vuelve a aparecer al pulsar atrás desde el menú.
                    backStack.clear()
                    backStack.add(Screen.Menu)
                },
            )
            Screen.Menu -> MenuScreen(onNavigate = ::push)
            Screen.Play -> SongSelectScreen(
                title = stringResource(R.string.menu_play),
                accent = BocchiPinkDeep,
                onBack = ::pop,
                onPlay = { song, d -> push(Screen.Game(song.path, d)) },
            )
            is Screen.Game -> GameScreen(songPath = screen.songPath, difficulty = screen.difficulty, onExit = ::pop)
            Screen.Characters -> CharactersScreen(
                onBack = ::pop,
                onOpen = { id -> push(Screen.CharacterDetail(id)) },
            )
            is Screen.CharacterDetail -> CharacterDetailScreen(id = screen.id, onBack = ::pop)
            Screen.Songs -> SongListScreen(onBack = ::pop, onOpen = { song -> push(Screen.Player(song.path)) })
            is Screen.Player -> PlayerScreen(startPath = screen.songPath, onBack = ::pop)
            Screen.Settings -> SettingsScreen(onBack = ::pop)
        }
    }
}
