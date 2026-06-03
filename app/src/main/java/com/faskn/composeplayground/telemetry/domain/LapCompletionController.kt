package com.faskn.composeplayground.telemetry.domain

import com.faskn.composeplayground.telemetry.TelemetryConstants.SECTOR_PREVIEW_DURATION_SECONDS
import com.faskn.composeplayground.telemetry.TelemetryUtils
import com.faskn.composeplayground.telemetry.data.LapCompletionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LapCompletionController(private val scope: CoroutineScope) {
    private var completionJob: Job? = null
    var isPreviewingLastLapSectors = false
        private set

    fun triggerLapCompletion(
        lapTime: Float,
        isBest: Boolean,
        onUpdate: (LapCompletionState?) -> Unit,
        onFinish: () -> Unit
    ) {
        cancel()
        completionJob = scope.launch {
            isPreviewingLastLapSectors = true
            onUpdate(
                LapCompletionState(
                    TelemetryUtils.mmsss(lapTime),
                    isBest
                )
            )

            delay(SECTOR_PREVIEW_DURATION_SECONDS)
            isPreviewingLastLapSectors = false
            onUpdate(null)
            onFinish()
        }
    }

    fun cancel() {
        completionJob?.cancel()
    }
}
