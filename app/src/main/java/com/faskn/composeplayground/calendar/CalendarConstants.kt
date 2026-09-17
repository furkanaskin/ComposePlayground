package com.faskn.composeplayground.calendar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val EventColors = listOf(
    Color(0xFF63D8FF),
    Color(0xFF7CF5B2),
    Color(0xFFFFB86B),
    Color(0xFF7CF5B2),
    Color(0xFFC084FC),
    Color(0xFF55E6E6),
    Color(0xFFFF7070),
    Color(0xFFE5F35B),
)
val DaysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
val CellHeight = 96.dp
val TimeColumnWidth = 58.dp
const val DaysInWeek = 7
const val CURRENT_TIME_HOUR = 8
const val CURRENT_TIME_MINUTE = 45

const val CAROUSEL_STAGGER_STEP_MS = 45L
const val CAROUSEL_MIN_SPAN_DEG = 70f
const val CAROUSEL_MAX_SPAN_DEG = 300f
const val CAROUSEL_SPAN_STEP_DEG = 46f
const val CAROUSEL_MIN_RADIUS_DP = 34f
const val CAROUSEL_MAX_RADIUS_DP = 168f
const val CAROUSEL_CARD_SPACING_FACTOR = 0.5f
