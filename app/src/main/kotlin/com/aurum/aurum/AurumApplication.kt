package com.aurum.aurum

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.aurum.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AurumApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AurumApplication)
            modules(appModule)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(coil3.network.okhttp.OkHttpNetworkFetcherFactory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
