package com.example.data.scanner

import android.os.FileObserver
import java.io.File

class MusicFileObserver(
    private val directoryPath: String,
    private val onNewSongAdded: (String) -> Unit
) : FileObserver(directoryPath, CLOSE_WRITE or MOVED_TO) {

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return
        if (!path.endsWith(".mp3", ignoreCase = true)) return

        val fullPath = if (directoryPath.endsWith("/")) "$directoryPath$path" else "$directoryPath/$path"
        onNewSongAdded(fullPath)
    }
}
