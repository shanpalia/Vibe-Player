package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import com.example.data.model.TrackInfo
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(UnstableApi::class)
class VibePlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var exoPlayer: ExoPlayer? = null
        private set

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _aspectRatio = MutableStateFlow(VideoAspectRatio.FIT)
    val aspectRatio: StateFlow<VideoAspectRatio> = _aspectRatio.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackInfo>> = _subtitleTracks.asStateFlow()

    private val _selectedAudioTrack = MutableStateFlow<TrackInfo?>(null)
    val selectedAudioTrack: StateFlow<TrackInfo?> = _selectedAudioTrack.asStateFlow()

    private val _selectedSubtitleTrack = MutableStateFlow<TrackInfo?>(null)
    val selectedSubtitleTrack: StateFlow<TrackInfo?> = _selectedSubtitleTrack.asStateFlow()

    private val _audioDelayMs = MutableStateFlow(0L)
    val audioDelayMs: StateFlow<Long> = _audioDelayMs.asStateFlow()

    private val _subtitleDelayMs = MutableStateFlow(0L)
    val subtitleDelayMs: StateFlow<Long> = _subtitleDelayMs.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private var progressPollingJob: Job? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (exoPlayer != null) return

        // Configure extractors for comprehensive format handling:
        // MKV, WebM, MP4, M4V, MOV, FLV, AVI, TS, MTS, 3GP, OGG/OGV
        val extractorsFactory = DefaultExtractorsFactory().apply {
            setConstantBitrateSeekingEnabled(true)
            setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)
            setTsExtractorMode(TsExtractor.MODE_SINGLE_PMT)
        }

        // Configure renderers with software fallback support if hardware decoder fails
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressPolling() else stopProgressPolling()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracks(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                val message = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                        "Hardware or software decoder could not decode this video track. The codec may not be supported by this device."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                        "This video container format is unsupported or corrupted."
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        "Video file could not be found or has been moved."
                    PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                        "Storage permission is required to read this video file."
                    else ->
                        "Playback error: ${error.localizedMessage ?: "Unknown media error"} (Code ${error.errorCode})"
                }
                _playerError.value = message
            }
        })

        exoPlayer = player
    }

    /**
     * Prepares and starts playback of a video item, optionally starting from a saved position.
     */
    fun playVideo(
        video: VideoItem,
        startPositionMs: Long = 0L,
        externalSubtitleUri: Uri? = null,
        initialSpeed: Float = 1.0f
    ) {
        val player = exoPlayer ?: return
        _currentVideo.value = video
        _playerError.value = null

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(video.uri)
            .setMediaId(video.uri.toString())

        // Attach external subtitle if provided
        if (externalSubtitleUri != null) {
            val mime = when {
                externalSubtitleUri.toString().endsWith(".vtt", true) -> MimeTypes.TEXT_VTT
                externalSubtitleUri.toString().endsWith(".ass", true) || externalSubtitleUri.toString().endsWith(".ssa", true) -> MimeTypes.TEXT_SSA
                else -> MimeTypes.APPLICATION_SUBRIP
            }
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(externalSubtitleUri)
                .setMimeType(mime)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setLabel("External Subtitle")
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
        }

        mediaItemBuilder.setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(video.title)
                .setArtist("Vibe Player • Shan Palia")
                .setAlbumTitle("Vibe Player")
                .build()
        )

        val mediaItem = mediaItemBuilder.build()
        player.setMediaItem(mediaItem)
        player.setPlaybackParameters(PlaybackParameters(initialSpeed))
        _playbackSpeed.value = initialSpeed

        if (startPositionMs > 0) {
            player.seekTo(startPositionMs)
            _currentPositionMs.value = startPositionMs
        } else {
            _currentPositionMs.value = 0L
        }

        player.prepare()
        player.play()
    }

    fun stopPlayback() {
        exoPlayer?.let {
            it.pause()
            it.stop()
            it.clearMediaItems()
        }
        _isPlaying.value = false
        _currentVideo.value = null
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.let {
            val safePos = positionMs.coerceIn(0L, it.duration.coerceAtLeast(0L))
            it.seekTo(safePos)
            _currentPositionMs.value = safePos
        }
    }

    fun seekRelative(offsetSeconds: Int) {
        exoPlayer?.let {
            val current = it.currentPosition
            val target = (current + offsetSeconds * 1000L).coerceIn(0L, it.duration.coerceAtLeast(0L))
            it.seekTo(target)
            _currentPositionMs.value = target
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.let {
            it.setPlaybackParameters(PlaybackParameters(speed))
            _playbackSpeed.value = speed
        }
    }

    fun setAspectRatio(ratio: VideoAspectRatio) {
        _aspectRatio.value = ratio
    }

    fun cycleAspectRatio() {
        val values = VideoAspectRatio.entries
        val nextIndex = (values.indexOf(_aspectRatio.value) + 1) % values.size
        _aspectRatio.value = values[nextIndex]
    }

    fun setAudioDelay(delayMs: Long) {
        _audioDelayMs.value = delayMs
    }

    fun setSubtitleDelay(delayMs: Long) {
        _subtitleDelayMs.value = delayMs
    }

    /**
     * Updates audio and subtitle track lists from ExoPlayer Tracks.
     */
    private fun updateTracks(tracks: Tracks) {
        val player = exoPlayer ?: return
        val audioList = mutableListOf<TrackInfo>()
        val subtitleList = mutableListOf<TrackInfo>()

        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            val mediaTrackGroup = group.mediaTrackGroup
            val trackType = group.type

            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                val lang = format.language ?: "und"
                val label = format.label ?: format.id ?: getTrackLabel(format, trackType, trackIndex)

                val trackInfo = TrackInfo(
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                    trackType = trackType,
                    name = label,
                    language = lang,
                    isSelected = isSelected,
                    mimeType = format.sampleMimeType,
                    channels = format.channelCount,
                    sampleRate = format.sampleRate
                )

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    audioList.add(trackInfo)
                    if (isSelected) _selectedAudioTrack.value = trackInfo
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subtitleList.add(trackInfo)
                    if (isSelected) _selectedSubtitleTrack.value = trackInfo
                }
            }
        }

        _audioTracks.value = audioList
        _subtitleTracks.value = subtitleList
    }

    private fun getTrackLabel(format: Format, trackType: Int, index: Int): String {
        val lang = format.language?.uppercase(Locale.ROOT)
        return when (trackType) {
            C.TRACK_TYPE_AUDIO -> {
                val channels = when (format.channelCount) {
                    1 -> "Mono"
                    2 -> "Stereo"
                    6 -> "5.1 Surround"
                    8 -> "7.1 Surround"
                    else -> if (format.channelCount > 0) "${format.channelCount}ch" else ""
                }
                listOfNotNull(lang, format.sampleMimeType?.substringAfterLast('/'), channels)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                    .ifBlank { "Audio Track ${index + 1}" }
            }
            C.TRACK_TYPE_TEXT -> {
                listOfNotNull(lang, format.sampleMimeType?.substringAfterLast('/'))
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                    .ifBlank { "Subtitle ${index + 1}" }
            }
            else -> "Track ${index + 1}"
        }
    }

    /**
     * Selects an audio track or disables/enables defaults.
     */
    fun selectAudioTrack(track: TrackInfo?) {
        val player = exoPlayer ?: return
        if (track == null) return

        val tracks = player.currentTracks
        if (track.groupIndex < tracks.groups.size) {
            val group = tracks.groups[track.groupIndex]
            val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(track.trackIndex))
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setOverrideForType(override)
                .build()
            _selectedAudioTrack.value = track
        }
    }

    /**
     * Selects a subtitle track or disables subtitles.
     */
    fun selectSubtitleTrack(track: TrackInfo?) {
        val player = exoPlayer ?: return
        if (track == null) {
            // Disable subtitles
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _selectedSubtitleTrack.value = null
        } else {
            val tracks = player.currentTracks
            if (track.groupIndex < tracks.groups.size) {
                val group = tracks.groups[track.groupIndex]
                val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(track.trackIndex))
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(override)
                    .build()
                _selectedSubtitleTrack.value = track
            }
        }
    }

    /**
     * Attaches an external subtitle file to the currently playing video.
     */
    fun loadExternalSubtitle(uri: Uri) {
        val current = _currentVideo.value ?: return
        val currentPos = exoPlayer?.currentPosition ?: 0L
        playVideo(current, startPositionMs = currentPos, externalSubtitleUri = uri, initialSpeed = _playbackSpeed.value)
    }

    private fun startProgressPolling() {
        stopProgressPolling()
        progressPollingJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                exoPlayer?.let {
                    _currentPositionMs.value = it.currentPosition.coerceAtLeast(0L)
                    _bufferedPositionMs.value = it.bufferedPosition.coerceAtLeast(0L)
                    _durationMs.value = it.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    fun release() {
        stopProgressPolling()
        exoPlayer?.release()
        exoPlayer = null
    }
}
