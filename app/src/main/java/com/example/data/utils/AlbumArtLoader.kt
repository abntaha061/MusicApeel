package com.example.data.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache

object AlbumArtLoader {
    
    // Local memory cache — songId to Bitmap
    private val memoryCache = LruCache<Long, Bitmap>(50) // Store up to 50 decoded images
    // Also store a cache of failed song IDs to avoid retrying file access repeatedly
    private val failedIds = mutableSetOf<Long>()
    
    fun loadBitmap(songId: Long, filePath: String): Bitmap? {
        // 1. Check memory cache first
        val cached = memoryCache.get(songId)
        if (cached != null) return cached
        
        if (failedIds.contains(songId)) return null
        
        // 2. Extract embedded artwork from audio file metadata
        val bitmap = try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val bytes = retriever.embeddedPicture
            retriever.release()
            bytes?.let { 
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        } catch (e: Exception) {
            null
        }
        
        if (bitmap != null) {
            memoryCache.put(songId, bitmap)
        } else {
            failedIds.add(songId)
        }
        return bitmap
    }
    
    fun clearCache() {
        memoryCache.evictAll()
        failedIds.clear()
    }
}
