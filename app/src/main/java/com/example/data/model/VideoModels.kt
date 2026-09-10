package com.example.data.model

import android.net.Uri
import java.util.Locale

/**
 * Represents a video media item scanned from device storage or opened via external Intent.
 */
data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val displayName: String,
    val path: String = "",
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val dateModified: Long = 0L,
    val bucketName: String = "Internal",
    val resolution: String = "",
    val mimeType: String = "video/*",
    val isFavorite: Boolean = false,
    val lastPositionMs: Long = 0L,
    val lastPlayedTimestamp: Long = 0L
) {
    val durationFormatted: String
        get() = formatDuration(durationMs)

    val sizeFormatted: String
        get() = formatFileSize(sizeBytes)

    val extension: String
        get() = displayName.substringAfterLast('.', "").uppercase(Locale.ROOT)
}

/**
 * Represents a directory/folder containing videos.
 */
data class FolderItem(
    val name: String,
    val path: String = "",
    val videoCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val latestDateModified: Long = 0L
) {
    val totalSizeFormatted: String
        get() = formatFileSize(totalSizeBytes)
}

/**
 * Model representing an audio or subtitle track.
 */
data class TrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val trackType: Int, // C.TRACK_TYPE_AUDIO or C.TRACK_TYPE_TEXT
    val name: String,
    val language: String? = null,
    val isSelected: Boolean = false,
    val mimeType: String? = null,
    val channels: Int = 0,
    val sampleRate: Int = 0
)

/**
 * Subtitle styling and configuration.
 */
data class SubtitleConfig(
    val isEnabled: Boolean = true,
    val textSizeSp: Float = 18f,
    val textColorArgb: Long = 0xFFFFFFFF,
    val backgroundColorArgb: Long = 0x88000000,
    val delayMs: Long = 0L
)

/**
 * Aspect ratio options for the player.
 */
enum class VideoAspectRatio(val label: String) {
    FIT("Fit to Screen"),
    FILL("Crop to Fill"),
    SIXTEEN_NINE("16:9"),
    FOUR_THREE("4:3"),
    ORIGINAL("Original")
}

/**
 * Sort options for video listing.
 */
enum class SortOption(val label: String) {
    DATE_DESC("Date (Newest first)"),
    DATE_ASC("Date (Oldest first)"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    SIZE_DESC("Size (Largest first)"),
    DURATION_DESC("Duration (Longest first)")
}

/**
 * Theme and appearance mode.
 */
enum class AppThemeMode(val label: String) {
    LIGHT("Clean White")
}

/**
 * Resume behavior options.
 */
enum class ResumeOption(val label: String) {
    ASK("Ask Each Time"),
    ALWAYS("Always Resume"),
    NEVER("Always Start Over")
}

/**
 * Global persistent playback settings.
 */
data class PlaybackSettings(
    val defaultSpeed: Float = 1.0f,
    val resumeOption: ResumeOption = ResumeOption.ASK,
    val autoPlayNext: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val backgroundAudioEnabled: Boolean = false,
    val hardwareAcceleration: Boolean = true,
    val brightnessGesture: Boolean = true,
    val volumeGesture: Boolean = true,
    val seekGesture: Boolean = true,
    val autoHideControlsDelayMs: Long = 3500L,
    val defaultAspectRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val accentColorIndex: Int = 0,
    val subtitleTextSizeSp: Float = 18f,
    val subtitleTextColorArgb: Long = 0xFFFFFFFF,
    val subtitleBgArgb: Long = 0x88000000,
    val showShortClips: Boolean = true,
    val sortOption: SortOption = SortOption.DATE_DESC
)

/**
 * Formats duration in milliseconds into "HH:MM:SS" or "MM:SS".
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "00:00"
    val totalSeconds = durationMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Formats raw file size in bytes to readable string (e.g. 14.5 MB, 1.2 GB).
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> String.format(Locale.ROOT, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.ROOT, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.ROOT, "%.0f KB", kb)
        else -> "$bytes B"
    }
}
