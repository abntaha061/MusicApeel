package com.example.data.utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object AlbumArtLoader {
    fun generatePlaceholderArt(songId: Long): GradientDrawable {
        val colors = listOf(
            intArrayOf(Color.parseColor("#443366"), Color.parseColor("#111122")),
            intArrayOf(Color.parseColor("#115588"), Color.parseColor("#051525")),
            intArrayOf(Color.parseColor("#883344"), Color.parseColor("#22050b")),
            intArrayOf(Color.parseColor("#226655"), Color.parseColor("#052210")),
            intArrayOf(Color.parseColor("#775533"), Color.parseColor("#201005")),
            intArrayOf(Color.parseColor("#663366"), Color.parseColor("#1a0520")),
            intArrayOf(Color.parseColor("#3F51B5"), Color.parseColor("#121833"))
        )
        val selectedColors = colors[(songId % colors.size).toInt()]
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            selectedColors
        ).apply {
            cornerRadius = 0f
        }
    }
}
