package com.example.data.scanner

import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import java.io.File

class MusicFileObserver(
    private val directoryPath: String,
    private val onNewSongAdded: (String) -> Unit
) : FileObserver(directoryPath, CREATE or MOVED_TO) {

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return
        if (!path.endsWith(".mp3", ignoreCase = true)) return

        val fullPath = if (directoryPath.endsWith("/")) "$directoryPath$path" else "$directoryPath/$path"
        
        Handler(Looper.getMainLooper()).postDelayed({
            onNewSongAdded(fullPath)
        }, 2000)
    }
}
