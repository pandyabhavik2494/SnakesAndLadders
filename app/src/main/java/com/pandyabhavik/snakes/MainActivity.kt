package com.pandyabhavik.snakes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pandyabhavik.snakes.ui.GameScreen
import com.pandyabhavik.snakes.ui.GameViewModel
import com.pandyabhavik.snakes.ui.theme.SnakesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GameViewModel = viewModel(factory = GameViewModel.factory(application))
            val state by viewModel.state.collectAsStateWithLifecycle()
            val theme = state.settings.theme
            // Status-bar icons follow the theme: dark icons on the light Sakura table.
            LaunchedEffect(theme.light) {
                val bars = if (theme.light) SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            SnakesTheme(theme) {
                GameScreen(viewModel)
            }
        }
    }
}
