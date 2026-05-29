package com.example.data.scanner

import android.content.Context
import android.os.Environment
import com.example.data.db.SongDao
import com.example.data.db.SongEntity
import java.io.File

class MediaScanner(private val context: Context, private val songDao: SongDao) {

    suspend fun scanAndPopulate(force: Boolean = false) {
        val count = songDao.getTotalSongsCount()
        if (count > 0 && !force) {
            return
        }

        if (force) {
            songDao.deleteAll()
        }

        val collectedSongs = mutableListOf<SongEntity>()

        // 1. Scan device storage if permission is granted and external storage is mounted
        try {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (musicDir != null && musicDir.exists()) {
                scanDirectory(musicDir, collectedSongs)
            }
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && downloadsDir.exists()) {
                scanDirectory(downloadsDir, collectedSongs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Inject rich dynamic mock Arabic songs if no physical files were scanned
        if (collectedSongs.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            val mockSongs = listOf(
                SongEntity(
                    title = "يا جدع",
                    artist = "أحمد جمال",
                    album = "يلا نعيش",
                    duration = 208000L, // 3:28
                    filePath = "/mock/ahmed_gamal_ya_gada3.mp3",
                    playCount = 12,
                    lastPlayedTimestamp = now - 500000L,
                    dateAdded = now - (2 * dayMs) // Joined 2 days ago
                ),
                SongEntity(
                    title = "يوم تلات",
                    artist = "عمرو دياب",
                    album = "سهران",
                    duration = 244000L, // 4:04
                    filePath = "/mock/amr_diab_yom_talat.mp3",
                    playCount = 34,
                    lastPlayedTimestamp = now - 1000000L,
                    dateAdded = now - (10 * dayMs) // Joined 10 days ago
                ),
                SongEntity(
                    title = "نسم علينا الهوى",
                    artist = "فيروز",
                    album = "نسم علينا الهوى",
                    duration = 246000L, // 4:06
                    filePath = "/mock/fairouz_nasam_alayna.mp3",
                    playCount = 25,
                    lastPlayedTimestamp = now - 200000L,
                    dateAdded = now - (1 * dayMs) // Joined 1 day ago
                ),
                SongEntity(
                    title = "أنت عمري",
                    artist = "أم كلثوم",
                    album = "روائع أم كلثوم",
                    duration = 600000L, // 10 mins (cut short)
                    filePath = "/mock/oum_kalthoum_enta_omri.mp3",
                    playCount = 8,
                    lastPlayedTimestamp = now - (3 * dayMs),
                    dateAdded = now - (30 * dayMs) // Joined 30 days ago
                ),
                SongEntity(
                    title = "تملي معاك",
                    artist = "عمرو دياب",
                    album = "تملي معاك",
                    duration = 269000L, // 4:29
                    filePath = "/mock/amr_diab_tamally_maak.mp3",
                    playCount = 42,
                    lastPlayedTimestamp = now - 10000L,
                    dateAdded = now - (15 * dayMs)
                ),
                SongEntity(
                    title = "لو حبنا غلطة",
                    artist = "وائل كفوري",
                    album = "أفضل الأغاني",
                    duration = 254000L,
                    filePath = "/mock/wael_kfoury_law_hobna.mp3",
                    playCount = 19,
                    lastPlayedTimestamp = now - 1200000L,
                    dateAdded = now - (5 * dayMs)
                ),
                SongEntity(
                    title = "بكتب اسمك يا حبيبي",
                    artist = "فيروز",
                    album = "بكتب اسمك يا حبيبي",
                    duration = 210000L,
                    filePath = "/mock/fairouz_baktob_esmak.mp3",
                    playCount = 15,
                    lastPlayedTimestamp = now - 3500000L,
                    dateAdded = now - (12 * dayMs)
                )
            )
            songDao.insertAll(mockSongs)
        } else {
            songDao.insertAll(collectedSongs)
        }
    }

    private fun scanDirectory(dir: File, list: MutableList<SongEntity>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanDirectory(file, list)
            } else {
                val name = file.name.lowercase()
                if (name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".ogg") || name.endsWith(".wav")) {
                    val title = file.nameWithoutExtension.capitalize()
                    list.add(
                        SongEntity(
                            title = title,
                            artist = "فنان غير معروف",
                            album = "ألبوم غير معروف",
                            duration = 180000L, // Default mock duration 3 mins
                            filePath = file.absolutePath,
                            dateAdded = file.lastModified()
                        )
                    )
                }
            }
        }
    }
}
