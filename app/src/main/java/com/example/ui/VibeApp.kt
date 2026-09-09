package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.VibeApplication
import com.example.data.model.VideoItem
import com.example.ui.screens.FolderVideosScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.VibePlayerTheme
import kotlinx.coroutines.launch

sealed interface VibeScreen {
    data object Home : VibeScreen
    data class Folder(val folderName: String) : VibeScreen
    data class Player(val video: VideoItem, val startPositionMs: Long = 0L) : VibeScreen
    data object Settings : VibeScreen
}

@Composable
fun VibeApp(
    externalVideoUri: Uri? = null,
    modifier: Modifier = Modifier
) {
    val app = VibeApplication.instance
    val settings by app.settingsRepository.settings.collectAsState()
    val mediaRepository = app.mediaRepository
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<VibeScreen>(VibeScreen.Home) }

    // Handle external video launch if opened via file manager or other app
    LaunchedEffect(externalVideoUri) {
        if (externalVideoUri != null) {
            scope.launch {
                val videoItem = mediaRepository.resolveExternalUri(externalVideoUri)
                currentScreen = VibeScreen.Player(video = videoItem, startPositionMs = 0L)
            }
        }
    }

    VibePlayerTheme(
        themeMode = settings.themeMode,
        accentIndex = settings.accentColorIndex
    ) {
        Surface(modifier = modifier.fillMaxSize()) {
            when (val screen = currentScreen) {
                is VibeScreen.Home -> {
                    HomeScreen(
                        onPlayVideo = { video, startPos ->
                            currentScreen = VibeScreen.Player(video, startPos)
                        },
                        onOpenFolder = { folderName ->
                            currentScreen = VibeScreen.Folder(folderName)
                        },
                        onOpenSettings = {
                            currentScreen = VibeScreen.Settings
                        }
                    )
                }

                is VibeScreen.Folder -> {
                    BackHandler { currentScreen = VibeScreen.Home }
                    FolderVideosScreen(
                        folderName = screen.folderName,
                        onBack = { currentScreen = VibeScreen.Home },
                        onPlayVideo = { video, startPos ->
                            currentScreen = VibeScreen.Player(video, startPos)
                        }
                    )
                }

                is VibeScreen.Player -> {
                    BackHandler { currentScreen = VibeScreen.Home }
                    PlayerScreen(
                        video = screen.video,
                        startPositionMs = screen.startPositionMs,
                        onBack = { currentScreen = VibeScreen.Home },
                        onPlayNext = { nextVideo ->
                            currentScreen = VibeScreen.Player(nextVideo, 0L)
                        }
                    )
                }

                is VibeScreen.Settings -> {
                    BackHandler { currentScreen = VibeScreen.Home }
                    SettingsScreen(
                        onBack = { currentScreen = VibeScreen.Home }
                    )
                }
            }
        }
    }
}
