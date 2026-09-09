@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.ui.screens


import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.VibeApplication
import com.example.service.VibePlaybackService
import com.example.data.model.TrackInfo
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoItem
import com.example.data.model.formatDuration
import com.example.ui.theme.VibeRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    playlist: List<VideoItem> = emptyList(),
    startPositionMs: Long = 0L,
    onBack: () -> Unit,
    onPlayNext: (VideoItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val app = VibeApplication.instance
    val playerManager = app.playerManager
    val settingsRepository = app.settingsRepository
    val mediaRepository = app.mediaRepository

    val settings by settingsRepository.settings.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPosition by playerManager.currentPositionMs.collectAsState()
    val duration by playerManager.durationMs.collectAsState()
    val bufferedPosition by playerManager.bufferedPositionMs.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()
    val aspectRatio by playerManager.aspectRatio.collectAsState()
    val playerError by playerManager.playerError.collectAsState()

    val audioTracks by playerManager.audioTracks.collectAsState()
    val subtitleTracks by playerManager.subtitleTracks.collectAsState()
    val selectedAudioTrack by playerManager.selectedAudioTrack.collectAsState()
    val selectedSubtitleTrack by playerManager.selectedSubtitleTrack.collectAsState()
    val audioDelayMs by playerManager.audioDelayMs.collectAsState()
    val subtitleDelayMs by playerManager.subtitleDelayMs.collectAsState()

    // Control visibility and locking
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    // Dialog & bottom sheet states
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showDelayDialog by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    // Gesture feedback states
    var brightnessFeedback by remember { mutableFloatStateOf(-1f) }
    var volumeFeedback by remember { mutableFloatStateOf(-1f) }
    var seekFeedback by remember { mutableLongStateOf(-1L) }
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    // Subtitle file picker launcher
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            playerManager.loadExternalSubtitle(uri)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    // Auto-hide controls timer
    LaunchedEffect(controlsVisible, isPlaying, isLocked) {
        if (controlsVisible && isPlaying && !isLocked && settings.autoHideControlsDelayMs > 0) {
            delay(settings.autoHideControlsDelayMs)
            controlsVisible = false
        }
    }

    // Player mode: automatically choose orientation from the video resolution and keep Android system bars visible.
    DisposableEffect(video.uri) {
        val window = activity?.window
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val landscapeVideo = video.resolution
            .split("x", "×")
            .mapNotNull { it.trim().toIntOrNull() }
            .let { if (it.size == 2) it[0] >= it[1] else true }

        if (activity != null) {
            activity.requestedOrientation = if (landscapeVideo) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }

        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.statusBarColor = Color.Black.toArgb()
            window.navigationBarColor = Color.Black.toArgb()
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.statusBarColor = Color.White.toArgb()
                window.navigationBarColor = Color.White.toArgb()
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
            @Suppress("UNUSED_VARIABLE")
            val ignored = previousOrientation
        }
    }

    // Start the MediaSessionService so playback survives leaving the Activity/home screen.
    LaunchedEffect(video.uri) {
        try {
            val serviceIntent = Intent(context, VibePlaybackService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) {
            // The MediaSessionService can still be used while the Activity is visible.
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Initialize playback and check for matching subtitle file automatically
    LaunchedEffect(video.uri) {
        // Find matching subtitle automatically if available
        val autoSub = mediaRepository.findMatchingSubtitle(video)
        playerManager.playVideo(
            video = video,
            startPositionMs = startPositionMs,
            externalSubtitleUri = autoSub,
            initialSpeed = settings.defaultSpeed
        )
    }

    // Save progress periodically and on leave
    DisposableEffect(video.uri) {
        onDispose {
            val player = playerManager.exoPlayer
            if (player != null) {
                val currentPos = player.currentPosition
                val dur = player.duration
                scope.launch {
                    mediaRepository.savePlaybackProgress(
                        uri = video.uri.toString(),
                        positionMs = currentPos,
                        durationMs = dur,
                        title = video.title
                    )
                }
            }
        }
    }

    // Handle Picture in Picture action
    val enterPiP: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val aspectRatioRational = Rational(16, 9)
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatioRational)
                    .build()
                activity?.enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }

    // Toggle screen orientation between sensor landscape and sensor portrait
    val toggleOrientation: () -> Unit = {
        activity?.let { act ->
            val currentOrientation = act.requestedOrientation
            act.requestedOrientation = if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Player Surface View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerManager.exoPlayer
                    useController = false // Custom Compose controls overlay
                    keepScreenOn = true
                    resizeMode = when (aspectRatio) {
                        VideoAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoAspectRatio.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        VideoAspectRatio.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        VideoAspectRatio.FOUR_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                        VideoAspectRatio.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                }
            },
            update = { playerView ->
                playerView.player = playerManager.exoPlayer
                playerView.resizeMode = when (aspectRatio) {
                    VideoAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    VideoAspectRatio.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    VideoAspectRatio.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    VideoAspectRatio.FOUR_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    VideoAspectRatio.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoomScale,
                    scaleY = zoomScale
                )
        )

        // Gesture detector layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.75f, 3.0f)
                    }
                }
                .pointerInput(isLocked) {
                    if (isLocked) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible }
                        )
                    } else {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                val width = size.width
                                when {
                                    offset.x < width * 0.35f -> {
                                        // Left side double tap: rewind
                                        val seekSeconds = settings.doubleTapSeekSeconds
                                        playerManager.seekRelative(-seekSeconds)
                                        doubleTapFeedback = "-${seekSeconds}s"
                                        scope.launch {
                                            delay(700)
                                            doubleTapFeedback = null
                                        }
                                    }
                                    offset.x > width * 0.65f -> {
                                        // Right side double tap: forward
                                        val seekSeconds = settings.doubleTapSeekSeconds
                                        playerManager.seekRelative(seekSeconds)
                                        doubleTapFeedback = "+${seekSeconds}s"
                                        scope.launch {
                                            delay(700)
                                            doubleTapFeedback = null
                                        }
                                    }
                                    else -> {
                                        // Center double tap: play/pause
                                        playerManager.togglePlayPause()
                                    }
                                }
                            }
                        )
                    }
                }
        )

        // Lock Banner overlay when locked
        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.TopStart
            ) {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = { isLocked = false },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock controls",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Gesture feedback HUDs (Brightness, Volume, Seek, Double-tap)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            doubleTapFeedback?.let { text ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Buffering spinner indicator
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Controls overlay (Top, Center, Bottom bars)
        AnimatedVisibility(
            visible = controlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Aspect ratio toggle
                    IconButton(onClick = { playerManager.cycleAspectRatio() }) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Cycle Aspect Ratio (${aspectRatio.label})",
                            tint = Color.White
                        )
                    }

                    // Audio track selector
                    IconButton(onClick = { showAudioSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Audio Tracks",
                            tint = if (audioTracks.size > 1) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    // Subtitle selector
                    IconButton(onClick = { showSubtitleSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.ClosedCaption,
                            contentDescription = "Subtitles",
                            tint = if (selectedSubtitleTrack != null) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    // Lock screen controls
                    IconButton(onClick = { isLocked = true }) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock controls",
                            tint = Color.White
                        )
                    }

                    // More options dropdown
                    Box {
                        IconButton(onClick = { moreMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More player options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Playback Speed (${playbackSpeed}x)") },
                                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    showSpeedDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Audio & Subtitle Delays") },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    showDelayDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Load External Subtitle") },
                                leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    subtitlePickerLauncher.launch(arrayOf("*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Picture-in-Picture") },
                                leadingIcon = { Icon(Icons.Default.PictureInPictureAlt, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    enterPiP()
                                }
                            )
                        }
                    }
                }

                // Center Play / Skip / Rewind Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Previous video if playlist available
                    val currentIndex = playlist.indexOfFirst { it.uri == video.uri }
                    val hasPrev = currentIndex > 0
                    val hasNext = currentIndex in 0 until playlist.size - 1

                    if (hasPrev) {
                        IconButton(
                            onClick = { onPlayNext(playlist[currentIndex - 1]) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Rewind 10s button
                    IconButton(
                        onClick = { playerManager.seekRelative(-settings.doubleTapSeekSeconds) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind ${settings.doubleTapSeekSeconds} seconds",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Large Play / Pause button
                    IconButton(
                        onClick = { playerManager.togglePlayPause() },
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Forward 10s button
                    IconButton(
                        onClick = { playerManager.seekRelative(settings.doubleTapSeekSeconds) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward ${settings.doubleTapSeekSeconds} seconds",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    if (hasNext) {
                        IconButton(
                            onClick = { onPlayNext(playlist[currentIndex + 1]) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Bottom Controls Bar (Current time, Seekbar, Duration, Speed, Screen Rotate, PiP)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Slider & Time row
                    var sliderPosition by remember { mutableLongStateOf(-1L) }
                    val displayPosition = if (sliderPosition >= 0) sliderPosition else currentPosition

                    Slider(
                        value = displayPosition.toFloat(),
                        onValueChange = { sliderPosition = it.toLong() },
                        onValueChangeFinished = {
                            if (sliderPosition >= 0) {
                                playerManager.seekTo(sliderPosition)
                                sliderPosition = -1L
                            }
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Current time / Total time
                        Text(
                            text = "${formatDuration(displayPosition)} / ${formatDuration(duration)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = Color.White
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Quick Speed chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { showSpeedDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            // PiP button
                            IconButton(
                                onClick = enterPiP,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Picture-in-Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Screen Orientation toggle
                            IconButton(
                                onClick = toggleOrientation,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Graceful error dialog when codec / container cannot be decoded
        playerError?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { /* Force user action */ },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = VibeRed,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = video.displayName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Offer open with external app
                            try {
                                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(video.uri, video.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(openIntent, "Open with another app"))
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open with another app")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onBack) {
                        Text("Close")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Audio Track Selection Dialog
        if (showAudioSheet) {
            AlertDialog(
                onDismissRequest = { showAudioSheet = false },
                title = {
                    Text(
                        text = "Audio Tracks",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (audioTracks.isEmpty()) {
                            Text(
                                text = "Default stereo audio track active",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD1D5DB)
                            )
                        } else {
                            audioTracks.forEach { track ->
                                val isSelected = track.isSelected || (selectedAudioTrack?.trackIndex == track.trackIndex && selectedAudioTrack?.groupIndex == track.groupIndex)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            playerManager.selectAudioTrack(track)
                                            showAudioSheet = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                        )
                                        if (track.language != null && track.language != "und") {
                                            Text(
                                                text = "Language: ${track.language.uppercase(Locale.ROOT)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFD1D5DB)
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioSheet = false }) {
                        Text("Close")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Subtitle Track Selection Dialog
        if (showSubtitleSheet) {
            AlertDialog(
                onDismissRequest = { showSubtitleSheet = false },
                title = {
                    Text(
                        text = "Subtitles",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Disable Subtitles option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    playerManager.selectSubtitleTrack(null)
                                    showSubtitleSheet = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Disable Subtitles",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selectedSubtitleTrack == null) MaterialTheme.colorScheme.primary else Color.White
                            )
                            if (selectedSubtitleTrack == null) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Available embedded subtitle tracks
                        subtitleTracks.forEach { track ->
                            val isSelected = (selectedSubtitleTrack?.trackIndex == track.trackIndex && selectedSubtitleTrack?.groupIndex == track.groupIndex)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        playerManager.selectSubtitleTrack(track)
                                        showSubtitleSheet = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                    if (track.language != null && track.language != "und") {
                                        Text(
                                            text = "Language: ${track.language.uppercase(Locale.ROOT)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFD1D5DB)
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button to load external subtitle
                        OutlinedButton(
                            onClick = {
                                showSubtitleSheet = false
                                subtitlePickerLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Load External Subtitle")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitleSheet = false }) {
                        Text("Close")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }


        // Playback Speed Dialog
        if (showSpeedDialog) {
            val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = {
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        speedOptions.forEach { speed ->
                            val isSelected = playbackSpeed == speed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        playerManager.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${speed}x",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Audio & Subtitle Delays Dialog
        if (showDelayDialog) {
            var tempAudioDelay by remember { mutableLongStateOf(audioDelayMs) }
            var tempSubDelay by remember { mutableLongStateOf(subtitleDelayMs) }

            AlertDialog(
                onDismissRequest = { showDelayDialog = false },
                title = {
                    Text(
                        text = "Audio & Subtitle Sync",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Audio Delay Controls
                        Column {
                            Text(
                                text = "Audio Delay: ${tempAudioDelay} ms",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { tempAudioDelay -= 100L },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("-100ms", color = Color.White)
                                }
                                TextButton(onClick = { tempAudioDelay = 0L }) {
                                    Text("Reset")
                                }
                                Button(
                                    onClick = { tempAudioDelay += 100L },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("+100ms", color = Color.White)
                                }
                            }
                        }

                        // Subtitle Delay Controls
                        Column {
                            Text(
                                text = "Subtitle Delay: ${tempSubDelay} ms",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { tempSubDelay -= 200L },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("-200ms", color = Color.White)
                                }
                                TextButton(onClick = { tempSubDelay = 0L }) {
                                    Text("Reset")
                                }
                                Button(
                                    onClick = { tempSubDelay += 200L },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("+200ms", color = Color.White)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            playerManager.setAudioDelay(tempAudioDelay)
                            playerManager.setSubtitleDelay(tempSubDelay)
                            scope.launch {
                                mediaRepository.updateDelays(video.uri.toString(), tempAudioDelay, tempSubDelay)
                            }
                            showDelayDialog = false
                        }
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDelayDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
