package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.VibeApplication
import com.example.data.model.AppThemeMode
import com.example.data.model.ResumeOption
import com.example.data.model.VideoAspectRatio
import com.example.ui.theme.AccentColors
import com.example.ui.theme.AccentNames
import com.example.ui.theme.VibeTextPrimary
import com.example.ui.theme.VibeTextSecondary
import com.example.ui.theme.VibeTextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = VibeApplication.instance
    val settingsRepository = app.settingsRepository
    val settings by settingsRepository.settings.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = VibeTextPrimary
                    )
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Playback Settings Section
                item {
                    SettingsSectionTitle("Playback")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            // Resume Option
                            EnumSettingRow(
                                title = "Resume Playback",
                                subtitle = settings.resumeOption.label,
                                icon = Icons.Default.PlayCircleOutline,
                                options = ResumeOption.entries.map { it to it.label },
                                selected = settings.resumeOption,
                                onSelect = { settingsRepository.setResumeOption(it) }
                            )

                            SettingsDivider()

                            // Auto-play next
                            SwitchSettingRow(
                                title = "Auto-play Next Video",
                                subtitle = "Automatically play the next item in folder",
                                icon = Icons.Default.FastForward,
                                checked = settings.autoPlayNext,
                                onCheckedChange = { settingsRepository.setAutoPlayNext(it) }
                            )

                            SettingsDivider()

                            // Double-tap seek seconds
                            EnumSettingRow(
                                title = "Double-tap Seek Duration",
                                subtitle = "${settings.doubleTapSeekSeconds} seconds",
                                icon = Icons.Default.TouchApp,
                                options = listOf(5 to "5 seconds", 10 to "10 seconds", 15 to "15 seconds", 30 to "30 seconds"),
                                selected = settings.doubleTapSeekSeconds,
                                onSelect = { settingsRepository.setDoubleTapSeekSeconds(it) }
                            )

                            SettingsDivider()

                            // Background audio
                            SwitchSettingRow(
                                title = "Background Audio Playback",
                                subtitle = "Continue playing audio when app is in background or screen is off",
                                icon = Icons.Default.Audiotrack,
                                checked = settings.backgroundAudioEnabled,
                                onCheckedChange = { settingsRepository.setBackgroundAudio(it) }
                            )
                        }
                    }
                }

                // Subtitles & Aspect Ratio Section
                item {
                    SettingsSectionTitle("Display & Subtitles")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            // Default aspect ratio
                            EnumSettingRow(
                                title = "Default Aspect Ratio",
                                subtitle = settings.defaultAspectRatio.label,
                                icon = Icons.Default.AspectRatio,
                                options = VideoAspectRatio.entries.map { it to it.label },
                                selected = settings.defaultAspectRatio,
                                onSelect = { settingsRepository.setDefaultAspectRatio(it) }
                            )

                            SettingsDivider()

                            // Subtitle text size
                            EnumSettingRow(
                                title = "Subtitle Font Size",
                                subtitle = "${settings.subtitleTextSizeSp.toInt()} sp",
                                icon = Icons.Default.ClosedCaption,
                                options = listOf(14f to "Small (14sp)", 18f to "Normal (18sp)", 22f to "Large (22sp)", 26f to "Extra Large (26sp)"),
                                selected = settings.subtitleTextSizeSp,
                                onSelect = { settingsRepository.setSubtitleTextSize(it) }
                            )
                        }
                    }
                }

                // Appearance Section
                item {
                    SettingsSectionTitle("Theme & Styling")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            // Theme Mode
                            EnumSettingRow(
                                title = "Theme Mode",
                                subtitle = settings.themeMode.label,
                                icon = Icons.Default.Palette,
                                options = AppThemeMode.entries.map { it to it.label },
                                selected = settings.themeMode,
                                onSelect = { settingsRepository.setThemeMode(it) }
                            )

                            SettingsDivider()

                            // Accent Colors
                            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ColorLens,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Accent Color: ${AccentNames.getOrElse(settings.accentColorIndex) { "Neon Violet" }}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = VibeTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.padding(start = 34.dp)
                                ) {
                                    AccentColors.forEachIndexed { index, (color, _) ->
                                        val isSelected = (settings.accentColorIndex == index)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .clickable { settingsRepository.setAccentColorIndex(index) }
                                                .border(
                                                    width = if (isSelected) 3.dp else 0.dp,
                                                    color = if (isSelected) Color.White else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Media Library Section
                item {
                    SettingsSectionTitle("Media Library")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            SwitchSettingRow(
                                title = "Show Short Clips",
                                subtitle = "Display videos shorter than 30 seconds",
                                icon = Icons.Default.VideoLibrary,
                                checked = settings.showShortClips,
                                onCheckedChange = { settingsRepository.setShowShortClips(it) }
                            )
                        }
                    }
                }

                // About Brand Card
                item {
                    SettingsSectionTitle("About")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    )
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = R.drawable.vibe_launcher_icon,
                                    contentDescription = "Vibe Player Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Vibe Player",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = VibeTextPrimary
                            )

                            Text(
                                text = "“Play Everything. Feel the Vibe.”",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeTextSecondary
                            )

                            Text(
                                text = "Developed by Shan Palia",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = VibeTextPrimary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Engineered with Android Media3 ExoPlayer with universal container parsers (MP4, MKV, AVI, MOV, WebM, FLV, 3GP, TS, MTS, OGV) and hardware/software decoding fallback.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = VibeTextTertiary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    )
}

@Composable
private fun SwitchSettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = VibeTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = VibeTextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun <T> EnumSettingRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = VibeTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (option, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = if (option == selected) MaterialTheme.colorScheme.primary else VibeTextPrimary,
                            fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}
