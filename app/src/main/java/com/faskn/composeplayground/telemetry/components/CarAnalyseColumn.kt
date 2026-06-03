package com.faskn.composeplayground.telemetry.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.faskn.composeplayground.R
import com.faskn.composeplayground.telemetry.RacingTheme
import com.faskn.composeplayground.telemetry.TelemetryUtils.format
import com.faskn.composeplayground.telemetry.data.CarConfig
import com.faskn.composeplayground.telemetry.racingColors
import com.faskn.composeplayground.ui.theme.Black900
import com.faskn.composeplayground.ui.theme.Gray400

@Composable
fun CarAnalyseColumn(
    modifier: Modifier = Modifier,
    @DrawableRes carImage: Int,
    throttle: Float,
    brake: Float,
    gear: Int,
    speed: Float,
    config: CarConfig?,
    showConfig: Boolean,
    onConfigToggle: () -> Unit,
    isLeft: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Config",
                tint = Gray400,
                modifier = Modifier
                    .align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
                    .padding(
                        start = if (isLeft) 12.dp else 0.dp,
                        end = if (isLeft) 0.dp else 12.dp
                    )
                    .size(16.dp)
                    .clickable { onConfigToggle() }
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (isLeft) {
                Spacer(Modifier.width(12.dp))
                AnimatedInfoToggle(gear, speed, config, showConfig, Alignment.Start)
                Spacer(Modifier.weight(1f))
                CarImage(carImage)
                Spacer(Modifier.weight(1f))
                CarBarsColumn(throttle, brake, true)
                Spacer(Modifier.weight(0.1f))
            } else {
                Spacer(Modifier.weight(0.1f))
                CarBarsColumn(throttle, brake, false)
                Spacer(Modifier.weight(1f))
                CarImage(carImage)
                Spacer(Modifier.weight(1f))
                AnimatedInfoToggle(gear, speed, config, showConfig, Alignment.End)
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

internal fun getSmoothnessColor(percentage: Float): Color {
    val fraction = (percentage / 100f).coerceIn(0f, 1f)
    return if (fraction < 0.5f) {
        lerp(Color.Red, Color.Yellow, fraction * 2f)
    } else {
        lerp(Color.Yellow, Color.Green, (fraction - 0.5f) * 2f)
    }
}

@Composable
private fun AnimatedInfoToggle(
    gear: Int,
    speed: Float,
    config: CarConfig?,
    showConfig: Boolean,
    alignment: Alignment.Horizontal
) {
    AnimatedContent(
        targetState = showConfig,
        transitionSpec = {
            fadeIn().togetherWith(fadeOut())
        },
        label = "CarInfoConfigToggle"
    ) { targetShowConfig ->
        if (targetShowConfig) {
            CarConfigColumn(config ?: CarConfig(), alignment)
        } else {
            CarInfoColumn(gear, speed, alignment)
        }
    }
}

@Composable
private fun CarConfigColumn(config: CarConfig, alignment: Alignment.Horizontal) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = alignment
    ) {
        ConfigItem("ABS", config.abs, alignment)
        ConfigItem("Brake Bias", config.brakeBias, alignment)
        ConfigItem("TC (Cut/Slip)", config.tc, alignment)
        ConfigItem(
            "Top Speed",
            "${config.topSpeed.format(1)} km/h",
            alignment
        )
    }
}

@Composable
private fun ConfigItem(label: String, value: String, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Gray400)
        Text(
            value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = if (alignment == Alignment.End) TextAlign.End else TextAlign.Start
        )
    }
}

@Composable
private fun CarInfoColumn(gear: Int, speed: Float, alignment: Alignment.Horizontal) {
    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = alignment) {
        Text("Gear", style = MaterialTheme.typography.labelSmall, color = Gray400)
        Text(
            if (gear == 0) "N" else gear.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text("Speed", style = MaterialTheme.typography.labelSmall, color = Gray400)
        Text(
            "${speed.toInt()} km/h",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
private fun CarImage(@DrawableRes carImage: Int) {
    Image(
        painterResource(carImage),
        null,
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(400f / 1000f)
    )
}

@Composable
private fun CarBarsColumn(throttle: Float, brake: Float, isLeft: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(end = if (isLeft) 16.dp else 0.dp, start = if (isLeft) 0.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SmallVerticalBar(
            Modifier
                .weight(1f)
                .padding(top = 16.dp), throttle, Color.Green
        )
        SmallVerticalBar(
            Modifier
                .weight(1f)
                .padding(bottom = 16.dp), brake, Color.Red
        )
    }
}

@Composable
private fun SmallVerticalBar(modifier: Modifier, value: Float, color: Color) {
    Box(
        modifier = modifier
            .width(6.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.DarkGray),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight((value / 100f).coerceIn(0f, 1f))
                .background(color)
        )
    }
}

@Preview
@Composable
private fun CarAnalyseColumnPreview() {
    RacingTheme {
        Row(
            modifier = Modifier
                .background(Black900)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarAnalyseColumn(
                modifier = Modifier.weight(1f),
                carImage = R.drawable.porsche_grello_v,
                throttle = 80f,
                brake = 20f,
                gear = 3,
                speed = 180f,
                config = CarConfig("1:1", "54:46", "2 (1/3)", 280f),
                showConfig = false,
                onConfigToggle = {},
                isLeft = true
            )

            Box(Modifier.weight(0.1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(160.dp)
                        .background(racingColors.divider)
                )
            }

            CarAnalyseColumn(
                modifier = Modifier.weight(1f), carImage = R.drawable.porsche_roxy_v,
                throttle = 40f,
                brake = 60f,
                gear = 2,
                speed = 120f,
                config = CarConfig("2:2", "50:50", "3 (1/4)", 260f),
                showConfig = false,
                onConfigToggle = {},
                isLeft = false
            )
        }
    }
}
