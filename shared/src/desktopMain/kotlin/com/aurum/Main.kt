package com.aurum

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aurum.di.appModule
import com.aurum.scanner.DesktopMusicScanner
import com.aurum.ui.desktop.DesktopAppLayout
import com.aurum.ui.library.LibraryViewModel
import com.aurum.ui.player.PlayerViewModel
import com.aurum.ui.player.ProgressDotStyle
import com.aurum.ui.player.VisualizerColorScheme
import com.aurum.ui.player.VisualizerStyle
import com.aurum.ui.theme.AppFps
import com.aurum.ui.theme.AurumTheme
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import java.awt.Dimension
import java.io.File
import java.util.Properties
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

fun main() {
    System.setProperty("sun.awt.keepWorkingSetOnMinimize", "true")

    startKoin {
        modules(appModule)
    }

    application {
        val windowState = rememberWindowState(width = 1100.dp, height = 720.dp)

        val savedProps = remember { loadSavedProperties() }

        var isDarkTheme by remember { mutableStateOf(savedProps.getProperty("isDarkTheme", "true").toBoolean()) }
        var visualizerStyle by remember {
            val name = savedProps.getProperty("visualizerStyle", VisualizerStyle.RAINBOW_WAVES.name)
            mutableStateOf(VisualizerStyle.entries.find { it.name == name } ?: VisualizerStyle.RAINBOW_WAVES)
        }
        var visualizerColorScheme by remember {
            val name = savedProps.getProperty("visualizerColorScheme", VisualizerColorScheme.RAINBOW.name)
            mutableStateOf(VisualizerColorScheme.entries.find { it.name == name } ?: VisualizerColorScheme.RAINBOW)
        }
        var appFps by remember {
            val name = savedProps.getProperty("appFps", AppFps.FPS_60.name)
            mutableStateOf(AppFps.entries.find { it.name == name } ?: AppFps.FPS_60)
        }
        var progressDotStyle by remember {
            val name = savedProps.getProperty("progressDotStyle", ProgressDotStyle.ORB.name)
            mutableStateOf(ProgressDotStyle.entries.find { it.name == name } ?: ProgressDotStyle.ORB)
        }
        var stopMusicOnAppClose by remember { mutableStateOf(savedProps.getProperty("stopMusicOnAppClose", "false").toBoolean()) }
        var selectedFolder by remember { mutableStateOf<String?>(savedProps.getProperty("selectedFolder")) }

        val scanner: DesktopMusicScanner = koinInject()
        val libraryViewModel: LibraryViewModel = koinInject()
        val playerViewModel: PlayerViewModel = koinInject()

        var isFullscreen by remember { mutableStateOf(false) }
        var onToggleFullscreenRef by remember { mutableStateOf<() -> Unit>({}) }

        Window(
            onCloseRequest = {
                if (stopMusicOnAppClose) {
                    try {
                        runBlocking {
                            playerViewModel.stop()
                        }
                    } catch (_: Exception) {}
                }
                exitApplication()
            },
            title = "Aurum Music Player",
            state = windowState,
            onKeyEvent = { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when {
                        keyEvent.key == Key.F11 || (keyEvent.key == Key.Enter && keyEvent.isAltPressed) -> {
                            onToggleFullscreenRef()
                            true
                        }
                        keyEvent.key == Key.Escape && isFullscreen -> {
                            onToggleFullscreenRef()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
        ) {
            window.minimumSize = Dimension(850, 580)

            val toggleFullscreen = remember {
                {
                    val nextFullscreen = !isFullscreen
                    isFullscreen = nextFullscreen
                    SwingUtilities.invokeLater {
                        window.dispose()
                        window.isUndecorated = nextFullscreen
                        if (nextFullscreen) {
                            windowState.placement = WindowPlacement.Maximized
                        } else {
                            windowState.placement = WindowPlacement.Floating
                        }
                        window.isVisible = true
                    }
                }
            }
            SideEffect {
                onToggleFullscreenRef = toggleFullscreen
            }

            coil3.compose.setSingletonImageLoaderFactory { context ->
                coil3.ImageLoader.Builder(context)
                    .components {
                        add(coil3.map.Mapper<String, File> { data, _ ->
                            if (!data.startsWith("http://") && !data.startsWith("https://")) {
                                try {
                                    val clean = data
                                        .removePrefix("file:///")
                                        .removePrefix("file://")
                                        .removePrefix("file:")
                                    val decoded = java.net.URLDecoder.decode(clean, "UTF-8")
                                    val f = File(decoded)
                                    if (f.exists() && f.isFile) f else {
                                        val f2 = File(clean)
                                        if (f2.exists() && f2.isFile) f2 else null
                                    }
                                } catch (_: Exception) {
                                    null
                                }
                            } else null
                        })
                    }
                    .build()
            }

            LaunchedEffect(selectedFolder) {
                if (selectedFolder != null) {
                    scanner.customFolder = selectedFolder
                    libraryViewModel.rescan()
                }
            }

            AurumTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DesktopAppLayout(
                        libraryViewModel = libraryViewModel,
                        playerViewModel = playerViewModel,
                        isDarkTheme = isDarkTheme,
                        onDarkThemeChange = { newDark ->
                            isDarkTheme = newDark
                            saveSingleProperty("isDarkTheme", newDark.toString())
                        },
                        selectedVisualizerStyle = visualizerStyle,
                        onVisualizerStyleChange = { newStyle ->
                            visualizerStyle = newStyle
                            saveSingleProperty("visualizerStyle", newStyle.name)
                        },
                        selectedVisualizerColorScheme = visualizerColorScheme,
                        onVisualizerColorSchemeChange = { newScheme ->
                            visualizerColorScheme = newScheme
                            saveSingleProperty("visualizerColorScheme", newScheme.name)
                        },
                        selectedAppFps = appFps,
                        onAppFpsChange = { newFps ->
                            appFps = newFps
                            saveSingleProperty("appFps", newFps.name)
                        },
                        selectedProgressDotStyle = progressDotStyle,
                        onProgressDotStyleChange = { newDot ->
                            progressDotStyle = newDot
                            saveSingleProperty("progressDotStyle", newDot.name)
                        },
                        stopMusicOnAppClose = stopMusicOnAppClose,
                        onStopMusicOnAppCloseChange = { newStop ->
                            stopMusicOnAppClose = newStop
                            saveSingleProperty("stopMusicOnAppClose", newStop.toString())
                        },
                        selectedFolder = selectedFolder,
                        onChooseFolder = {
                            SwingUtilities.invokeLater {
                                val chooser = JFileChooser().apply {
                                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                    dialogTitle = "Select Music Directory"
                                    isAcceptAllFileFilterUsed = false
                                }
                                val result = chooser.showOpenDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    val newFolder = chooser.selectedFile?.absolutePath
                                    if (newFolder != null) {
                                        selectedFolder = newFolder
                                        saveSingleProperty("selectedFolder", newFolder)
                                        scanner.customFolder = newFolder
                                        libraryViewModel.rescan()
                                    }
                                }
                            }
                        },
                        isFullscreen = isFullscreen,
                        onToggleFullscreen = toggleFullscreen
                    )
                }
            }
        }
    }
}

private fun loadSavedProperties(): Properties {
    val props = Properties()
    val configFile = File(File(System.getProperty("user.home"), ".aurum"), "config.properties")
    if (configFile.exists()) {
        try {
            configFile.inputStream().use { props.load(it) }
        } catch (_: Exception) {}
    }
    return props
}

private fun saveSingleProperty(key: String, value: String) {
    val configDir = File(System.getProperty("user.home"), ".aurum")
    if (!configDir.exists()) configDir.mkdirs()
    val configFile = File(configDir, "config.properties")
    val props = Properties()
    if (configFile.exists()) {
        try {
            configFile.inputStream().use { props.load(it) }
        } catch (_: Exception) {}
    }
    props.setProperty(key, value)
    try {
        configFile.outputStream().use { props.store(it, "Aurum Config") }
    } catch (_: Exception) {}
}

