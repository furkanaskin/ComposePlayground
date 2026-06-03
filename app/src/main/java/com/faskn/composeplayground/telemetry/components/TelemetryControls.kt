package com.faskn.composeplayground.telemetry.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.faskn.composeplayground.telemetry.RacingTheme
import com.faskn.composeplayground.telemetry.TelemetryUtils
import com.faskn.composeplayground.ui.theme.RacingRed
import com.faskn.composeplayground.ui.theme.RacingRed3
import com.faskn.composeplayground.ui.theme.RacingWhite

@Composable
fun TelemetryControls(
    isPlaying: Boolean,
    currentProgress: Float,
    totalFrames: Int,
    currentTimeSeconds: Float,
    totalSessionTimeSeconds: Float,
    lapStartIndices: Map<Int, Int>,
    onTogglePlayback: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeGestures.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onTogglePlayback,
            modifier = Modifier
                .padding(top = 14.dp)
                .background(RacingRed3, RoundedCornerShape(50.dp))
                .size(32.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = RacingWhite,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        LapSeekbar(
            currentProgress = currentProgress,
            totalFrames = totalFrames,
            lapStartIndices = lapStartIndices,
            onSeek = onSeek,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(16.dp))

        Text(
            modifier = Modifier.padding(top = 14.dp),
            text = "${TelemetryUtils.sessionTime(currentTimeSeconds)} / ${
                TelemetryUtils.sessionTime(
                    totalSessionTimeSeconds
                )
            }",
            style = MaterialTheme.typography.labelMedium,
            color = RacingWhite,
            textAlign = TextAlign.Right
        )
    }
}

@Composable
private fun LapSeekbar(
    currentProgress: Float,
    totalFrames: Int,
    lapStartIndices: Map<Int, Int>,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedLaps = remember(lapStartIndices) { lapStartIndices.toList().sortedBy { it.second } }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle =
        MaterialTheme.typography.labelSmall.copy(color = RacingWhite.copy(alpha = 0.6f))

    val measuredLabels = remember(sortedLaps, labelStyle) {
        sortedLaps.map { (lap, _) ->
            textMeasurer.measure("Lap $lap", labelStyle)
        }
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .pointerInput(totalFrames) {
                fun calculateProgress(x: Float): Float {
                    return (x / size.width * totalFrames).coerceIn(0f, totalFrames.toFloat())
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onSeek(calculateProgress(down.position.x))
                    drag(down.id) { change: PointerInputChange ->
                        onSeek(calculateProgress(change.position.x))
                        change.consume()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackY = size.height / 2f + 8.dp.toPx()
            val trackHeight = 2.dp.toPx()
            val markerHeight = 12.dp.toPx()

            // Draw track
            drawRect(
                color = RacingWhite.copy(alpha = 0.3f),
                topLeft = Offset(0f, trackY - trackHeight / 2f),
                size = Size(size.width, trackHeight)
            )

            // Draw progress
            val progressX = (currentProgress / totalFrames) * size.width
            drawRect(
                color = RacingRed,
                topLeft = Offset(0f, trackY - trackHeight / 2f),
                size = Size(progressX, trackHeight)
            )

            // Draw lap divisions and labels
            sortedLaps.forEachIndexed { index, (_, startIdx) ->
                val startX = (startIdx.toFloat() / totalFrames) * size.width
                val endIdx =
                    if (index + 1 < sortedLaps.size) sortedLaps[index + 1].second else totalFrames
                val endX = (endIdx.toFloat() / totalFrames) * size.width

                // Vertical bar
                drawLine(
                    color = RacingWhite.copy(alpha = 0.8f),
                    start = Offset(startX, trackY - markerHeight / 2f),
                    end = Offset(startX, trackY + markerHeight / 2f),
                    strokeWidth = 1.dp.toPx()
                )

                // Label
                val textLayoutResult = measuredLabels[index]
                val labelX = startX + (endX - startX) / 2f - textLayoutResult.size.width / 2f
                val labelY = trackY - markerHeight - 16.dp.toPx()

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(labelX, labelY)
                )
            }

            // Draw thumb
            drawCircle(
                color = RacingRed,
                radius = 6.dp.toPx(),
                center = Offset(progressX, trackY)
            )
        }
    }
}

@Preview
@Composable
private fun TelemetryControlsPreview() {
    RacingTheme {
        TelemetryControls(
            isPlaying = false,
            currentProgress = 50f,
            totalFrames = 100,
            currentTimeSeconds = 120f,
            totalSessionTimeSeconds = 300f,
            lapStartIndices = mapOf(0 to 0, 1 to 40, 2 to 80),
            onTogglePlayback = {},
            onSeek = {}
        )
    }
}
