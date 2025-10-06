package com.faskn.composeplayground.agsl

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.agsl.SpirographConstants.HUE_RANGE
import com.faskn.composeplayground.agsl.SpirographConstants.L1_RANGE
import com.faskn.composeplayground.agsl.SpirographConstants.L2_RANGE
import com.faskn.composeplayground.agsl.SpirographConstants.S1_RANGE
import com.faskn.composeplayground.agsl.SpirographConstants.S2_RANGE
import com.faskn.composeplayground.ui.theme.Black700
import com.faskn.composeplayground.ui.theme.Gray300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParameterSlider(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    isHueSlider: Boolean = false,
    isIntegerSlider: Boolean = false,
    baseHue: Float = 0f
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = if (isIntegerSlider) "$label: ${value.toInt()}" else "$label: %.2f".format(value),
            modifier = Modifier.weight(0.2f),
            fontSize = 12.sp,
            color = Color.White
        )
        Slider(
            value = value,
            onValueChange = { newValue ->
                if (isIntegerSlider) {
                    onValueChange(newValue.toInt().toFloat())
                } else {
                    onValueChange(newValue)
                }
            },
            track = { sliderState ->
                val trackProgress =
                    (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)

                if (isHueSlider) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.hsv(0f, 1f, 1f),
                                        Color.hsv(60f, 1f, 1f),
                                        Color.hsv(120f, 1f, 1f),
                                        Color.hsv(180f, 1f, 1f),
                                        Color.hsv(240f, 1f, 1f),
                                        Color.hsv(300f, 1f, 1f),
                                        Color.hsv(355f, 1f, 1f)
                                    )
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Gray300, shape = RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(trackProgress)
                                .height(4.dp)
                                .background(
                                    color = Color.hsv(baseHue, 0.8f, 1f).copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            },
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = MutableInteractionSource(),
                    enabled = true,
                    thumbSize = DpSize(20.dp, 20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.hsv(
                            if (isHueSlider) value else baseHue, 1f, 1f
                        )
                    )
                )
            },
            valueRange = valueRange,
            modifier = Modifier
                .weight(0.75f)
                .height(30.dp)
        )
    }
}

@Composable
fun ParameterControls(
    params: SpirographParams,
    onParamsChange: (SpirographParams) -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Black700.copy(alpha = 0.50f))
            .padding(
                top = 24.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + 8.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ParameterSlider(
            label = "L1",
            value = params.l1,
            onValueChange = { onParamsChange(params.copy(l1 = it)) },
            valueRange = L1_RANGE,
            baseHue = params.baseHue
        )
        ParameterSlider(
            label = "L2",
            value = params.l2,
            onValueChange = { onParamsChange(params.copy(l2 = it)) },
            valueRange = L2_RANGE,
            baseHue = params.baseHue
        )
        ParameterSlider(
            label = "S1",
            value = params.s1,
            onValueChange = { onParamsChange(params.copy(s1 = it)) },
            valueRange = S1_RANGE,
            isIntegerSlider = true,
            baseHue = params.baseHue
        )
        ParameterSlider(
            label = "S2",
            value = params.s2,
            onValueChange = { onParamsChange(params.copy(s2 = it)) },
            valueRange = S2_RANGE,
            isIntegerSlider = true,
            baseHue = params.baseHue
        )
        ParameterSlider(
            label = "HSV",
            value = params.baseHue,
            onValueChange = { onParamsChange(params.copy(baseHue = it)) },
            valueRange = HUE_RANGE,
            isHueSlider = true,
            baseHue = params.baseHue
        )
    }
}
