package com.faskn.composeplayground.telemetry.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.telemetry.RacingTheme
import com.faskn.composeplayground.telemetry.TelemetryUtils.format
import com.faskn.composeplayground.telemetry.data.LapCompletionState
import com.faskn.composeplayground.ui.theme.AndroidGreen
import com.faskn.composeplayground.ui.theme.Magenta100
import com.faskn.composeplayground.ui.theme.RacingRed


@Composable
fun LapSelector(availableLaps: List<Int>, selectedLap: Int, onLapSelected: (Int) -> Unit) {
    val currentIndex = remember(availableLaps, selectedLap) { availableLaps.indexOf(selectedLap) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniActionButton("<") { if (currentIndex > 0) onLapSelected(availableLaps[currentIndex - 1]) }
        Text(
            "Lap $selectedLap",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        MiniActionButton(">") {
            if (currentIndex < availableLaps.lastIndex) onLapSelected(
                availableLaps[currentIndex + 1]
            )
        }
    }
}

@Composable
fun DeltaDisplay(
    delta: Float,
    currentLapTime: String,
    isOutLap: Boolean,
    lapCompletion: LapCompletionState?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = lapCompletion,
            transitionSpec = {
                if (targetState != null) {
                    (fadeIn(animationSpec = tween(500)) + slideInVertically(tween(500))) togetherWith
                            (fadeOut(animationSpec = tween(500)) + slideOutVertically(tween(500)))
                } else {
                    fadeIn(animationSpec = tween(100)) togetherWith fadeOut(
                        animationSpec = tween(
                            100
                        )
                    )
                }
            },
            label = "lapCompletionAnimation"
        ) { completion ->
            if (completion != null) {
                LapCompletionColumn(completion)
            } else {
                CurrentLapTimeColumn(delta, currentLapTime, isOutLap)
            }
        }
    }
}

@Composable
private fun LapCompletionColumn(completion: LapCompletionState) {
    val color = if (completion.isBest) Magenta100 else AndroidGreen
    val labelSmallSize = MaterialTheme.typography.labelSmall.fontSize
    val density = LocalDensity.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(with(density) { labelSmallSize.toDp() }))
        Text(
            if (completion.isBest) "BEST LAP" else "LAST LAP",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
            color = color
        )
        Text(
            completion.time,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = color
        )
    }
}

@Composable
private fun CurrentLapTimeColumn(
    delta: Float,
    currentLapTime: String,
    isOutLap: Boolean
) {
    val displayDelta = if (isOutLap) 0f else delta
    val deltaColor = when {
        isOutLap -> Color.White
        displayDelta > 0.005f -> RacingRed
        displayDelta < -0.005f -> Color.Green
        else -> Color.White
    }
    val sign = if (displayDelta > 0) "+" else if (displayDelta < 0) "" else "+"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val textMeasurer = rememberTextMeasurer()
        val deltaTextStyle = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold
        )
        val density = LocalDensity.current
        val dashWidth = remember(textMeasurer, deltaTextStyle, density) {
            with(density) {
                textMeasurer.measure("-", deltaTextStyle).size.width.toDp()
            }
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = dashWidth),
            textAlign = TextAlign.Center,
            text = "$sign${displayDelta.format(3)}",
            style = deltaTextStyle,
            color = deltaColor
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = currentLapTime,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 12.sp
            ),
            color = Color.White
        )
    }
}

@Composable
fun MiniActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}


@Preview
@Composable
private fun LapSelectorPreview() {
    RacingTheme {
        LapSelector(availableLaps = listOf(0, 1, 2), selectedLap = 1, onLapSelected = {})
    }
}

@Preview
@Composable
private fun DeltaDisplayPreview() {
    RacingTheme {
        DeltaDisplay(
            delta = -0.055f,
            currentLapTime = "01:23:456",
            isOutLap = false,
            lapCompletion = null
        )
    }
}

@Preview
@Composable
private fun BestLapDisplayPreview() {
    RacingTheme {
        DeltaDisplay(
            delta = -0.055f,
            currentLapTime = "01:21:456",
            isOutLap = false,
            lapCompletion = LapCompletionState(
                "01:21:456",
                isBest = true
            ),
        )
    }
}

@Preview
@Composable
private fun LastLapDisplayPreview() {
    RacingTheme {
        DeltaDisplay(
            delta = -0.055f,
            currentLapTime = "01:21:456",
            isOutLap = false,
            lapCompletion = LapCompletionState(
                "01:21:456",
                isBest = false
            ),
        )
    }
}


