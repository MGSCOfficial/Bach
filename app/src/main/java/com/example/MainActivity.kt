package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AudioTrack
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SongEditorScreen
import com.example.ui.theme.SonicTagTheme
import com.example.ui.theme.StudioDarkBg
import com.example.ui.viewmodel.MusicViewModel

sealed class AppScreen {
    object Library : AppScreen()
    data class Editor(val track: AudioTrack) : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SonicTagTheme {
                val viewModel: MusicViewModel = viewModel()
                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Library) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioDarkBg
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            is AppScreen.Library -> {
                                LibraryScreen(
                                    viewModel = viewModel,
                                    onTrackSelect = { track ->
                                        viewModel.openTrackEditor(track)
                                        currentScreen = AppScreen.Editor(track)
                                    }
                                )
                            }
                            is AppScreen.Editor -> {
                                SongEditorScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        currentScreen = AppScreen.Library
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
