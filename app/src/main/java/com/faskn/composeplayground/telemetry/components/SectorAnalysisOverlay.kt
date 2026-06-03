package com.faskn.composeplayground.telemetry.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.telemetry.RacingTheme
import com.faskn.composeplayground.telemetry.TelemetryUtils
import com.faskn.composeplayground.telemetry.TelemetryUtils.format
import com.faskn.composeplayground.telemetry.data.LapSectorData
import com.faskn.composeplayground.telemetry.data.SectorInfo

@Composable
fun SectorAnalysisOverlay(sectorData: LapSectorData?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectorBox("S1", sectorData?.s1)
        SectorBox("S2", sectorData?.s2)
        SectorBox("S3", sectorData?.s3)
    }
}

@Composable
fun SectorBox(label: String, info: SectorInfo?, modifier: Modifier = Modifier) {
    val targetColor = when (info?.color) {
        1 -> Color(0xFF00C853) // Faster than Rival (Green)
        2 -> Color(0xFFAA00FF) // Personal/Session Best (Purple)
        3 -> Color(0xFFFF1744) // Slower than Rival (Red)
        else -> Color.DarkGray
    }
    val bgColor by animateColorAsState(
        targetValue = if (info != null && info.time > 0) targetColor.copy(0.85f) else Color.DarkGray.copy(0.6f),
        animationSpec = tween(500), label = "sectorColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor.copy(alpha = 0.35f))
            .border(2.dp, bgColor, RoundedCornerShape(6.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = Color.White.copy(0.9f),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp
        )
        AnimatedContent(
            targetState = info?.time ?: 0f,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "sectorTime"
        ) { time ->
            Text(
                if (time > 0) TelemetryUtils.lapTime(time) else "-",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                fontSize = 12.sp
            )
        }
        if (info != null && info.time > 0 && info.delta != 0f) {
            val sign = if (info.delta > 0) "+" else ""
            Text(
                "$sign${info.delta.format(3)}",
                color = Color.White.copy(0.9f),
                style = MaterialTheme.typography.titleSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Preview
@Composable
private fun SectorAnalysisOverlayPreview() {
    RacingTheme {
        SectorAnalysisOverlay(
            sectorData = LapSectorData(
                s1 = SectorInfo(39.068f, 1, -0.123f),
                s2 = SectorInfo(64.622f, 2, 0.456f),
                s3 = SectorInfo(40.294f, 3, -0.050f)
            )
        )
    }
}
