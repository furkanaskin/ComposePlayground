package com.faskn.composeplayground.telemetry

import androidx.compose.ui.unit.dp

object TelemetryConstants {
    // Data Sampling & Logic
    const val GPS_SAMPLING_MULTIPLIER = 0.1f
    const val TELEMETRY_SAMPLING_MULTIPLIER = 0.5f
    const val OUT_LAP_NUMBER = 0
    const val DEFAULT_TRACK_WIDTH_METERS = 15f
    const val MIN_TRACK_WIDTH_METERS = 8f
    const val SECTOR_PREVIEW_DURATION_SECONDS = 3500L
    const val METER_TO_LAT_LON = 111_320f
    const val LMU_REF_LAT_COS = 0.5f // Le Mans Ultimate Const - cos(60°)

    const val MIN_BRAKE_FRAMES = 5
    const val STEERING_TRIM_PERCENT = 0.10f
    const val MAX_NORMALIZED_DISTANCE = 1.01f
    const val FALLBACK_TRACK_PERIMETER = 6979f

    const val STEERING_WINDOW             = 150   // 150 frames @ 100Hz = 1.5s
    const val BRAKE_ACTIVE_MIN            = 5f    // 0-100 scale
    const val LAT_G_CORNER_MIN            = 0.3f  // safe identification for corner entry
    const val STEERING_ACTIVE_MIN         = 3.0f  // degrees — ignore micro-inputs on straights
    const val STEERING_STRAIGHT_THRESHOLD = 0.10f // p90 of straight deltas (degrees/frame)
    const val STEERING_CORNER_THRESHOLD   = 0.29f // p90 of corner deltas   (degrees/frame)


    // TrackMap UI Constants
    const val DEFAULT_TRACK_SCALE = 3.125f
    const val MAX_TRACK_SCALE = 100f
    val DRIVER_PATH_STROKE_WIDTH = 2.dp
    val DRIVER_INDICATOR_RADIUS = 2.dp
    val DRIVER_INDICATOR_BORDER = 0.25.dp
    const val DRIVER_INDICATOR_INNER_SCALE = 0.5f
    const val TRACK_EDGE_STROKE_SCALE = 1.1f
    const val TRACK_ASPHALT_STROKE_SCALE = 1f
    const val TRACK_SCRATCH_STROKE_SCALE = 0.05f
    const val TRACK_GLOW_STROKE_SCALE = 2.5f
    const val TRACK_GLOW_BLUR_SCALE = 0.75f
    const val MARKER_LINE_WIDTH_SCALE = 0.5f
    const val TRACK_ANIMATION_DURATION_MS = 1000

    // Screen UI Constants
    const val TRACK_MAP_HEIGHT_FRACTION = 0.33f
    const val FRAME_INTERVAL_NANOS = 10_000_000L
    const val VERSUS_TRANSITION_DELAY_MS = 3750L
}
