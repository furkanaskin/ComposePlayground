package com.faskn.composeplayground.agsl

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.agsl.SpirographConstants.DEFAULT_HUE
import com.faskn.composeplayground.agsl.SpirographConstants.DEFAULT_L1
import com.faskn.composeplayground.agsl.SpirographConstants.DEFAULT_L2
import com.faskn.composeplayground.agsl.SpirographConstants.DEFAULT_S1
import com.faskn.composeplayground.agsl.SpirographConstants.DEFAULT_S2
import com.faskn.composeplayground.agsl.SpirographConstants.RANDOM_L_MAX
import com.faskn.composeplayground.agsl.SpirographConstants.RANDOM_L_MIN
import com.faskn.composeplayground.agsl.SpirographConstants.RANDOM_S1_MAX
import com.faskn.composeplayground.agsl.SpirographConstants.RANDOM_S1_MIN
import com.faskn.composeplayground.agsl.SpirographConstants.RANDOM_S2_MAX
import com.faskn.composeplayground.agsl.SpirographConstants.RANDOM_S2_MIN
import com.faskn.composeplayground.ui.theme.Black700
import com.faskn.composeplayground.ui.theme.Black900
import com.faskn.composeplayground.ui.theme.White800
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import kotlin.random.Random

@Language("AGSL")
private const val shaderCode = """
uniform float2 resolution;
uniform float time, baseHue, L1, L2, S1, S2;

/**
 * Main idea:
 * - Two arms connected together
 * - Arm1: rotates around center (length=L1, speed=S1)
 * - Arm2: attached to end of Arm1 (length=L2, speed=S2) 
 * - We draw at the tip of Arm2
 * 
 * Trail effect:
 * - Draw 180 points from past to present
 * - Old points are dim, new points are bright
 */

const int TRAIL_STEPS = 180;
const float TRAIL_STEP_SIZE = 0.02;
const float LINE_WIDTH = 12.0;
const float HUE_RANGE = 60.0;

// Convert HSV color to RGB
half3 hsv2rgb(half3 hsv) {
    half4 K = half4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
    half3 p = abs(fract(hsv.xxx + K.xyz) * 6.0 - K.www);
    return hsv.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), hsv.y);
}

// Rotate a vector by given cosine and sine values
float2 rotateVector(float2 vector, float cosAngle, float sinAngle) {
    return float2(
        cosAngle * vector.x - sinAngle * vector.y,
        sinAngle * vector.x + cosAngle * vector.y
    );
}

// Get trail color based on age 
half3 getTrailColor(int stepIndex, float baseHue) {
    float age = float(stepIndex) / float(TRAIL_STEPS); 
    half fade = half(1.0 - age);
    
    float hueShift = age * HUE_RANGE - (HUE_RANGE * 0.5);  
    float currentHue = fract((baseHue + hueShift) / 360.0);  
    
    return hsv2rgb(half3(half(currentHue), fade, 1.0)) * fade;
}

half4 main(float2 coord) {
    float2 center = resolution / 2.0;
    float maxRadius = min(resolution.x, resolution.y) / 3.0;
    half3 finalColor = half3(0.0);

    // Calculate arm lengths
    float totalLength = L1 + L2;
    float arm1Length = (L1 / totalLength) * maxRadius;
    float arm2Length = (L2 / totalLength) * maxRadius;

    // Pre-calculate rotation increments
    float deltaAngle1 = TRAIL_STEP_SIZE * S1;
    float deltaAngle2 = TRAIL_STEP_SIZE * S2;
    float cos1 = cos(deltaAngle1), sin1 = sin(deltaAngle1);
    float cos2 = cos(deltaAngle2), sin2 = sin(deltaAngle2);

    // Starting arm vectors at current time - Start at (0,1) Y-axis top
    float startAngle1 = time * S1 + 1.5708; // Add π/2 to start at Y-axis
    float startAngle2 = time * S2 + 1.5708; // Add π/2 to start at Y-axis
    float2 arm1Vector = float2(cos(startAngle1), sin(startAngle1)) * arm1Length;
    float2 arm2Vector = float2(cos(startAngle2), sin(startAngle2)) * arm2Length;

    float lineWidthSquared = LINE_WIDTH * LINE_WIDTH;

    // Draw trail points with vector rotation
    for (int i = 0; i < TRAIL_STEPS; i++) {
        // Current drawing position = center + arm1 + arm2
        float2 drawingPoint = center + arm1Vector + arm2Vector;
        
        // Distance check
        float2 diff = coord - drawingPoint;
        float distSquared = dot(diff, diff);
        
        if (distSquared < lineWidthSquared) {
            half3 trailColor = getTrailColor(i, baseHue);
            finalColor += trailColor;
        }

        // Rotate arms for next time step
        arm1Vector = rotateVector(arm1Vector, cos1, sin1);
        arm2Vector = rotateVector(arm2Vector, cos2, sin2);
    }

    finalColor = min(finalColor, half3(1.0));
    return half4(finalColor, 1.0);
}
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AGSLSampleScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(shaderCode) }
        var time by remember { mutableFloatStateOf(0f) }
        var params by remember {
            mutableStateOf(
                SpirographParams(
                    l1 = DEFAULT_L1,
                    l2 = DEFAULT_L2,
                    s1 = DEFAULT_S1,
                    s2 = DEFAULT_S2,
                    baseHue = DEFAULT_HUE
                )
            )
        }

        val scaffoldState = rememberBottomSheetScaffoldState()
        val scope = rememberCoroutineScope()
        val isSheetExpanded by remember {
            derivedStateOf { scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded }
        }

        LaunchedEffect(Unit) {
            var start = 0L
            while (true) {
                withFrameNanos { now ->
                    if (start == 0L) start = now
                    time = (now - start) / 4_000_000_000f
                }
            }
        }

        BottomSheetScaffold(
            modifier = Modifier.background(Black900),
            scaffoldState = scaffoldState,
            sheetPeekHeight = WindowInsets.safeDrawing.asPaddingValues()
                .calculateBottomPadding() + 56.dp,
            sheetContainerColor = Color.Transparent,
            sheetDragHandle = {},
            sheetContent = {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .animateContentSize()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                params = params.copy(
                                    l1 = Random.nextFloat() * (RANDOM_L_MAX - RANDOM_L_MIN) + RANDOM_L_MIN,
                                    l2 = Random.nextFloat() * (RANDOM_L_MAX - RANDOM_L_MIN) + RANDOM_L_MIN,
                                    s1 = Random.nextInt(RANDOM_S1_MIN, RANDOM_S1_MAX).toFloat(),
                                    s2 = Random.nextInt(RANDOM_S2_MIN, RANDOM_S2_MAX).toFloat()
                                )
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Black700),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Animation,
                                contentDescription = "Randomize",
                                tint = White800
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch { if (!isSheetExpanded) scaffoldState.bottomSheetState.expand() else scaffoldState.bottomSheetState.partialExpand() }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Black700),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            AnimatedContent(
                                targetState = isSheetExpanded,
                                label = "settings_icon_animation"
                            ) { expanded ->
                                Icon(
                                    imageVector = if (!expanded) Icons.Default.Settings else Icons.Default.Close,
                                    contentDescription = if (!expanded) "Settings" else "Close",
                                    tint = White800
                                )
                            }
                        }
                    }

                    ParameterControls(
                        params = params,
                        onParamsChange = { params = it }
                    )
                }

            },
            containerColor = Black900
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val squareSize = minOf(maxWidth, maxHeight)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Canvas(
                        modifier = Modifier.size(squareSize)
                    ) {
                        shader.setFloatUniform("resolution", size.width, size.height)
                        shader.setFloatUniform("time", time)
                        shader.setFloatUniform("baseHue", params.baseHue)
                        shader.setFloatUniform("L1", params.l1)
                        shader.setFloatUniform("L2", params.l2)
                        shader.setFloatUniform("S1", params.s1)
                        shader.setFloatUniform("S2", params.s2)
                        drawRect(brush = ShaderBrush(shader))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black900),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier
                    .padding(20.dp),
                text = "This sample is available on\nAndroid 13 and above.",
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}