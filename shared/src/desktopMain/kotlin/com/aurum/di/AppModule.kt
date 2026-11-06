package com.aurum.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.aurum.database.AurumDatabase
import com.aurum.data.repository.MusicRepository
import com.aurum.data.repository.MusicRepositoryImpl
import com.aurum.player.DesktopMusicPlayer
import com.aurum.player.MusicPlayer
import com.aurum.scanner.DesktopMusicScanner
import com.aurum.scanner.MusicScanner
import com.aurum.ui.library.LibraryViewModel
import com.aurum.ui.player.PlayerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

actual val appModule: Module = module {
    single<SqlDriver> {
        val dbDir = java.io.File(System.getProperty("user.home"), ".aurum")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }
        val dbFile = java.io.File(dbDir, "aurum.db")
        val isNew = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNew) {
            AurumDatabase.Schema.create(driver)
        }
        driver
    }
    single { AurumDatabase(get()) }
    single { DesktopMusicPlayer() }
    single<MusicPlayer> { get<DesktopMusicPlayer>() }
    single { DesktopMusicScanner() }
    single<MusicScanner> { get<DesktopMusicScanner>() }
    single<MusicRepository> { MusicRepositoryImpl(get(), get()) }

    viewModel { LibraryViewModel(get(), get(), getOrNull()) }
    viewModel { PlayerViewModel(get()) }
}
