package com.faskn.composeplayground.telemetry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.telemetry.FONT_FEATURE_TNUM
import com.faskn.composeplayground.telemetry.RacingTheme
import com.faskn.composeplayground.telemetry.TelemetryUtils.format
import com.faskn.composeplayground.telemetry.racingColors
import com.faskn.composeplayground.ui.theme.Black900
import com.faskn.composeplayground.ui.theme.Gray400
import com.faskn.composeplayground.ui.theme.RacingWhite
import kotlin.math.abs

@Composable
fun racingMetricStyle(): TextStyle = MaterialTheme.typography.labelSmall.copy(
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = FONT_FEATURE_TNUM
)

@Composable
fun TelemetryComparisonRow(
    userSteeringSmoothness: Float,
    userTrailBraking: Float,
    userSteeringAngle: Float,
    rivalSteeringSmoothness: Float,
    rivalTrailBraking: Float,
    rivalSteeringAngle: Float,
    userSteeringRange: Pair<Float, Float>,
    rivalSteeringRange: Pair<Float, Float>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SmoothnessColumn(
            modifier = Modifier.weight(0.25f),
            steering = userSteeringSmoothness,
            braking = userTrailBraking,
            isLeft = true
        )

        SteeringBar(
            modifier = Modifier
                .align(Alignment.Bottom)
                .weight(0.5f)
                .padding(horizontal = 12.dp),
            userAngle = userSteeringAngle,
            rivalAngle = rivalSteeringAngle,
            userSteeringRange = userSteeringRange,
            rivalSteeringRange = rivalSteeringRange,
            userColor = racingColors.userDriver,
            rivalColor = racingColors.rivalDriver
        )

        SmoothnessColumn(
            modifier = Modifier.weight(0.25f),
            steering = rivalSteeringSmoothness,
            braking = rivalTrailBraking,
            isLeft = false
        )
    }
}

@Composable
fun SmoothnessColumn(
    steering: Float,
    braking: Float,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (isLeft) Alignment.Start else Alignment.End
    Column(
        modifier = modifier,
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricItem(
            label = "Steering Smoothness",
            value = "${steering.toInt()}%",
            color = getSmoothnessColor(steering),
            isLeft = isLeft
        )

        MetricItem(
            label = "Trail Braking",
            value = if (braking < 0f) "—" else "${braking.toInt()}%",
            color = if (braking < 0f) Gray400 else getSmoothnessColor(braking),
            isLeft = isLeft,
            footer = {
                Text(
                    text = "(Last Corner)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = RacingWhite
                )
            }
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color,
    isLeft: Boolean,
    footer: @Composable (() -> Unit)? = null
) {
    val alignment = if (isLeft) Alignment.Start else Alignment.End
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Gray400,
            textAlign = if (isLeft) TextAlign.Start else TextAlign.End
        )
        Text(
            text = value,
            style = racingMetricStyle(),
            color = color
        )
        footer?.invoke()
    }
}

@Composable
fun SteeringBar(
    userAngle: Float,
    rivalAngle: Float,
    userSteeringRange: Pair<Float, Float>,
    rivalSteeringRange: Pair<Float, Float>,
    userColor: Color,
    rivalColor: Color,
    modifier: Modifier = Modifier
) {
    val minAngle = minOf(userSteeringRange.first, rivalSteeringRange.first)
    val maxAngle = maxOf(userSteeringRange.second, rivalSteeringRange.second)
    val halfRange = maxOf(abs(minAngle), abs(maxAngle)).coerceAtLeast(1f)

    val userBias = (userAngle / halfRange).coerceIn(-1f, 1f)
    val rivalBias = (rivalAngle / halfRange).coerceIn(-1f, 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Steering Angle",
            style = MaterialTheme.typography.labelSmall,
            color = Gray400,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Track bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(2.dp))
                    .background(racingColors.divider)
            ) {
                // Zero marker — always at exact center (bias = 0f)
                Box(
                    Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .align(BiasAlignment(0f, 0f))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }

            // Rival dot
            Box(
                Modifier
                    .align(BiasAlignment(rivalBias, 0f))
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(rivalColor)
            )

            // User dot (drawn on top)
            Box(
                Modifier
                    .align(BiasAlignment(userBias, 0f))
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(userColor)
            )

            // Angle values below the bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${userAngle.format(1)}°",
                    style = racingMetricStyle().copy(fontWeight = FontWeight.ExtraBold),
                    color = userColor
                )
                Text(
                    text = "${rivalAngle.format(1)}°",
                    style = racingMetricStyle().copy(fontWeight = FontWeight.ExtraBold),
                    color = rivalColor
                )
            }
        }
    }
}

@Preview
@Composable
fun TelemetryComparisonRowPreview() {
    RacingTheme {
        Box(
            Modifier
                .background(Black900)
        ) {
            TelemetryComparisonRow(
                userSteeringSmoothness = 85f,
                userTrailBraking = 92f,
                userSteeringAngle = 63.5f,
                rivalSteeringSmoothness = 45f,
                rivalTrailBraking = -1f,
                rivalSteeringAngle = -30f,
                userSteeringRange = -90f to 90f,
                rivalSteeringRange = -90f to 90f
            )
        }
    }
}