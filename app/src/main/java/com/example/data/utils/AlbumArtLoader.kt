package com.example.data.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

object AlbumArtLoader {
    
    // Memory-aware LruCache configured based on 1/8 of available JVM max heap memory
    private val memoryCache: LruCache<Long, Bitmap> by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8 // 1/8 of runtime heap
        object : LruCache<Long, Bitmap>(cacheSize) {
            override fun sizeOf(key: Long, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }
    
    // Track failed IDs in an eviction-safe manner to prevent repetitive disk reads for invalid artwork
    private val failedIds = mutableSetOf<Long>()
    
    // Mutex & Deferred map to de-duplicate pending/simultaneous artwork retrieval requests
    private val pendingRequests = mutableMapOf<Long, Deferred<Bitmap?>>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Dynamic synchronous or semi-asynchronous loading
    suspend fun getOrLoad(songId: Long, filePath: String): Bitmap? {
        // 1. Memory check
        memoryCache.get(songId)?.let { return it }
        if (failedIds.contains(songId)) return null
        
        // 2. Pending requests de-duplication
        val deferred = mutex.withLock {
            pendingRequests[songId] ?: scope.async {
                loadFromFile(songId, filePath)
            }.also { pendingRequests[songId] = it }
        }
        
        val result = deferred.await()
        
        mutex.withLock {
            pendingRequests.remove(songId)
        }
        
        return result
    }

    // Keep legacy synchronous method signature intact to prevent any downstream compilation errors
    fun loadBitmap(songId: Long, filePath: String): Bitmap? {
        val cached = memoryCache.get(songId)
        if (cached != null) return cached
        if (failedIds.contains(songId)) return null
        
        val bitmap = loadFromFile(songId, filePath)
        if (bitmap != null) {
            memoryCache.put(songId, bitmap)
        } else {
            failedIds.add(songId)
        }
        return bitmap
    }
    
    private fun loadFromFile(songId: Long, filePath: String): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null
        
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val bytes = retriever.embeddedPicture
            retriever.release()
            
            bytes?.let { b ->
                // Decode bounds first to allocate memory economically
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(b, 0, b.size, options)
                
                // Scale down to ~200x200 size max since it's used inside cards/items (saving massive amounts of RAM)
                options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, 200, 200)
                options.inJustDecodeBounds = false
                
                BitmapFactory.decodeByteArray(b, 0, b.size, options)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    
    fun clearCache() {
        memoryCache.evictAll()
        failedIds.clear()
        pendingRequests.clear()
    }
}
