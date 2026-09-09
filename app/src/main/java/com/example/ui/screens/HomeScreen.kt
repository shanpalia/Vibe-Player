package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VibeApplication
import com.example.data.model.FolderItem
import com.example.data.model.ResumeOption
import com.example.data.model.SortOption
import com.example.data.model.VideoItem
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.FolderCard
import com.example.ui.components.ResumePlaybackDialog
import com.example.ui.components.VibeLogoHeader
import com.example.ui.components.VideoCard
import com.example.ui.theme.VibeTextPrimary
import com.example.ui.theme.VibeTextSecondary
import com.example.ui.theme.VibeTextTertiary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onPlayVideo: (video: VideoItem, startPositionMs: Long) -> Unit,
    onOpenFolder: (folderName: String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = VibeApplication.instance
    val mediaRepository = app.mediaRepository
    val settingsRepository = app.settingsRepository

    val settings by settingsRepository.settings.collectAsState()
    val allVideos by mediaRepository.videosFlow.collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Dialog states
    var videoForDetails by remember { mutableStateOf<VideoItem?>(null) }
    var videoForResume by remember { mutableStateOf<VideoItem?>(null) }
    var videoForDelete by remember { mutableStateOf<VideoItem?>(null) }

    // Permission state & launcher
    var hasMediaPermission by remember { mutableStateOf(false) }
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMediaPermission = granted
        if (granted) {
            scope.launch { mediaRepository.scanDeviceVideos() }
        }
    }

    // SAF Open Document picker for direct file opening
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                val resolved = mediaRepository.resolveExternalUri(uri)
                onPlayVideo(resolved, 0L)
            }
        }
    }

    LaunchedEffect(Unit) {
        // Check permission initial state
        hasMediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        mediaRepository.scanDeviceVideos()
    }

    // Filter videos by query and short clip filter
    val filteredVideos = remember(allVideos, searchQuery, settings.showShortClips) {
        allVideos.filter { video ->
            val matchesQuery = searchQuery.isBlank() ||
                    video.title.contains(searchQuery, ignoreCase = true) ||
                    video.bucketName.contains(searchQuery, ignoreCase = true)
            val matchesDuration = settings.showShortClips || video.durationMs >= 30_000L
            matchesQuery && matchesDuration
        }
    }

    val sortedVideos = remember(filteredVideos, settings.sortOption) {
        mediaRepository.sortVideos(filteredVideos, settings.sortOption)
    }

    val folders = remember(sortedVideos) {
        mediaRepository.groupFolders(sortedVideos)
    }

    val recentlyPlayed = remember(sortedVideos) {
        sortedVideos.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp }
    }

    val favorites = remember(sortedVideos) {
        sortedVideos.filter { it.isFavorite }
    }

    // Handle video tap considering resume options
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
            // Logo Header
            VibeLogoHeader(
                onSearchClick = { isSearchActive = !isSearchActive },
                onSettingsClick = onOpenSettings
            )

            // Search Bar (Animated Visibility)
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_field"),
                        placeholder = { Text("Search by video name or folder...", color = VibeTextTertiary) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = VibeTextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = VibeTextPrimary,
                            unfocusedTextColor = VibeTextPrimary,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            // Categories Tabs
            val tabs = listOf(
                "All Videos" to sortedVideos.size,
                "Folders" to folders.size,
                "Recently Played" to recentlyPlayed.size,
                "Favorites" to favorites.size
            )

            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = VibeTextPrimary,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, (title, count) ->
                    val isSelected = (selectedTabIndex == index)
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else VibeTextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else VibeTextTertiary
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Controls Sub-header: Sort & Rescan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort Dropdown Button
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable { sortMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = settings.sortOption.label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = VibeTextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (option == settings.sortOption) MaterialTheme.colorScheme.primary else VibeTextPrimary,
                                        fontWeight = if (option == settings.sortOption) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    sortMenuExpanded = false
                                    settingsRepository.setSortOption(option)
                                }
                            )
                        }
                    }
                }

                // Rescan Button
                IconButton(
                    onClick = { scope.launch { mediaRepository.scanDeviceVideos() } },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan library",
                        tint = VibeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Permission Request Banner if not granted
            if (!hasMediaPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Storage Permission Needed",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = VibeTextPrimary
                            )
                            Text(
                                text = "Grant permission to scan and play all video formats on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { permissionLauncher.launch(permissionToRequest) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }

            // Main Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTabIndex) {
                0 -> {
                    // All Videos Tab
                    if (sortedVideos.isEmpty()) {
                        EmptyState(
                            message = if (searchQuery.isNotBlank()) "No videos match \"$searchQuery\"" else "No videos found on device",
                            onOpenFile = { openDocumentLauncher.launch(arrayOf("video/*")) },
                            onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                            hasPermission = hasMediaPermission
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sortedVideos, key = { it.uri.toString() }) { video ->
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
                }

                1 -> {
                    // Folders Tab
                    if (folders.isEmpty()) {
                        EmptyState(
                            message = "No video folders discovered yet",
                            onOpenFile = { openDocumentLauncher.launch(arrayOf("video/*")) },
                            onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                            hasPermission = hasMediaPermission
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(folders, key = { it.name }) { folder ->
                                FolderCard(
                                    folder = folder,
                                    onClick = { onOpenFolder(folder.name) }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Recently Played Tab
                    if (recentlyPlayed.isEmpty()) {
                        EmptyState(
                            message = "No recently played videos",
                            onOpenFile = { openDocumentLauncher.launch(arrayOf("video/*")) },
                            onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                            hasPermission = hasMediaPermission
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(recentlyPlayed, key = { it.uri.toString() }) { video ->
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
                }

                3 -> {
                    // Favorites Tab
                    if (favorites.isEmpty()) {
                        EmptyState(
                            message = "No favorite videos yet. Tap the heart on any video to add it!",
                            onOpenFile = { openDocumentLauncher.launch(arrayOf("video/*")) },
                            onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                            hasPermission = hasMediaPermission
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(favorites, key = { it.uri.toString() }) { video ->
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
                }
                }
            }

            // Bottom navigation: always stays inside the app content and above Android system navigation.
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                containerColor = Color.White,
                tonalElevation = 2.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Folders") },
                    label = { Text("Folders") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Recent") },
                    label = { Text("Recent") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                    label = { Text("Favorites") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenSettings,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
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

@Composable
private fun EmptyState(
    message: String,
    onOpenFile: () -> Unit,
    onRequestPermission: () -> Unit,
    hasPermission: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.VideoFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = VibeTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onOpenFile,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Storage")
                }

                if (!hasPermission) {
                    Text(
                        "Use Grant Permission above to scan your videos.",
                        color = VibeTextTertiary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
