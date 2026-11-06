package com.aurum.ui.desktop

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.aurum.ui.common.TopographicContourPlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.URLDecoder
import javax.imageio.ImageIO

private val desktopBitmapCache = mutableMapOf<String, ImageBitmap>()

/**
 * Native Desktop Image Composable using Java ImageIO & Compose toComposeImageBitmap.
 */
@Composable
fun AurumAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val modelKey = model?.toString()?.trim() ?: ""
    var imageBitmap by remember(modelKey) { mutableStateOf<ImageBitmap?>(desktopBitmapCache[modelKey]) }

    LaunchedEffect(modelKey) {
        if (modelKey.isBlank()) {
            imageBitmap = null
            return@LaunchedEffect
        }

        if (desktopBitmapCache.containsKey(modelKey)) {
            imageBitmap = desktopBitmapCache[modelKey]
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val bitmap = loadDesktopBitmap(modelKey)
            if (bitmap != null) {
                if (desktopBitmapCache.size > 100) {
                    desktopBitmapCache.clear()
                }
                desktopBitmapCache[modelKey] = bitmap
                withContext(Dispatchers.Main) {
                    imageBitmap = bitmap
                }
            } else {
                withContext(Dispatchers.Main) {
                    imageBitmap = null
                }
            }
        }
    }

    val bitmap = imageBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        TopographicContourPlaceholder(modifier = modifier)
    }
}

private fun loadDesktopBitmap(source: String): ImageBitmap? {
    try {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            val url = URI(source).toURL()
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            val img = conn.inputStream.use { ImageIO.read(it) }
            if (img != null) {
                println("AurumAsyncImage: Successfully loaded network bitmap for $source")
                return img.toComposeImageBitmap()
            }
        } else {
            val clean = source
                .removePrefix("file:///")
                .removePrefix("file://")
                .removePrefix("file:")
            val decoded = try { URLDecoder.decode(clean, "UTF-8") } catch (_: Exception) { clean }

            val file = File(decoded)
            val targetFile = if (file.exists() && file.isFile) file else File(clean)

            if (targetFile.exists() && targetFile.isFile && targetFile.length() > 0) {
                val img = ImageIO.read(targetFile)
                if (img != null) {
                    println("AurumAsyncImage: Successfully loaded local bitmap from ${targetFile.absolutePath}")
                    return img.toComposeImageBitmap()
                } else {
                    println("AurumAsyncImage: ImageIO returned null for file ${targetFile.absolutePath}")
                }
            } else {
                println("AurumAsyncImage: File does not exist at $decoded or $clean")
            }
        }
    } catch (e: Exception) {
        println("AurumAsyncImage: Error loading bitmap for $source: ${e.message}")
    }
    return null
}
