package com.bocchifan.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bocchifan.app.audio.MusicPlayer
import com.bocchifan.app.data.GameSettings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.bocchifan.app.navigation.BocchiApp
import com.bocchifan.app.ui.components.TapFxOverlay
import com.bocchifan.app.ui.components.UiSounds
import com.bocchifan.app.ui.theme.BocchiTheme

// AppCompatActivity permite cambiar el idioma desde la app (español / inglés).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicPlayer.init(this)
        GameSettings.init(this)
        enableEdgeToEdge()
        UiSounds.init(this)
        com.bocchifan.app.ui.theme.Palette.init(this)
        setContent {
            BocchiTheme {
                Box(Modifier.fillMaxSize()) {
                    BocchiApp()
                    // Estrellitas, púas y notas al tocar, encima de todo.
                    TapFxOverlay(Modifier.fillMaxSize())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MusicPlayer.onForeground()
    }

    override fun onStop() {
        super.onStop()
        // Al cambiar de idioma la pantalla se recrea: la música sigue sin cortarse.
        if (!isChangingConfigurations) MusicPlayer.onBackground()
    }
}
