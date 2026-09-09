package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VibeApplication
import com.example.data.model.ResumeOption
import com.example.data.model.VideoItem
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.ResumePlaybackDialog
import com.example.ui.components.VideoCard
import com.example.ui.theme.VibeTextPrimary
import com.example.ui.theme.VibeTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderVideosScreen(
    folderName: String,
    onBack: () -> Unit,
    onPlayVideo: (video: VideoItem, startPositionMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = VibeApplication.instance
    val mediaRepository = app.mediaRepository
    val settingsRepository = app.settingsRepository

    val settings by settingsRepository.settings.collectAsState()
    val allVideos by mediaRepository.videosFlow.collectAsState(initial = emptyList())

    val folderVideos = remember(allVideos, folderName, settings.sortOption) {
        val filtered = allVideos.filter { it.bucketName.equals(folderName, ignoreCase = true) }
        mediaRepository.sortVideos(filtered, settings.sortOption)
    }

    var videoForDetails by remember { mutableStateOf<VideoItem?>(null) }
    var videoForResume by remember { mutableStateOf<VideoItem?>(null) }
    var videoForDelete by remember { mutableStateOf<VideoItem?>(null) }

    val handleVideoClick: (VideoItem) -> Unit = { video ->
        if (video.lastPositionMs > 5000L && video.durationMs > 0 && video.lastPositionMs < video.durationMs - 5000L) {
            when (settings.resumeOption) {
                ResumeOption.ASK -> videoForResume = video
                ResumeOption.ALWAYS -> onPlayVideo(video, video.lastPositionMs)
                ResumeOption.NEVER -> onPlayVideo(video, 0L)
            }
        } else {
            onPlayVideo(video, 0L)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = VibeTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${folderVideos.size} ${if (folderVideos.size == 1) "video" else "videos"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VibeTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(folderVideos, key = { it.uri.toString() }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { handleVideoClick(video) },
                        onToggleFavorite = { scope.launch { mediaRepository.toggleFavorite(video) } },
                        onPlayAudioOnly = { onPlayVideo(video, 0L) },
                        onShare = { shareVideo(context, video) },
                        onDelete = { videoForDelete = video },
                        onShowDetails = { videoForDetails = video }
                    )
                }
            }
        }

        // Dialogs
        videoForDetails?.let { video ->
            FileDetailsDialog(
                video = video,
                onDismiss = { videoForDetails = null }
            )
        }

        videoForResume?.let { video ->
            ResumePlaybackDialog(
                videoTitle = video.title,
                lastPositionMs = video.lastPositionMs,
                onResume = {
                    val pos = video.lastPositionMs
                    videoForResume = null
                    onPlayVideo(video, pos)
                },
                onStartOver = {
                    videoForResume = null
                    onPlayVideo(video, 0L)
                },
                onDismiss = { videoForResume = null }
            )
        }

        videoForDelete?.let { video ->
            DeleteConfirmDialog(
                videoTitle = video.title,
                onConfirm = {
                    scope.launch { mediaRepository.deleteVideo(video) }
                    videoForDelete = null
                },
                onDismiss = { videoForDelete = null }
            )
        }
    }
}

private fun shareVideo(context: Context, video: VideoItem) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = video.mimeType
            putExtra(Intent.EXTRA_STREAM, video.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share \"${video.title}\""))
    } catch (_: Exception) {}
}
