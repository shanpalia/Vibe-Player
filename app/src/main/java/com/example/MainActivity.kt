package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import com.example.ui.VibeApp

class MainActivity : ComponentActivity() {
    private var openedVideoUri by mutableStateOf<Uri?>(null)

    private val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // HomeScreen will rescan automatically when it sees the granted permission.
    }

    private fun hasVideoPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return androidx.core.content.ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun requestVideoPermissionIfNeeded() {
        if (!hasVideoPermission()) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            mediaPermissionLauncher.launch(permission)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        val bars = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        bars.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        bars.isAppearanceLightStatusBars = true
        bars.isAppearanceLightNavigationBars = true

        openedVideoUri = extractVideoUri(intent)

        setContent {
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1200)
                showSplash = false
                requestVideoPermissionIfNeeded()
            }

            if (showSplash) {
                VibeSplashScreen()
            } else {
                VibeApp(
                    externalVideoUri = openedVideoUri,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = extractVideoUri(intent)
        if (uri != null) {
            openedVideoUri = uri
        }
    }

    private fun extractVideoUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> intent.data
        }
    }
}

// Retained for screenshot test compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Vibe Player $name", modifier = modifier)
}


@Composable
private fun VibeSplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.vibe_launcher_icon),
            contentDescription = "Vibe Player",
            modifier = Modifier.size(190.dp),
            contentScale = ContentScale.Fit
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(14.dp))
        Text(
            text = "Vibe Player",
            color = Color(0xFF17203A),
            fontSize = 28.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black
        )
        Text(
            text = "Play Everything. Feel the Vibe.",
            color = Color(0xFF657085),
            fontSize = 13.sp
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(30.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(width = 150.dp, height = 4.dp)
                .background(Color(0xFFE4E7EF))
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))
        Text(
            text = "Developer by ShanPalia",
            color = Color(0xFF4B5563),
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}
