package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AudioTrack
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SongEditorScreen
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SonicTagTheme
import com.example.ui.viewmodel.MusicViewModel

sealed class AppScreen {
    object Main : AppScreen()
    data class Editor(val track: AudioTrack) : AppScreen()
}

enum class MainTab(val label: String, val icon: ImageVector) {
    SONGS("Canciones", Icons.Default.MusicNote),
    FOLDERS("Carpetas", Icons.Default.Folder),
    SETTINGS("Ajustes", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val playerState by viewModel.audioPlayer.playerState.collectAsStateWithLifecycle()
            val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()

            val activePlayingTrack = remember(playerState.activeTrackId, libraryState.tracks) {
                libraryState.tracks.firstOrNull { it.id == playerState.activeTrackId }
            }

            SonicTagTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor
            ) {
                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Main) }
                var currentTab by remember { mutableStateOf(MainTab.SONGS) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            is AppScreen.Main -> {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    containerColor = MaterialTheme.colorScheme.background,
                                    bottomBar = {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .navigationBarsPadding()
                                        ) {
                                            MiniPlayerBar(
                                                playerState = playerState,
                                                track = activePlayingTrack,
                                                onPlayPauseClick = { viewModel.audioPlayer.togglePlayPause() },
                                                onSeek = { pos -> viewModel.audioPlayer.seekTo(pos) },
                                                onOpenEditor = {
                                                    activePlayingTrack?.let {
                                                        viewModel.openTrackEditor(it)
                                                        currentScreen = AppScreen.Editor(it)
                                                    }
                                                }
                                            )

                                            NavigationBar(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                tonalElevation = 8.dp
                                            ) {
                                                MainTab.values().forEach { tab ->
                                                    val isSelected = currentTab == tab
                                                    NavigationBarItem(
                                                        selected = isSelected,
                                                        onClick = { currentTab = tab },
                                                        icon = {
                                                            Icon(
                                                                imageVector = tab.icon,
                                                                contentDescription = tab.label
                                                            )
                                                        },
                                                        label = {
                                                            Text(
                                                                text = tab.label,
                                                                fontSize = 11.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                            )
                                                        },
                                                        colors = NavigationBarItemDefaults.colors(
                                                            selectedIconColor = Color.Black,
                                                            selectedTextColor = EmeraldAccent,
                                                            indicatorColor = EmeraldAccent,
                                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                ) { paddingValues ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(paddingValues)
                                    ) {
                                        when (currentTab) {
                                            MainTab.SONGS -> {
                                                LibraryScreen(
                                                    viewModel = viewModel,
                                                    onTrackSelect = { track ->
                                                        viewModel.openTrackEditor(track)
                                                        currentScreen = AppScreen.Editor(track)
                                                    }
                                                )
                                            }
                                            MainTab.FOLDERS -> {
                                                FoldersScreen(
                                                    viewModel = viewModel,
                                                    onTrackSelect = { track ->
                                                        viewModel.openTrackEditor(track)
                                                        currentScreen = AppScreen.Editor(track)
                                                    }
                                                )
                                            }
                                            MainTab.SETTINGS -> {
                                                SettingsScreen(
                                                    viewModel = viewModel
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is AppScreen.Editor -> {
                                SongEditorScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        currentScreen = AppScreen.Main
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
