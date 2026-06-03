package com.faskn.composeplayground.telemetry

import androidx.compose.ui.geometry.Offset
import com.faskn.composeplayground.telemetry.TelemetryConstants.METER_TO_LAT_LON
import com.faskn.composeplayground.telemetry.data.BestLapTimeEntity
import com.faskn.composeplayground.telemetry.data.TelemetryFrame
import com.faskn.composeplayground.telemetry.data.TrackMarker
import com.faskn.composeplayground.telemetry.data.TrackProjection
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sqrt

object TelemetryUtils {
    /**
     * Formats seconds into MM:SS:SSS string.
     */
    fun mmsss(seconds: Float): String {
        if (seconds <= 0f) return "00:00:000"
        val mins = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        val millis = ((seconds % 1) * 1000).toInt()
        return String.format(Locale.US, "%02d:%02d:%03d", mins, secs, millis)
    }

    /**
     * Formats seconds into M:SS.SSS string.
     */
    fun lapTime(seconds: Float): String {
        if (seconds <= 0f) return "--:--.---"
        val minutes = (seconds / 60).toInt()
        val remainingSeconds = seconds % 60
        return String.format(Locale.US, "%d:%06.3f", minutes, remainingSeconds)
    }

    /**
     * Formats seconds into hh:mm:ss or mm:ss string.
     */
    fun sessionTime(seconds: Float): String {
        val hrs = (seconds / 3600).toInt()
        val mins = ((seconds % 3600) / 60).toInt()
        val secs = (seconds % 60).toInt()
        return if (hrs > 0) String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        else String.format(Locale.US, "%d:%02d", mins, secs)
    }

    fun Float.format(decimals: Int): String {
        return String.format(Locale.US, "%.${decimals}f", this)
    }

    fun createMarker(
        label: String,
        index: Int,
        data: List<TelemetryFrame>,
        projection: TrackProjection?,
    ): TrackMarker {
        val f = data.getOrNull(index) ?: return TrackMarker(label, Offset.Zero, 0f, Offset.Zero)
        val pos = f.position ?: Offset.Zero

        val prevIdx = (index - 5).coerceAtLeast(0)
        val nextIdx = (index + 5).coerceAtMost(data.lastIndex)
        val prevPos = data.getOrNull(prevIdx)?.position ?: pos
        val nextPos = data.getOrNull(nextIdx)?.position ?: pos

        val dx = nextPos.x - prevPos.x
        val dy = nextPos.y - prevPos.y
        val len = sqrt((dx * dx) + (dy * dy)).coerceAtLeast(1e-9f)

        val nx = -dy / len
        val ny = dx / len
        val angle = if (prevPos != nextPos) atan2(dy, dx) else 0f

        val meterToNorm = 1f / ((projection?.maxSpan ?: 1f) * METER_TO_LAT_LON)
        val centeredPos = Offset(
            pos.x - nx * f.lateral * meterToNorm,
            pos.y - ny * f.lateral * meterToNorm
        )
        return TrackMarker(label, centeredPos, angle, Offset(nx, ny))
    }

    fun List<Float>.lerpAt(idx1: Int, idx2: Int, frac: Float): Float {
        val v1 = getOrElse(idx1) { 0f }
        val v2 = getOrElse(idx2) { 0f }
        return v1 + (v2 - v1) * frac
    }

    fun List<BestLapTimeEntity>.minPositive(): BestLapTimeEntity? {
        return asSequence().filter { it.value > 0 }.minByOrNull { it.value }
    }
}
