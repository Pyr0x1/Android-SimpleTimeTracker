package com.example.util.simpletimetracker.navigation.params.screen

import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize

@Parcelize
data class EmojiSelectionDialogParams(
    @ColorInt val color: Int = Color.BLACK,
    val emojiCodes: List<String> = emptyList()
) : Parcelable, ScreenParams