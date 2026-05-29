package com.example.presentation.player

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.example.data.db.SongEntity
import com.example.data.lyrics.LyricLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics = _lyrics.asStateFlow()

    private val _dominantColors = MutableStateFlow<List<Color>>(listOf(Color(0xFF2196F3), Color(0xFF141414)))
    val dominantColors = _dominantColors.asStateFlow()

    fun updateSong(song: SongEntity?) {
        if (song == null) {
            _lyrics.value = emptyList()
            return
        }

        // Return a customized set of beautifully synced Arabic lyric lines depending on the track title!
        val lyricsText = when (song.title) {
            "يا جدع" -> listOf(
                LyricLine(2000L, "يا جدع.. يا جدع يا جدع"),
                LyricLine(8000L, "مين غيرك في الشدة واقف جنبي؟"),
                LyricLine(14000L, "يا اللي كلامك كله دهب وبيريح قلبي"),
                LyricLine(20000L, "حبك ده حكاية كبيرة وأغلى ما عندي"),
                LyricLine(26000L, "يا جدع قلبي وعيني معاك على طول الدرب"),
                LyricLine(32000L, "دقات قلبي بتنادي اسمك يا بطل الحب")
            )
            "يوم تلات" -> listOf(
                LyricLine(2000L, "يوم تلات.. تلات بنات"),
                LyricLine(6000L, "أنا شفت الحلاوة والجمال من غير كلام"),
                LyricLine(12000L, "واحدة سمرا وجميلة مليانة بالدلال"),
                LyricLine(18000L, "بالجمال الروحي سحرتني يا سلام"),
                LyricLine(24000L, "يوم تلات في عمري ما هنساه أبدًا مدي الحياة"),
                LyricLine(30000L, "قلبي تاه وداب في هواهم والغرام خداه")
            )
            "نسم علينا الهوى" -> listOf(
                LyricLine(2000L, "نسم علينا الهوى من مفرق الوادي"),
                LyricLine(9000L, "يا هوى دخل الهوى خذني على بلادي"),
                LyricLine(16000L, "يا نسيم الشوق رجّعني للوطن الغالي"),
                LyricLine(23000L, "صوب الحبايب والبيت العالي بالبالي"),
                LyricLine(30000L, "نسم علينا الهوى.. خذني على بلادي")
            )
            "أنت عمري" -> listOf(
                LyricLine(2000L, "رجعتوني للأيام القديمة والذكريات"),
                LyricLine(12000L, "صالحوني على الأشواق وعلى اللي فات"),
                LyricLine(22000L, "رجعوني لحنان قلبي وعيونك الدفايات"),
                LyricLine(32000L, "أنت عمري اللي ابتدي بنورك صباحه يا غالي")
            )
            else -> listOf(
                LyricLine(2000L, "موسيقى تملأ الوجدان والروح صدى.."),
                LyricLine(8000L, "أنغام تعزف تفاصيل الساعات والأحلام"),
                LyricLine(14000L, "كل نغمة تروي للحنين حكاية حب وسلام")
            )
        }
        _lyrics.value = lyricsText

        // Generate matching visual theme colors dynamically based on track titles
        val isWarm = song.title == "يوم تلات" || song.title == "أنت عمري"
        _dominantColors.value = if (isWarm) {
            listOf(Color(0xFFE57373), Color(0xFF141414))
        } else {
            listOf(Color(0xFF4FC3F7), Color(0xFF141414))
        }
    }
}
