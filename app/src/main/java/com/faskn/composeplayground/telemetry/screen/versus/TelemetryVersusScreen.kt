package com.faskn.composeplayground.telemetry.screen.versus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faskn.composeplayground.R
import com.faskn.composeplayground.telemetry.RacingTheme
import com.faskn.composeplayground.telemetry.TelemetryConstants.VERSUS_TRANSITION_DELAY_MS
import com.faskn.composeplayground.telemetry.data.VersusUiState
import com.faskn.composeplayground.telemetry.racingColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val dampedBounce = keyframes {
    durationMillis = 1000
    0f at 0
    80f at 100
    0f at 200
    60f at 320
    0f at 440
    40f at 560
    0f at 660
    30f at 760
    0f at 840
    20f at 920
    0f at 1000
}

@Composable
private fun Float.toFastestColor(other: Float): Color {
    val colors = racingColors
    return when {
        this <= 0f || other <= 0f -> colors.neutral
        this < other -> colors.fastest
        this > other -> colors.slowest
        else -> colors.neutral
    }
}

@Composable
fun TelemetryVersusScreen(
    padding: PaddingValues,
    onNavigateToAnalyse: () -> Unit,
    viewModel: TelemetryVersusViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.startValidation() }

    LaunchedEffect(viewModel.uiState) {
        if (viewModel.uiState is VersusUiState.Success) {
            delay(VERSUS_TRANSITION_DELAY_MS)
            onNavigateToAnalyse()
        }
    }

    RacingTheme {
        TelemetryVersusContent(uiState = viewModel.uiState, padding = padding)
    }
}

@Composable
fun TelemetryVersusContent(
    uiState: VersusUiState,
    padding: PaddingValues,
    skipAnimation: Boolean = false
) {
    val colors = racingColors
    var showTrackName by remember { mutableStateOf(skipAnimation) }
    var showVs by remember { mutableStateOf(skipAnimation) }
    var showLeftDriver by remember { mutableStateOf(skipAnimation) }
    var showRightDriver by remember { mutableStateOf(skipAnimation) }

    val leftCarX = remember { Animatable(if (skipAnimation) 0f else -1400f) }
    val rightCarX = remember { Animatable(if (skipAnimation) 0f else 1400f) }
    val leftCarY = remember { Animatable(0f) }
    val rightCarY = remember { Animatable(0f) }
    val vsAlpha by animateFloatAsState(if (showVs) 1f else 0f, tween(1200), label = "vs")

    LaunchedEffect(uiState) {
        if (uiState !is VersusUiState.Success || skipAnimation) return@LaunchedEffect

        playIntroAnimation(
            onShowTrackName = { showTrackName = it },
            onShowLeftDriver = { showLeftDriver = it },
            onShowVs = { showVs = it },
            onShowRightDriver = { showRightDriver = it },
            leftCarX = leftCarX,
            leftCarY = leftCarY,
            rightCarX = rightCarX,
            rightCarY = rightCarY
        )
    }

    val containerSize = LocalWindowInfo.current.containerSize
    val bgAlpha by animateFloatAsState(if (showTrackName) 1f else 0f, tween(1400), label = "bg")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                alpha = bgAlpha,
                brush = Brush.radialGradient(
                    colors = listOf(colors.transparentGrey, colors.black),
                    center = Offset(containerSize.width / 2f, 0f),
                    radius = containerSize.height / 1.33f
                )
            )
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            VersusUiState.Idle, VersusUiState.Validating -> CircularProgressIndicator(color = colors.slowest)

            is VersusUiState.Error -> Text(
                text = uiState.message,
                color = colors.slowest,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            is VersusUiState.Success -> {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(Modifier.fillMaxHeight(0.125f))

                    AnimatedVisibility(showTrackName, enter = fadeIn(tween(500))) {

                        Text(
                            text = uiState.trackName,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = colors.neutral,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        )
                    }

                    Spacer(Modifier.fillMaxHeight(0.25f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CarColumn(
                            modifier = Modifier.weight(1f),
                            showLogo = showLeftDriver,
                            carImage = R.drawable.porsche_grello_h,
                            carModifier = Modifier.graphicsLayer {
                                translationX = leftCarX.value
                                translationY = leftCarY.value
                            }
                        )

                        Text(
                            text = "VS.",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.W100,
                                fontSize = 20.sp,
                                fontStyle = FontStyle.Italic
                            ),
                            color = colors.neutral,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .graphicsLayer { alpha = vsAlpha }
                        )

                        CarColumn(
                            modifier = Modifier.weight(1f),
                            showLogo = showRightDriver,
                            carImage = R.drawable.porsche_roxy_h,
                            carModifier = Modifier.graphicsLayer {
                                scaleX = -1f
                                translationX = rightCarX.value
                                translationY = rightCarY.value
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth()) {
                        DriverInfo(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            visible = showLeftDriver,
                            name = uiState.leftDriverName,
                            lapTime = uiState.leftBestLapTime,
                            lapColor = uiState.leftBestLapRaw.toFastestColor(uiState.rightBestLapRaw)
                        )
                        DriverInfo(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            visible = showRightDriver,
                            name = uiState.rightDriverName,
                            lapTime = uiState.rightBestLapTime,
                            lapColor = uiState.rightBestLapRaw.toFastestColor(uiState.leftBestLapRaw)
                        )
                    }

                    Spacer(Modifier.fillMaxHeight(0.33f))
                }
            }
        }
    }
}

@Composable
private fun CarColumn(
    modifier: Modifier = Modifier,
    showLogo: Boolean,
    carImage: Int,
    carModifier: Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(showLogo, enter = fadeIn(tween(1000, delayMillis = 350))) {
            Image(
                painter = painterResource(R.drawable.ic_porsche_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(4400f / 300f)
                    .padding(bottom = 8.dp)
            )
        }
        Image(
            painter = painterResource(carImage),
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1000f / 400f)
                .then(carModifier)
        )
    }
}

@Composable
private fun DriverInfo(
    modifier: Modifier = Modifier,
    visible: Boolean,
    name: String,
    lapTime: String,
    lapColor: Color
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible, enter = fadeIn(tween(500, delayMillis = 1000))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = racingColors.neutral
                )

                Text(
                    lapTime,
                    style = MaterialTheme.typography.titleSmall,
                    color = lapColor,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Preview
@Composable
fun TelemetryVersusPreview() {
    RacingTheme {
        TelemetryVersusContent(
            uiState = VersusUiState.Success(
                trackName = "SPA-FRANCORCHAMPS",
                leftDriverName = "Furkan",
                rightDriverName = "Rival",
                leftBestLapTime = "2:18.452",
                rightBestLapTime = "2:19.123",
                leftBestLapRaw = 138.452f,
                rightBestLapRaw = 139.123f
            ),
            padding = PaddingValues.Zero,
            skipAnimation = true
        )
    }
}

private suspend fun playIntroAnimation(
    onShowTrackName: (Boolean) -> Unit,
    onShowLeftDriver: (Boolean) -> Unit,
    onShowVs: (Boolean) -> Unit,
    onShowRightDriver: (Boolean) -> Unit,
    leftCarX: Animatable<Float, *>,
    leftCarY: Animatable<Float, *>,
    rightCarX: Animatable<Float, *>,
    rightCarY: Animatable<Float, *>
) = coroutineScope {
    delay(250); onShowTrackName(true)
    delay(250); onShowLeftDriver(true)

    launch { leftCarX.animateTo(0f, tween(1000, easing = FastOutSlowInEasing)) }
    launch { leftCarY.animateTo(0f, dampedBounce) }

    delay(700)
    onShowVs(true)
    delay(350)
    onShowRightDriver(true)

    launch { rightCarX.animateTo(0f, tween(1000, easing = FastOutSlowInEasing)) }
    launch { rightCarY.animateTo(0f, dampedBounce) }
}
