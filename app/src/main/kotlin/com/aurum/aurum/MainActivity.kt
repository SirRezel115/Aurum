package com.aurum.aurum

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aurum.ui.navigation.AppNavigation
import com.aurum.ui.theme.AurumTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions result handled
    }

    private var selectedFolderUriState = mutableStateOf<String?>(null)

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val folder = uri.lastPathSegment ?: uri.toString()
            selectedFolderUriState.value = folder
            getSharedPreferences("aurum_prefs", MODE_PRIVATE).edit().putString("selected_folder_uri", folder).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.aurum.data.repository.OnlineArtworkFetcher.applicationContext = applicationContext
        com.aurum.data.repository.DeezerMusicProvider.applicationContext = applicationContext
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val params = window.attributes
            params.preferredRefreshRate = 120f
            window.attributes = params
        }

        hideSystemBars()

        checkAndRequestPermissions()

        val prefs = getSharedPreferences("aurum_prefs", MODE_PRIVATE)
        selectedFolderUriState.value = prefs.getString("selected_folder_uri", null)

        setContent {
            var isDarkTheme by rememberSaveable {
                mutableStateOf(prefs.getBoolean("is_dark_theme", true))
            }
            var stopMusicOnAppClose by rememberSaveable {
                mutableStateOf(prefs.getBoolean("stop_on_app_close", false))
            }
            var selectedVisualizerStyle by rememberSaveable {
                mutableStateOf(
                    prefs.getString("visualizer_style", null)?.let { name ->
                        runCatching { com.aurum.ui.player.VisualizerStyle.valueOf(name) }.getOrNull()
                    } ?: com.aurum.ui.player.VisualizerStyle.RAINBOW_WAVES
                )
            }
            var selectedVisualizerColorScheme by rememberSaveable {
                mutableStateOf(
                    prefs.getString("visualizer_color_scheme", null)?.let { name ->
                        runCatching { com.aurum.ui.player.VisualizerColorScheme.valueOf(name) }.getOrNull()
                    } ?: com.aurum.ui.player.VisualizerColorScheme.RAINBOW
                )
            }
            var selectedAppFps by rememberSaveable {
                mutableStateOf(
                    prefs.getString("app_fps", null)?.let { name ->
                        runCatching { com.aurum.ui.theme.AppFps.valueOf(name) }.getOrNull()
                    } ?: com.aurum.ui.theme.getInitialAppFps()
                )
            }
            var selectedProgressDotStyle by rememberSaveable {
                mutableStateOf(
                    prefs.getString("progress_dot_style", null)?.let { name ->
                        runCatching { com.aurum.ui.player.ProgressDotStyle.valueOf(name) }.getOrNull()
                    } ?: com.aurum.ui.player.ProgressDotStyle.ORB
                )
            }
            var deezerEnabled by rememberSaveable {
                mutableStateOf(prefs.getBoolean("deezer_enabled", true))
            }
            var deezerArlToken by rememberSaveable {
                mutableStateOf(prefs.getString("deezer_arl_token", "") ?: "")
            }
            var selectedFolder by selectedFolderUriState

            LaunchedEffect(selectedAppFps) {
                runCatching {
                    val params = window.attributes
                    params.preferredRefreshRate = selectedAppFps.targetFps.toFloat()
                    window.attributes = params
                }
            }

            AurumTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation(
                        isDarkTheme = isDarkTheme,
                        onDarkThemeChange = { newValue ->
                            isDarkTheme = newValue
                            prefs.edit().putBoolean("is_dark_theme", newValue).apply()
                        },
                        selectedVisualizerStyle = selectedVisualizerStyle,
                        onVisualizerStyleChange = { newValue ->
                            selectedVisualizerStyle = newValue
                            prefs.edit().putString("visualizer_style", newValue.name).apply()
                        },
                        selectedVisualizerColorScheme = selectedVisualizerColorScheme,
                        onVisualizerColorSchemeChange = { newValue ->
                            selectedVisualizerColorScheme = newValue
                            prefs.edit().putString("visualizer_color_scheme", newValue.name).apply()
                        },
                        selectedAppFps = selectedAppFps,
                        onAppFpsChange = { newValue ->
                            selectedAppFps = newValue
                            prefs.edit().putString("app_fps", newValue.name).apply()
                        },
                        selectedProgressDotStyle = selectedProgressDotStyle,
                        onProgressDotStyleChange = { newValue ->
                            selectedProgressDotStyle = newValue
                            prefs.edit().putString("progress_dot_style", newValue.name).apply()
                        },
                        selectedFolder = selectedFolder,
                        onChooseFolder = { folderPickerLauncher.launch(null) },
                        stopMusicOnAppClose = stopMusicOnAppClose,
                        onStopMusicOnAppCloseChange = { newValue ->
                            stopMusicOnAppClose = newValue
                            prefs.edit().putBoolean("stop_on_app_close", newValue).apply()
                        },
                        deezerEnabled = deezerEnabled,
                        onDeezerEnabledChange = { newValue ->
                            deezerEnabled = newValue
                            prefs.edit().putBoolean("deezer_enabled", newValue).apply()
                        },
                        deezerArlToken = deezerArlToken,
                        onDeezerArlTokenChange = { newValue ->
                            deezerArlToken = newValue
                            prefs.edit().putString("deezer_arl_token", newValue).apply()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
