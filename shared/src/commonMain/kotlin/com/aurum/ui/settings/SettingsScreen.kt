package com.aurum.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aurum.ui.player.ProgressDotStyle
import com.aurum.ui.player.VisualizerColorScheme
import com.aurum.ui.player.VisualizerStyle
import com.aurum.ui.theme.AppFps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean = true,
    onDarkThemeChange: (Boolean) -> Unit = {},
    selectedVisualizerStyle: VisualizerStyle = VisualizerStyle.RAINBOW_WAVES,
    onVisualizerStyleChange: (VisualizerStyle) -> Unit = {},
    selectedVisualizerColorScheme: VisualizerColorScheme = VisualizerColorScheme.RAINBOW,
    onVisualizerColorSchemeChange: (VisualizerColorScheme) -> Unit = {},
    selectedAppFps: AppFps = AppFps.FPS_60,
    onAppFpsChange: (AppFps) -> Unit = {},
    selectedProgressDotStyle: ProgressDotStyle = ProgressDotStyle.ORB,
    onProgressDotStyleChange: (ProgressDotStyle) -> Unit = {},
    selectedFolder: String? = null,
    onChooseFolder: () -> Unit = {},
    stopMusicOnAppClose: Boolean = false,
    onStopMusicOnAppCloseChange: (Boolean) -> Unit = {},
    deezerEnabled: Boolean = true,
    onDeezerEnabledChange: (Boolean) -> Unit = {},
    deezerArlToken: String = "",
    onDeezerArlTokenChange: (String) -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenMetadataEditor: () -> Unit = {},
    onRepairAllArtwork: () -> Unit = {},
    onRemoveDuplicates: () -> Unit = {},
    onRescan: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val glassBg = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val glassBorder = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val bgColors = if (isDark) {
        listOf(Color(0xFF0D0E15), Color(0xFF141622), Color(0xFF090A0E))
    } else {
        listOf(Color(0xFFFFFFFF), Color(0xFFFAF8F2), Color(0xFFF3EFE6))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { com.aurum.ui.common.RainbowText("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        com.aurum.ui.common.RainbowIcon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = bgColors))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = glassBg,
                    border = glassBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme", style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                            Text(if (isDarkTheme) "Black & Silver aesthetic" else "Light Silver & White aesthetic", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onDarkThemeChange
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = glassBg,
                    border = glassBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Stop Music on App Close", style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                            Text("Stop music playback when the app is force-closed or swiped away", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                        }
                        Switch(
                            checked = stopMusicOnAppClose,
                            onCheckedChange = onStopMusicOnAppCloseChange
                        )
                    }
                }

                // Visualizer Style Dropdown Card
                SettingDropdownCard(
                    title = "Visualizer Style",
                    subtitle = "Choose your preferred progress bar visualizer style",
                    options = VisualizerStyle.entries,
                    selectedOption = selectedVisualizerStyle,
                    getDisplayName = { it.displayName },
                    onOptionSelected = onVisualizerStyleChange
                )

                // Visualizer Color Scheme Dropdown Card
                SettingDropdownCard(
                    title = "Visualizer Color Scheme",
                    subtitle = "Choose the color palette for the visualizer",
                    options = VisualizerColorScheme.entries,
                    selectedOption = selectedVisualizerColorScheme,
                    getDisplayName = { it.displayName },
                    onOptionSelected = onVisualizerColorSchemeChange
                )

                // App Frame Rate (FPS) Dropdown Card
                SettingDropdownCard(
                    title = "App Frame Rate (FPS)",
                    subtitle = "Choose target frame rate for the app and visualizer (30, 60, 90, 120 FPS)",
                    options = AppFps.entries,
                    selectedOption = selectedAppFps,
                    getDisplayName = { it.displayName },
                    onOptionSelected = onAppFpsChange
                )

                // Progress Dot Style Dropdown Card
                SettingDropdownCard(
                    title = "Progress Bar Indicator / Thumb Dot",
                    subtitle = "Choose the playhead dot style on the progress bar",
                    options = ProgressDotStyle.entries,
                    selectedOption = selectedProgressDotStyle,
                    getDisplayName = { it.displayName },
                    onOptionSelected = onProgressDotStyleChange
                )

                // Audio Crossfade Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = glassBg,
                    border = glassBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Audio Crossfade & Gapless Transitions", style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Seamless song blending without silence gaps", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                    }
                }

                // Sleep Timer Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = glassBg,
                    border = glassBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Sleep Timer with Audio Fade-Out", style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Gently fades out volume before stopping playback", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                    }
                }

                // System Equalizer Launch Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = glassBg,
                    border = glassBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Audio Equalizer", style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Adjust audio frequency bands or launch device system equalizer", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onOpenEqualizer,
                            modifier = Modifier.fillMaxWidth(),
                            border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Launch System Equalizer", color = if (isDark) Color.White else MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Deezer Music Provider Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = glassBg,
                    border = glassBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🎶 Deezer Music Provider", style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Search and download track audio files directly from Deezer", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                            }
                            Switch(
                                checked = deezerEnabled,
                                onCheckedChange = onDeezerEnabledChange
                            )
                        }
                        if (deezerEnabled) {
                            var arlStatus by remember { mutableStateOf<com.aurum.data.repository.DeezerArlStatus?>(null) }
                            LaunchedEffect(deezerArlToken) {
                                if (deezerArlToken.isNotBlank()) {
                                    arlStatus = com.aurum.data.repository.DeezerMusicProvider.validateArlToken(deezerArlToken)
                                } else {
                                    arlStatus = com.aurum.data.repository.DeezerArlStatus(
                                        isValid = false,
                                        message = "No ARL token entered (Using Standard Quality)"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = deezerArlToken,
                                onValueChange = onDeezerArlTokenChange,
                                label = { Text("Deezer ARL Token (Optional)") },
                                placeholder = { Text("Paste ARL token for high quality audio...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = textSecondary.copy(alpha = 0.5f)
                                )
                            )
                            val status = arlStatus
                            if (status != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = if (status.isValid) Color(0xFF1E3A29) else (if (deezerArlToken.isBlank()) Color.DarkGray else Color(0xFF3E1E1E))
                                    ) {
                                        Text(
                                            text = if (status.isValid) "🟢 ${status.message}" else (if (deezerArlToken.isBlank()) "⚪ ${status.message}" else "🔴 ${status.message}"),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (status.isValid) Color(0xFF81C784) else (if (deezerArlToken.isBlank()) textSecondary else Color(0xFFE57373)),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = textSecondary.copy(alpha = 0.08f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("💡 How to get a fresh Deezer ARL cookie:", style = MaterialTheme.typography.labelLarge, color = textPrimary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("1. Open deezer.com in desktop browser (Chrome/Edge/Firefox) and log in.", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                                    Text("2. Press F12 -> Go to 'Application' (or 'Storage') tab -> 'Cookies' -> 'deezer.com'.", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                                    Text("3. Copy value of 'arl' cookie (192-char string) and paste into field above.", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                                }
                            }
                        }
                    }
                }

                // Metadata Editor Option Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = glassBg,
                    border = glassBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("✏️ Metadata Editor & Artwork Search", style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Search Apple Music API for 1000x1000 HD artwork and apply metadata directly to tracks, albums, or artists", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onOpenMetadataEditor,
                            modifier = Modifier.fillMaxWidth(),
                            border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Open Metadata Search & Editor", color = if (isDark) Color.White else MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Bulk Repair All Artwork Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = glassBg,
                    border = glassBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("🎨 Repair All Missing Artwork", style = MaterialTheme.typography.bodyLarge, color = textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Refetch 1000x1000 HD artwork from Apple Music API for all albums missing artwork", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onRepairAllArtwork,
                            modifier = Modifier.fillMaxWidth(),
                            border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Repair All Missing Library Artwork", color = if (isDark) Color.White else MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = glassBg,
                    border = glassBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Music Library Folder",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            selectedFolder ?: "Default (Scans All Device Audio)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onChooseFolder,
                            modifier = Modifier.fillMaxWidth(),
                            border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Choose Custom Music Folder", color = if (isDark) Color.White else MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onRemoveDuplicates,
                    modifier = Modifier.fillMaxWidth(),
                    border = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("🧹 Clean & Remove Duplicate Songs", color = if (isDark) Color.White else MaterialTheme.colorScheme.primary)
                }

                Button(
                    onClick = onRescan,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary)
                ) {
                    Text("Rescan Device Music Library", color = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingDropdownCard(
    title: String,
    subtitle: String,
    options: List<T>,
    selectedOption: T,
    getDisplayName: (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val glassBg = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val glassBorder = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    val textPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = glassBg,
        border = glassBorder,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = textPrimary
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = getDisplayName(selectedOption),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(getDisplayName(option)) },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun SettingsScreenPreview() {
    com.aurum.ui.theme.AurumTheme {
        SettingsScreen(
            isDarkTheme = true,
            onDarkThemeChange = {},
            selectedVisualizerStyle = VisualizerStyle.RAINBOW_WAVES,
            onVisualizerStyleChange = {},
            selectedVisualizerColorScheme = VisualizerColorScheme.RAINBOW,
            onVisualizerColorSchemeChange = {},
            selectedAppFps = AppFps.FPS_60,
            onAppFpsChange = {},
            selectedProgressDotStyle = ProgressDotStyle.ORB,
            onProgressDotStyleChange = {},
            selectedFolder = "/Music/Aurum",
            onChooseFolder = {},
            onRemoveDuplicates = {},
            onRescan = {}
        )
    }
}
