package com.aurum.di

import org.koin.core.module.Module

/**
 * Common DI module — wires the expect platform implementations.
 * Declared here; resolved per-platform via actual in androidMain / iosMain.
 */
expect val appModule: Module