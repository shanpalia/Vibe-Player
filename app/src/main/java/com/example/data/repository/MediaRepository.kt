package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.VideoRecordDao
import com.example.data.local.VideoRecordEntity
import com.example.data.model.FolderItem
import com.example.data.model.SortOption
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(
    private val context: Context,
    private val videoRecordDao: VideoRecordDao
) {
    private val _scannedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val scannedVideos = _scannedVideos.asStateFlow()

    val recordsFlow: Flow<List<VideoRecordEntity>> = videoRecordDao.getAllRecords()

    /**
     * Combines scanned videos with database records to provide an updated reactive list.
     */
    val videosFlow: Flow<List<VideoItem>> = combine(_scannedVideos, recordsFlow) { scanned, records ->
        val recordMap = records.associateBy { it.uri }
        scanned.map { video ->
            val record = recordMap[video.uri.toString()]
            if (record != null) {
                video.copy(
                    isFavorite = record.isFavorite,
                    lastPositionMs = record.lastPositionMs,
                    lastPlayedTimestamp = record.lastPlayedTimestamp
                )
            } else {
                video
            }
        }
    }

    /**
     * Scans MediaStore for video files.
     */
    suspend fun scanDeviceVideos() = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.MIME_TYPE
        )

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val bucketColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val resolutionColumn = cursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION)
                val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val name = cursor.getString(nameColumn) ?: "Video_$id"
                    val path = if (dataColumn != -1) cursor.getString(dataColumn) ?: "" else ""
                    val duration = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                    val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                    val dateModified = if (dateModifiedColumn != -1) cursor.getLong(dateModifiedColumn) * 1000L else 0L
                    val bucketName = if (bucketColumn != -1) cursor.getString(bucketColumn) ?: "Videos" else "Videos"
                    val resolution = if (resolutionColumn != -1) cursor.getString(resolutionColumn) ?: "" else ""
                    val mimeType = if (mimeTypeColumn != -1) cursor.getString(mimeTypeColumn) ?: "video/*" else "video/*"

                    val item = VideoItem(
                        id = id,
                        uri = contentUri,
                        title = name.substringBeforeLast('.'),
                        displayName = name,
                        path = path,
                        durationMs = duration,
                        sizeBytes = size,
                        dateModified = dateModified,
                        bucketName = bucketName,
                        resolution = resolution,
                        mimeType = mimeType
                    )
                    videoList.add(item)
                }
            }
        } catch (_: Exception) {
            // Permission not yet granted or MediaStore query error
        }

        _scannedVideos.value = videoList
    }

    /**
     * Resolves a video from external Uri (e.g. opened via Intent.ACTION_VIEW or SAF).
     */
    suspend fun resolveExternalUri(uri: Uri): VideoItem = withContext(Dispatchers.IO) {
        // Check if already in scanned list
        val existing = _scannedVideos.value.find { it.uri == uri || it.uri.toString() == uri.toString() }
        if (existing != null) return@withContext existing

        var displayName = uri.lastPathSegment ?: "External Video"
        var size: Long = 0L
        var mimeType: String = "video/*"

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { displayName = it }
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                    if (mimeIndex != -1) {
                        cursor.getString(mimeIndex)?.let { mimeType = it }
                    }
                }
            }
        } catch (_: Exception) {}

        val record = videoRecordDao.getRecordByUriSync(uri.toString())
        val item = VideoItem(
            id = uri.hashCode().toLong(),
            uri = uri,
            title = displayName.substringBeforeLast('.'),
            displayName = displayName,
            path = uri.path ?: "",
            durationMs = record?.durationMs ?: 0L,
            sizeBytes = size,
            dateModified = System.currentTimeMillis(),
            bucketName = "External",
            resolution = "",
            mimeType = mimeType,
            isFavorite = record?.isFavorite ?: false,
            lastPositionMs = record?.lastPositionMs ?: 0L,
            lastPlayedTimestamp = record?.lastPlayedTimestamp ?: 0L
        )

        // Add to scanned list so user can navigate back
        val currentList = _scannedVideos.value.toMutableList()
        if (currentList.none { it.uri == uri }) {
            currentList.add(0, item)
            _scannedVideos.value = currentList
        }

        item
    }

    suspend fun toggleFavorite(video: VideoItem) = withContext(Dispatchers.IO) {
        val newFav = !video.isFavorite
        val existing = videoRecordDao.getRecordByUriSync(video.uri.toString())
        if (existing != null) {
            videoRecordDao.setFavorite(video.uri.toString(), newFav)
        } else {
            videoRecordDao.upsertRecord(
                VideoRecordEntity(
                    uri = video.uri.toString(),
                    title = video.title,
                    durationMs = video.durationMs,
                    isFavorite = newFav
                )
            )
        }
    }

    suspend fun savePlaybackProgress(uri: String, positionMs: Long, durationMs: Long, title: String) = withContext(Dispatchers.IO) {
        val existing = videoRecordDao.getRecordByUriSync(uri)
        if (existing != null) {
            videoRecordDao.upsertRecord(
                existing.copy(
                    lastPositionMs = positionMs,
                    lastPlayedTimestamp = System.currentTimeMillis(),
                    durationMs = if (durationMs > 0) durationMs else existing.durationMs
                )
            )
        } else {
            videoRecordDao.upsertRecord(
                VideoRecordEntity(
                    uri = uri,
                    title = title,
                    durationMs = durationMs,
                    lastPositionMs = positionMs,
                    lastPlayedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateDelays(uri: String, audioDelayMs: Long, subtitleDelayMs: Long) = withContext(Dispatchers.IO) {
        videoRecordDao.updateDelays(uri, audioDelayMs, subtitleDelayMs)
    }

    suspend fun getRecordSync(uri: String): VideoRecordEntity? = withContext(Dispatchers.IO) {
        videoRecordDao.getRecordByUriSync(uri)
    }

    suspend fun deleteVideo(video: VideoItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleted = context.contentResolver.delete(video.uri, null, null) > 0
            if (deleted) {
                videoRecordDao.deleteRecord(video.uri.toString())
                _scannedVideos.value = _scannedVideos.value.filter { it.id != video.id }
            }
            deleted
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if there is a matching subtitle file (.srt, .vtt, .ass) in the same directory.
     */
    suspend fun findMatchingSubtitle(video: VideoItem): Uri? = withContext(Dispatchers.IO) {
        if (video.path.isBlank()) return@withContext null
        try {
            val videoFile = File(video.path)
            val parent = videoFile.parentFile ?: return@withContext null
            val baseName = videoFile.nameWithoutExtension

            val extensions = listOf(".srt", ".vtt", ".ass", ".ssa")
            for (ext in extensions) {
                val subFile = File(parent, "$baseName$ext")
                if (subFile.exists() && subFile.canRead()) {
                    return@withContext Uri.fromFile(subFile)
                }
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Groups videos into Folder items.
     */
    fun groupFolders(videos: List<VideoItem>): List<FolderItem> {
        return videos.groupBy { it.bucketName }
            .map { (name, list) ->
                FolderItem(
                    name = name,
                    path = list.firstOrNull()?.path?.let { File(it).parent ?: "" } ?: "",
                    videoCount = list.size,
                    totalSizeBytes = list.sumOf { it.sizeBytes },
                    latestDateModified = list.maxOfOrNull { it.dateModified } ?: 0L
                )
            }
            .sortedByDescending { it.latestDateModified }
    }

    /**
     * Sorts video items by the selected criteria.
     */
    fun sortVideos(videos: List<VideoItem>, sortOption: SortOption): List<VideoItem> {
        return when (sortOption) {
            SortOption.DATE_DESC -> videos.sortedByDescending { it.dateModified }
            SortOption.DATE_ASC -> videos.sortedBy { it.dateModified }
            SortOption.NAME_ASC -> videos.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> videos.sortedByDescending { it.title.lowercase() }
            SortOption.SIZE_DESC -> videos.sortedByDescending { it.sizeBytes }
            SortOption.DURATION_DESC -> videos.sortedByDescending { it.durationMs }
        }
    }
}
