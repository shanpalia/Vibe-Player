package com.example.data.repository

import com.example.data.local.SettingDao
import com.example.data.local.SettingEntity
import com.example.data.model.AppThemeMode
import com.example.data.model.PlaybackSettings
import com.example.data.model.ResumeOption
import com.example.data.model.SortOption
import com.example.data.model.VideoAspectRatio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsRepository(
    private val settingDao: SettingDao,
    private val scope: CoroutineScope
) {
    private val _settings = MutableStateFlow(PlaybackSettings())
    val settings: StateFlow<PlaybackSettings> = _settings.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            settingDao.getAllSettings().collect { entities ->
                val map = entities.associate { it.key to it.value }
                _settings.value = PlaybackSettings(
                    defaultSpeed = map["default_speed"]?.toFloatOrNull() ?: 1.0f,
                    resumeOption = map["resume_option"]?.let { safeValueOf<ResumeOption>(it) } ?: ResumeOption.ASK,
                    autoPlayNext = map["auto_play_next"]?.toBooleanStrictOrNull() ?: true,
                    doubleTapSeekSeconds = map["double_tap_seek_seconds"]?.toIntOrNull() ?: 10,
                    backgroundAudioEnabled = map["background_audio"]?.toBooleanStrictOrNull() ?: true,
                    hardwareAcceleration = map["hardware_accel"]?.toBooleanStrictOrNull() ?: true,
                    brightnessGesture = map["brightness_gesture"]?.toBooleanStrictOrNull() ?: true,
                    volumeGesture = map["volume_gesture"]?.toBooleanStrictOrNull() ?: true,
                    seekGesture = map["seek_gesture"]?.toBooleanStrictOrNull() ?: true,
                    autoHideControlsDelayMs = map["auto_hide_delay"]?.toLongOrNull() ?: 3500L,
                    defaultAspectRatio = map["default_aspect_ratio"]?.let { safeValueOf<VideoAspectRatio>(it) } ?: VideoAspectRatio.FIT,
                    themeMode = map["theme_mode"]?.let { safeValueOf<AppThemeMode>(it) } ?: AppThemeMode.LIGHT,
                    accentColorIndex = map["accent_color_index"]?.toIntOrNull() ?: 0,
                    subtitleTextSizeSp = map["subtitle_text_size"]?.toFloatOrNull() ?: 18f,
                    subtitleTextColorArgb = map["subtitle_text_color"]?.toLongOrNull() ?: 0xFFFFFFFF,
                    subtitleBgArgb = map["subtitle_bg_color"]?.toLongOrNull() ?: 0x88000000,
                    showShortClips = map["show_short_clips"]?.toBooleanStrictOrNull() ?: true,
                    sortOption = map["sort_option"]?.let { safeValueOf<SortOption>(it) } ?: SortOption.DATE_DESC
                )
            }
        }
    }

    fun updateSetting(key: String, value: String) {
        scope.launch(Dispatchers.IO) {
            settingDao.setSetting(SettingEntity(key = key, value = value))
        }
    }

    fun setDefaultSpeed(speed: Float) = updateSetting("default_speed", speed.toString())
    fun setResumeOption(option: ResumeOption) = updateSetting("resume_option", option.name)
    fun setAutoPlayNext(enabled: Boolean) = updateSetting("auto_play_next", enabled.toString())
    fun setDoubleTapSeekSeconds(seconds: Int) = updateSetting("double_tap_seek_seconds", seconds.toString())
    fun setBackgroundAudio(enabled: Boolean) = updateSetting("background_audio", enabled.toString())
    fun setHardwareAcceleration(enabled: Boolean) = updateSetting("hardware_accel", enabled.toString())
    fun setBrightnessGesture(enabled: Boolean) = updateSetting("brightness_gesture", enabled.toString())
    fun setVolumeGesture(enabled: Boolean) = updateSetting("volume_gesture", enabled.toString())
    fun setSeekGesture(enabled: Boolean) = updateSetting("seek_gesture", enabled.toString())
    fun setAutoHideDelay(delayMs: Long) = updateSetting("auto_hide_delay", delayMs.toString())
    fun setDefaultAspectRatio(ratio: VideoAspectRatio) = updateSetting("default_aspect_ratio", ratio.name)
    fun setThemeMode(mode: AppThemeMode) = updateSetting("theme_mode", mode.name)
    fun setAccentColorIndex(index: Int) = updateSetting("accent_color_index", index.toString())
    fun setSubtitleTextSize(sp: Float) = updateSetting("subtitle_text_size", sp.toString())
    fun setSubtitleTextColor(argb: Long) = updateSetting("subtitle_text_color", argb.toString())
    fun setSubtitleBgColor(argb: Long) = updateSetting("subtitle_bg_color", argb.toString())
    fun setShowShortClips(show: Boolean) = updateSetting("show_short_clips", show.toString())
    fun setSortOption(sort: SortOption) = updateSetting("sort_option", sort.name)

    private inline fun <reified T : Enum<T>> safeValueOf(name: String): T? {
        return try {
            java.lang.Enum.valueOf(T::class.java, name)
        } catch (_: Exception) {
            null
        }
    }
}
