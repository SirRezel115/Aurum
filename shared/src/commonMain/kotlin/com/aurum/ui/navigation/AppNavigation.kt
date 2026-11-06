package com.aurum.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aurum.ui.theme.AurumTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import com.aurum.domain.SongSortBy
import com.aurum.player.PlayerState
import com.aurum.ui.common.TopographicContourPlaceholder
import com.aurum.ui.library.AlbumDetailScreen
import com.aurum.ui.library.AlbumsScreen
import com.aurum.ui.library.ArtistDetailScreen
import com.aurum.ui.library.ArtistsScreen
import com.aurum.ui.library.GenreDetailScreen
import com.aurum.ui.library.GenresScreen
import com.aurum.ui.library.LibraryScreen
import com.aurum.ui.library.LibraryViewModel
import com.aurum.ui.library.PlaylistsScreen
import com.aurum.ui.library.SortedLibraryScreen
import com.aurum.ui.player.MiniPlayerBar
import com.aurum.ui.player.PlayerScreen
import com.aurum.ui.player.PlayerViewModel
import com.aurum.ui.player.ProgressDotStyle
import com.aurum.ui.player.VisualizerColorScheme
import com.aurum.ui.player.VisualizerStyle
import com.aurum.ui.search.SearchScreen
import com.aurum.ui.settings.SettingsScreen
import com.aurum.ui.theme.AppFps
import kotlinx.coroutines.launch

import org.koin.compose.viewmodel.koinViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Library : Screen("library", "Library", Icons.Default.Home)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    libraryViewModel: LibraryViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel(),
    overridePlayerState: PlayerState? = null,
    navController: NavHostController = rememberNavController(),
    isDarkTheme: Boolean = true,
    onDarkThemeChange: (Boolean) -> Unit = {},
    selectedVisualizerStyle: VisualizerStyle = VisualizerStyle.RAINBOW_WAVES,
    onVisualizerStyleChange: (VisualizerStyle) -> Unit = {},
    selectedVisualizerColorScheme: VisualizerColorScheme = VisualizerColorScheme.RAINBOW,
    onVisualizerColorSchemeChange: (VisualizerColorScheme) -> Unit = {},
    selectedAppFps: AppFps = com.aurum.ui.theme.getInitialAppFps(),
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
    onDeezerArlTokenChange: (String) -> Unit = {}
) {
    val playerState = overridePlayerState ?: playerViewModel.playerState.value

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    AurumTheme(
        darkTheme = isDarkTheme
    ) {
        CompositionLocalProvider(com.aurum.ui.common.LocalVisualizerColorScheme provides selectedVisualizerColorScheme) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isDark = isDarkTheme
                val drawerContainerBg = if (isDark) Color(0xEB12131A) else Color(0xF5F8F7F2)
                val glassHeaderBg = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f)
                val glassHeaderBorder = if (isDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)) else BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f))
                val dividerColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)

                val drawerItemColors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = if (isDark) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer,
                    unselectedContainerColor = Color.Transparent,
                    selectedIconColor = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedIconColor = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerState = drawerState,
                            drawerContainerColor = drawerContainerBg,
                            windowInsets = WindowInsets(top = 28.dp)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = glassHeaderBg,
                                border = glassHeaderBorder
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    com.aurum.ui.common.RainbowText(
                                        "Aurum",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = dividerColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            NavigationDrawerItem(
                                icon = { Icon(Screen.Library.icon, contentDescription = Screen.Library.title) },
                                label = { Text(Screen.Library.title) },
                                selected = currentRoute == Screen.Library.route,
                                colors = drawerItemColors,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (currentRoute != Screen.Library.route) {
                                        navController.navigate("library") {
                                            popUpTo("library") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            val queue = playerState.queue.ifEmpty { playerState.currentSong?.let { listOf(it) } ?: emptyList() }
                            if (queue.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = dividerColor
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Playing Queue (${queue.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isDark) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    TextButton(onClick = { libraryViewModel.playSong(queue.first(), emptyList()) }) {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    itemsIndexed(queue, key = { idx, song -> "drawer_${song.id}_$idx" }) { index, song ->
                                        val isCurrent = song.id == playerState.currentSong?.id
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    scope.launch { drawerState.close() }
                                                    libraryViewModel.playSong(song, queue)
                                                },
                                            color = if (isCurrent) if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                ) {
                                                    if (!song.albumArtUri.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = song.albumArtUri,
                                                            contentDescription = song.title,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        TopographicContourPlaceholder(modifier = Modifier.fillMaxSize())
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = song.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = song.artist,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (isCurrent && playerState.isPlaying) {
                                                    Icon(
                                                        Icons.Default.Pause,
                                                        contentDescription = "Playing",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                selected = currentRoute == "settings",
                                colors = drawerItemColors,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (currentRoute != "settings") {
                                        navController.navigate("settings") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    },
                    content = {
                        Scaffold(
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                if (currentRoute != "player" && playerState.currentSong != null) {
                                    MiniPlayerBar(
                                        playerState = playerState,
                                        visualizerStyle = selectedVisualizerStyle,
                                        visualizerColorScheme = selectedVisualizerColorScheme,
                                        appFps = selectedAppFps,
                                        progressDotStyle = selectedProgressDotStyle,
                                        onClick = { navController.navigate("player") }
                                    )
                                }
                            }
                        ) { padding ->
                            val actualPadding = when (currentRoute) {
                                "video_player", "player" -> PaddingValues(0.dp)
                                else -> padding
                            }
                            NavHost(
                                navController = navController,
                                startDestination = "library",
                                modifier = Modifier.padding(actualPadding)
                            ) {
                                composable(
                                    route = "library",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) {
                                    LibraryScreen(
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist); navController.navigate("player") },
                                        onArtistClick = { name -> navController.navigate("artist/$name") },
                                        onAlbumClick = { title -> navController.navigate("album/$title") },
                                        onGenreClick = { name -> navController.navigate("genre/$name") },
                                        onOpenDrawer = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } },
                                        onOpenMetadataFixerForSong = { song ->
                                            libraryViewModel.setEditingTargetSong(song)
                                            navController.navigate("metadata_editor")
                                        },
                                        onOpenMetadataFixerForAlbum = { album ->
                                            libraryViewModel.setEditingTargetAlbum(album)
                                            navController.navigate("metadata_editor")
                                        },
                                        onOpenMetadataFixerForArtist = { artist ->
                                            libraryViewModel.setEditingTargetArtist(artist)
                                            navController.navigate("metadata_editor")
                                        },
                                        deezerArlToken = deezerArlToken,
                                        selectedFolder = selectedFolder
                                    )
                                }
                                composable(
                                    route = "library/artists",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) {
                                    ArtistsScreen(
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist); navController.navigate("player") },
                                        onArtistClick = { name -> navController.navigate("artist/$name") },
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "library/albums",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) {
                                    AlbumsScreen(
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist); navController.navigate("player") },
                                        onAlbumClick = { title -> navController.navigate("album/$title") },
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "library/genres",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) {
                                    GenresScreen(
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist); navController.navigate("player") },
                                        onGenreClick = { name -> navController.navigate("genre/$name") },
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "library/playlists",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) {
                                    PlaylistsScreen(viewModel = libraryViewModel, onBackClick = { navController.popBackStack() })
                                }
                                composable(
                                    route = "library/title",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) {
                                    SortedLibraryScreen(
                                        title = "Songs (A-Z)",
                                        sortBy = SongSortBy.TITLE,
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist); navController.navigate("player") },
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "library/duration",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) {
                                    SortedLibraryScreen(
                                        title = "Duration",
                                        sortBy = SongSortBy.DURATION,
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist); navController.navigate("player") },
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "album/{albumTitle}",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) { backStackEntry ->
                                    val albumTitle = backStackEntry.arguments?.read { getString("albumTitle") } ?: ""
                                    AlbumDetailScreen(
                                        albumTitle = albumTitle,
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist); navController.navigate("player") },
                                        onBackClick = { navController.popBackStack() },
                                        onOpenMetadataFixerForSong = { song ->
                                            libraryViewModel.setEditingTargetSong(song)
                                            navController.navigate("metadata_editor")
                                        }
                                    )
                                }
                                composable(
                                    route = "artist/{artistName}",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) { backStackEntry ->
                                    val artistName = backStackEntry.arguments?.read { getString("artistName") } ?: ""
                                    ArtistDetailScreen(
                                        artistName = artistName,
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist); navController.navigate("player") },
                                        onAlbumClick = { title -> navController.navigate("album/$title") },
                                        onBackClick = { navController.popBackStack() },
                                        onOpenMetadataFixerForSong = { song ->
                                            libraryViewModel.setEditingTargetSong(song)
                                            navController.navigate("metadata_editor")
                                        },
                                        onOpenMetadataFixerForAlbum = { album ->
                                            libraryViewModel.setEditingTargetAlbum(album)
                                            navController.navigate("metadata_editor")
                                        }
                                    )
                                }
                                composable(
                                    route = "genre/{genreName}",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) { backStackEntry ->
                                    val genreName = backStackEntry.arguments?.read { getString("genreName") } ?: ""
                                    GenreDetailScreen(
                                        genreName = genreName,
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist); navController.navigate("player") },
                                        onBackClick = { navController.popBackStack() },
                                        onOpenMetadataFixerForSong = { song ->
                                            libraryViewModel.setEditingTargetSong(song)
                                            navController.navigate("metadata_editor")
                                        }
                                    )
                                }
                                composable(
                                    route = "search",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) {
                                    SearchScreen(
                                        viewModel = libraryViewModel,
                                        onSongClick = { song, playlist -> libraryViewModel.playSong(song, playlist); navController.navigate("player") },
                                        selectedFolder = selectedFolder,
                                        deezerArlToken = deezerArlToken,
                                        onOpenDrawer = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }
                                    )
                                }
                                composable(
                                    route = "player",
                                    enterTransition = { slideInVertically { height -> height } + fadeIn() },
                                    exitTransition = { slideOutVertically { height -> height } + fadeOut() },
                                    popEnterTransition = { fadeIn() },
                                    popExitTransition = { slideOutVertically { height -> height } + fadeOut() },
                                ) {
                                    PlayerScreen(
                                        playerState = playerState,
                                        viewModel = playerViewModel,
                                        visualizerStyle = selectedVisualizerStyle,
                                        visualizerColorScheme = selectedVisualizerColorScheme,
                                        appFps = selectedAppFps,
                                        progressDotStyle = selectedProgressDotStyle,
                                        onOpenDrawer = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } },
                                        onOpenMetadataFixer = { song ->
                                            libraryViewModel.setEditingTargetSong(song)
                                            navController.navigate("metadata_editor")
                                        }
                                    )
                                }
                                composable(
                                    route = "settings",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) {
                                    SettingsScreen(
                                        isDarkTheme = isDarkTheme,
                                        onDarkThemeChange = onDarkThemeChange,
                                        selectedVisualizerStyle = selectedVisualizerStyle,
                                        onVisualizerStyleChange = onVisualizerStyleChange,
                                        selectedVisualizerColorScheme = selectedVisualizerColorScheme,
                                        onVisualizerColorSchemeChange = onVisualizerColorSchemeChange,
                                        selectedAppFps = selectedAppFps,
                                        onAppFpsChange = onAppFpsChange,
                                        selectedProgressDotStyle = selectedProgressDotStyle,
                                        onProgressDotStyleChange = onProgressDotStyleChange,
                                        selectedFolder = selectedFolder,
                                        onChooseFolder = onChooseFolder,
                                        stopMusicOnAppClose = stopMusicOnAppClose,
                                        onStopMusicOnAppCloseChange = onStopMusicOnAppCloseChange,
                                        deezerEnabled = deezerEnabled,
                                        onDeezerEnabledChange = onDeezerEnabledChange,
                                        deezerArlToken = deezerArlToken,
                                        onDeezerArlTokenChange = onDeezerArlTokenChange,
                                        onOpenEqualizer = { playerViewModel.openSystemEqualizer() },
                                        onOpenMetadataEditor = { navController.navigate("metadata_editor") },
                                        onRepairAllArtwork = { libraryViewModel.repairAllMissingArtwork { _, _ -> } },
                                        onRemoveDuplicates = { libraryViewModel.removeDuplicateSongs { } },
                                        onRescan = { libraryViewModel.rescan() },
                                        onOpenDrawer = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } },
                                        onBackClick = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "metadata_editor",
                                    enterTransition = { slideInHorizontally { width -> width } + fadeIn() },
                                    exitTransition = { slideOutHorizontally { width -> -width } + fadeOut() },
                                    popEnterTransition = { slideInHorizontally { width -> -width } + fadeIn() },
                                    popExitTransition = { slideOutHorizontally { width -> width } + fadeOut() },
                                ) {
                                    com.aurum.ui.settings.MetadataEditorScreen(
                                        viewModel = libraryViewModel,
                                        onBackClick = {
                                            libraryViewModel.clearEditingTargets()
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
