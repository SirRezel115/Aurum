package com.aurum.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.aurum.database.AurumDatabase
import com.aurum.data.repository.MusicRepository
import com.aurum.data.repository.MusicRepositoryImpl
import com.aurum.player.AndroidMusicPlayer
import com.aurum.player.MusicPlayer
import com.aurum.scanner.AndroidMusicScanner
import com.aurum.scanner.MusicScanner
import com.aurum.ui.library.LibraryViewModel
import com.aurum.ui.player.PlayerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Android platform DI module.
 * Provides ExoPlayer-based player, MediaStore-based scanner, SQLDelight DB, repository, and ViewModels.
 */
actual val appModule: Module = module {
    single<SqlDriver> {
        AndroidSqliteDriver(AurumDatabase.Schema, get(), "aurum.db")
    }
    single { AurumDatabase(get()) }
    single<MusicPlayer> { AndroidMusicPlayer(get()) }
    single<MusicScanner> { AndroidMusicScanner(get()) }
    single<MusicRepository> { MusicRepositoryImpl(get(), get()) }

    viewModel { LibraryViewModel(get(), get(), getOrNull()) }
    viewModel { PlayerViewModel(get()) }
}
