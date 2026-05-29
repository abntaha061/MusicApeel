package com.example

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.memory.MemoryCache
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure Coil with powerful caching and custom MP3 Album Art fetcher
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(Mp3AlbumArtFetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20) // 20% of RAM for caching images
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("album_art_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .crossfade(true)
            .build()

        Coil.setImageLoader(imageLoader)
    }
}

// Custom Fetcher to extract embedded pictures from MP3 files on background thread
class Mp3AlbumArtFetcher(
    private val data: File,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val bytes = withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(data.absolutePath)
                val pic = retriever.embeddedPicture
                retriever.release()
                pic
            } catch (e: Exception) {
                null
            }
        } ?: return null

        val bitmap = withContext(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } ?: return null

        val drawable = BitmapDrawable(options.context.resources, bitmap)

        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<File> {
        override fun create(data: File, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.extension.lowercase() == "mp3") {
                return Mp3AlbumArtFetcher(data, options)
            }
            return null
        }
    }
}
