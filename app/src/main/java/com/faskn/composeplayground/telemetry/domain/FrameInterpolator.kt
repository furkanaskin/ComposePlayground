package com.faskn.composeplayground.telemetry.domain

import com.faskn.composeplayground.telemetry.TelemetryConstants.OUT_LAP_NUMBER
import com.faskn.composeplayground.telemetry.data.TelemetryFrame

class FrameInterpolator {
    private var cachedUserProgress = -1f
    private var cachedUserFrame: TelemetryFrame? = null
    private var cachedRivalUserProgress = -1f
    private var cachedRivalFrame: TelemetryFrame? = null

    fun getInterpolatedFrame(
        isUser: Boolean,
        progress: Float,
        userTelemetry: List<TelemetryFrame>,
        rivalBestLapTelemetry: List<TelemetryFrame>,
        userLapStartTimes: Map<Int, Float>,
        rivalBestLapStartTime: Float,
        rivalBestLapEndTime: Float
    ): TelemetryFrame? {
        val data = if (isUser) userTelemetry else rivalBestLapTelemetry
        if (data.isEmpty()) return null

        if (isUser) {
            if (progress == cachedUserProgress) return cachedUserFrame
            val idx = progress.toInt()
            val nextIdx = (idx + 1).coerceAtMost(data.lastIndex)
            val result = if (idx == nextIdx) data[idx] else TelemetryFrame.lerp(
                data[idx],
                data[nextIdx],
                progress - idx
            )
            cachedUserProgress = progress
            cachedUserFrame = result
            return result
        }

        if (progress == cachedRivalUserProgress) return cachedRivalFrame
        val userFrame = getInterpolatedFrame(
            isUser = true,
            progress = progress,
            userTelemetry = userTelemetry,
            rivalBestLapTelemetry = rivalBestLapTelemetry,
            userLapStartTimes = userLapStartTimes,
            rivalBestLapStartTime = rivalBestLapStartTime,
            rivalBestLapEndTime = rivalBestLapEndTime
        ) ?: return null
        
        if (userFrame.lap == OUT_LAP_NUMBER) return null

        val userLapStartTs = userLapStartTimes[userFrame.lap] ?: return null
        val userElapsed = userFrame.timeSeconds - userLapStartTs
        val rivalTargetTs = rivalBestLapStartTime + userElapsed

        if (rivalTargetTs < rivalBestLapStartTime) return null
        if (rivalTargetTs >= rivalBestLapEndTime) return data.last()

        var idx = data.binarySearch { it.timeSeconds.compareTo(rivalTargetTs) }
        if (idx < 0) idx = -(idx + 1)
        idx = idx.coerceIn(1, data.lastIndex)

        val f1 = data[idx - 1]
        val f2 = data[idx]
        val span = f2.timeSeconds - f1.timeSeconds
        val frac = if (span > 0f) (rivalTargetTs - f1.timeSeconds) / span else 0f
        val result = TelemetryFrame.lerp(f1, f2, frac)
        cachedRivalUserProgress = progress
        cachedRivalFrame = result
        return result
    }

    fun getRivalFrameAtAbsoluteDist(targetDist: Float, data: List<TelemetryFrame>): TelemetryFrame? {
        if (data.isEmpty()) return null

        if (targetDist <= 0f) return data.first()
        if (targetDist >= data.last().lapDistance) return data.last()

        var idx = data.binarySearch { it.lapDistance.compareTo(targetDist) }
        if (idx < 0) idx = -(idx + 1)
        idx = idx.coerceIn(1, data.lastIndex)

        val f1 = data[idx - 1]
        val f2 = data[idx]
        val span = f2.lapDistance - f1.lapDistance
        val frac = if (span > 0f) (targetDist - f1.lapDistance) / span else 0f
        return TelemetryFrame.lerp(f1, f2, frac)
    }
}
