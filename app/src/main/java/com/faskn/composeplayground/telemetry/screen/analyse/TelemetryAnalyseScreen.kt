package com.faskn.composeplayground.telemetry.screen.analyse

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faskn.composeplayground.R
import com.faskn.composeplayground.telemetry.FONT_FEATURE_TNUM
import com.faskn.composeplayground.telemetry.RacingTheme
import com.faskn.composeplayground.telemetry.TelemetryConstants.FRAME_INTERVAL_NANOS
import com.faskn.composeplayground.telemetry.TelemetryConstants.TRACK_MAP_HEIGHT_FRACTION
import com.faskn.composeplayground.telemetry.components.CarAnalyseColumn
import com.faskn.composeplayground.telemetry.components.DeltaDisplay
import com.faskn.composeplayground.telemetry.components.RaceEngineerBubble
import com.faskn.composeplayground.telemetry.components.SectorAnalysisOverlay
import com.faskn.composeplayground.telemetry.components.TelemetryComparisonRow
import com.faskn.composeplayground.telemetry.components.TelemetryControls
import com.faskn.composeplayground.telemetry.components.TrackMap
import com.faskn.composeplayground.telemetry.data.LapSectorData
import com.faskn.composeplayground.telemetry.data.ReplayMarker
import com.faskn.composeplayground.telemetry.data.SectorInfo
import com.faskn.composeplayground.telemetry.data.TelemetryFrame
import com.faskn.composeplayground.telemetry.data.TelemetryUiState
import com.faskn.composeplayground.telemetry.data.TrackMarker
import com.faskn.composeplayground.telemetry.racingColors
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryAnalyseScreen(
    viewModel: TelemetryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isPlaying) {
        if (!uiState.isPlaying) return@LaunchedEffect
        var lastTime = 0L
        while (viewModel.uiState.value.isPlaying) {
            withFrameNanos { now ->
                if (lastTime == 0L) {
                    lastTime = now
                } else {
                    val deltaNanos = now - lastTime
                    lastTime = now
                    viewModel.advanceIndex(deltaNanos.toFloat() / FRAME_INTERVAL_NANOS)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.togglePlayback()
    }

    RacingTheme {
        val userCurrentFrame = remember(uiState.currentProgress, uiState.userTelemetry) {
            viewModel.getInterpolatedFrame(isUser = true)
        }
        val rivalCurrentFrame = remember(uiState.currentProgress, uiState.rivalBestLapTelemetry) {
            viewModel.getInterpolatedFrame(isUser = false)
        }
        val currentLap = userCurrentFrame?.lap ?: -1

        val userLapPath = remember(uiState.currentProgress, currentLap, uiState.userTelemetry) {
            val startIdx = uiState.lapStartIndices[currentLap] ?: return@remember emptyList()
            val endIdx =
                (uiState.currentProgress.toInt() + 1).coerceAtMost(uiState.userTelemetry.size)
            if (endIdx <= startIdx) emptyList() else uiState.userTelemetry.subList(
                startIdx,
                endIdx
            )
        }

        val rivalLapPath = remember(rivalCurrentFrame, uiState.rivalBestLapTelemetry) {
            val lapData = uiState.rivalBestLapTelemetry
            if (rivalCurrentFrame == null || lapData.isEmpty()) return@remember emptyList()
            val targetTime = rivalCurrentFrame.timeSeconds
            var endIdx = lapData.binarySearch { it.timeSeconds.compareTo(targetTime) }
            if (endIdx < 0) endIdx = -(endIdx + 1)
            endIdx = (endIdx + 1).coerceAtMost(lapData.size)
            lapData.subList(0, endIdx)
        }

        TelemetryAnalyseContent(
            uiState = uiState,
            totalFrames = uiState.userTelemetry.size,
            totalSessionTime = uiState.userTelemetry.lastOrNull()?.timeSeconds ?: 0f,
            userLapPath = userLapPath,
            rivalLapPath = rivalLapPath,
            userCurrentFrame = userCurrentFrame,
            rivalCurrentFrame = rivalCurrentFrame,
            userSteeringRange = uiState.userSteeringRange,
            rivalSteeringRange = uiState.rivalSteeringRange,
            onTogglePlayback = { viewModel.togglePlayback() },
            onLapSelected = { viewModel.selectLap(it) },
            onSeek = { viewModel.updateIndex(it) },
            onToggleConfig = { viewModel.toggleConfig() },
            onSendMessage = { viewModel.sendChatMessage(it) },
            onMarkerClick = { marker ->
                if (viewModel.uiState.value.isPlaying) {
                    viewModel.togglePlayback()
                }
                viewModel.updateIndex(marker.frame.toFloat())
            }
        )
    }
}

@Composable
fun TelemetryAnalyseContent(
    uiState: TelemetryUiState,
    totalFrames: Int,
    totalSessionTime: Float,
    userLapPath: List<TelemetryFrame>,
    rivalLapPath: List<TelemetryFrame>,
    userCurrentFrame: TelemetryFrame?,
    rivalCurrentFrame: TelemetryFrame?,
    userSteeringRange: Pair<Float, Float>,
    rivalSteeringRange: Pair<Float, Float>,
    onTogglePlayback: () -> Unit,
    onLapSelected: (Int) -> Unit,
    onSeek: (Float) -> Unit,
    onToggleConfig: () -> Unit,
    onSendMessage: (String) -> Unit,
    onMarkerClick: (ReplayMarker) -> Unit = {}
) {
    val colors = racingColors
    Scaffold(
        containerColor = colors.black,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colors.black)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (uiState.isLoading) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.slowest)
                    }
                } else {
                    if (uiState.trackPoints.isNotEmpty()) {
                        Box {
                            TrackMap(
                                points = uiState.trackPoints,
                                markers = uiState.trackMarkers,
                                trackWidthNormalized = uiState.trackWidthNormalized,
                                userLapPath = userLapPath,
                                rivalLapPath = rivalLapPath,
                                userCurrentPos = userCurrentFrame?.position,
                                rivalCurrentPos = rivalCurrentFrame?.position,
                                availableLaps = uiState.availableLaps,
                                selectedLap = uiState.selectedLap,
                                onLapSelected = onLapSelected,
                                lapStatus = uiState.lapStatus,
                                userDistance = userCurrentFrame?.lapDistance,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(TRACK_MAP_HEIGHT_FRACTION)
                            )
                            SectorAnalysisOverlay(
                                sectorData = uiState.currentLiveSectors,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .zIndex(2f)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalDivider(
                            color = racingColors.divider,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.userDriverName,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = FONT_FEATURE_TNUM
                                ),
                                color = racingColors.userDriver,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.33f)
                            )
                            DeltaDisplay(
                                modifier = Modifier.weight(0.33f),
                                delta = uiState.currentDelta,
                                currentLapTime = uiState.currentLapTime,
                                isOutLap = uiState.selectedLap == 0,
                                lapCompletion = uiState.lapCompletion
                            )
                            Text(
                                text = "RIVAL'S BEST",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = FONT_FEATURE_TNUM
                                ),
                                color = racingColors.rivalDriver,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.33f)
                            )
                        }

                        HorizontalDivider(color = racingColors.divider)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                "Best: ${uiState.bestLapTime} / Last: ${uiState.lastLapTime}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = colors.neutral,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {


                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CarAnalyseColumn(
                                        modifier = Modifier.weight(1f),
                                        carImage = R.drawable.porsche_grello_v,
                                        throttle = userCurrentFrame?.throttle ?: 0f,
                                        brake = userCurrentFrame?.brake ?: 0f,
                                        gear = userCurrentFrame?.gear ?: 0,
                                        speed = userCurrentFrame?.speed ?: 0f,
                                        config = uiState.userConfig,
                                        showConfig = uiState.showConfig,
                                        onConfigToggle = onToggleConfig,
                                        isLeft = true
                                    )

                                    Box(
                                        Modifier.weight(0.1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            Modifier
                                                .width(1.dp)
                                                .height(160.dp)
                                                .background(racingColors.divider)
                                        )
                                    }

                                    CarAnalyseColumn(
                                        modifier = Modifier.weight(1f),
                                        carImage = R.drawable.porsche_roxy_v,
                                        throttle = rivalCurrentFrame?.throttle ?: 0f,
                                        brake = rivalCurrentFrame?.brake ?: 0f,
                                        gear = rivalCurrentFrame?.gear ?: 0,
                                        speed = rivalCurrentFrame?.speed ?: 0f,
                                        config = uiState.rivalConfig,
                                        showConfig = uiState.showConfig,
                                        onConfigToggle = onToggleConfig,
                                        isLeft = false
                                    )
                                }

                                TelemetryComparisonRow(
                                    userSteeringSmoothness = userCurrentFrame?.steeringSmoothness
                                        ?: 100f,
                                    userTrailBraking = userCurrentFrame?.trailBrakingScore ?: 100f,
                                    userSteeringAngle = userCurrentFrame?.steering ?: 0f,
                                    rivalSteeringSmoothness = rivalCurrentFrame?.steeringSmoothness
                                        ?: 100f,
                                    rivalTrailBraking = rivalCurrentFrame?.trailBrakingScore
                                        ?: 100f,
                                    rivalSteeringAngle = rivalCurrentFrame?.steering ?: 0f,
                                    userSteeringRange = userSteeringRange,
                                    rivalSteeringRange = rivalSteeringRange
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        TelemetryControls(
                            isPlaying = uiState.isPlaying,
                            currentProgress = uiState.currentProgress,
                            totalFrames = totalFrames,
                            currentTimeSeconds = userCurrentFrame?.timeSeconds ?: 0f,
                            totalSessionTimeSeconds = totalSessionTime,
                            lapStartIndices = uiState.lapStartIndices,
                            onTogglePlayback = onTogglePlayback,
                            onSeek = onSeek
                        )
                    }
                }
            }

            RaceEngineerBubble(
                messages = uiState.chatMessages,
                isLoading = uiState.isChatLoading,
                onSendMessage = onSendMessage,
                onMarkerClick = onMarkerClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 4.dp, end = 4.dp, bottom = 90.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TelemetryAnalyseScreenPreview() {
    RacingTheme {
        TelemetryAnalyseContent(
            uiState = TelemetryUiState(
                isPlaying = false, isLoading = false, currentProgress = 25.5f,
                selectedLap = 1, availableLaps = persistentListOf(0, 1, 2, 3),
                bestLapTime = "02:22:871", lastLapTime = "02:23:984", lapStatus = "Timed Lap",
                currentDelta = -0.055f, currentLapTime = "01:12:345", trackWidthNormalized = 0.05f,
                userDriverName = "FURKAN ASKIN",
                currentLiveSectors = LapSectorData(
                    SectorInfo(39.068f, 1, -0.123f),
                    SectorInfo(64.622f, 2, 0.456f),
                    SectorInfo(40.294f, 3, -0.050f)
                ),
                trackPoints = persistentListOf(
                    Offset(0.1f, 0.1f),
                    Offset(0.9f, 0.1f),
                    Offset(0.9f, 0.9f),
                    Offset(0.1f, 0.9f)
                ),
                trackMarkers = persistentListOf(
                    TrackMarker("S/F", Offset(0.1f, 0.1f), 0f, Offset(0f, 1f)),
                    TrackMarker("S1", Offset(0.9f, 0.1f), 1.57f, Offset(-1f, 0f)),
                    TrackMarker("S2", Offset(0.9f, 0.9f), 3.14f, Offset(0f, -1f))
                )
            ),
            totalFrames = 100,
            totalSessionTime = 250f,
            userLapPath = emptyList(), rivalLapPath = emptyList(),
            userCurrentFrame = TelemetryFrame(
                throttle = 80f,
                brake = 20f,
                steering = 90f,
                gear = 1,
                speed = 180f,
                lap = 1,
                timeSeconds = 12.345f,
                lapDistance = 0.0f
            ),
            rivalCurrentFrame = TelemetryFrame(
                throttle = 70f,
                brake = 10f,
                steering = 85f,
                gear = 2,
                speed = 185f,
                lap = 1,
                timeSeconds = 12.400f,
                lapDistance = 0.0f
            ),
            userSteeringRange = -90f to 90f,
            rivalSteeringRange = -90f to 90f,
            onTogglePlayback = {}, onLapSelected = {}, onSeek = {}, onToggleConfig = {},
            onSendMessage = {}
        )
    }
}
