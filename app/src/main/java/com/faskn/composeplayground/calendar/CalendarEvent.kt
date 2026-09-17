package com.faskn.composeplayground.calendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String,
    val startTime: Int,
    val endTime: Int,
    val dayOfWeek: Int,
    val color: Color,
    val icon: ImageVector? = null
)

@Immutable
data class CellIndex(val day: Int, val hour: Int)
