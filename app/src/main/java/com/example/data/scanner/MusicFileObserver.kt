package com.example.data.scanner

import android.os.FileObserver
import java.io.File

class MusicFileObserver(path: String, private val onChange: () -> Unit) {
    private var observer: FileObserver? = null

    init {
        try {
            val file = File(path)
            if (file.exists()) {
                observer = object : FileObserver(file.absolutePath, CREATE or DELETE or MODIFY) {
                    override fun onEvent(event: Int, path: String?) {
                        onChange()
                    }
                }
                observer?.startWatching()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        observer?.stopWatching()
    }
}
