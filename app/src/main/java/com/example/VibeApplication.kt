package com.example

import android.app.Application
import com.example.data.local.VibePlayerDatabase
import com.example.data.repository.MediaRepository
import com.example.data.repository.SettingsRepository
import com.example.player.VibePlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class VibeApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { VibePlayerDatabase.getDatabase(this) }
    val mediaRepository by lazy { MediaRepository(this, database.videoRecordDao()) }
    val settingsRepository by lazy { SettingsRepository(database.settingDao(), applicationScope) }
    val playerManager by lazy { VibePlayerManager(this, applicationScope) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: VibeApplication
            private set
    }
}
