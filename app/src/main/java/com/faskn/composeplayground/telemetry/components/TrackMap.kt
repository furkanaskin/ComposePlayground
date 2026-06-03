package com.faskn.composeplayground.telemetry.components

import android.graphics.BlurMaskFilter
import android.graphics.CornerPathEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.faskn.composeplayground.telemetry.RacingColors
import com.faskn.composeplayground.telemetry.TelemetryConstants.DEFAULT_TRACK_SCALE
import com.faskn.composeplayground.telemetry.TelemetryConstants.DRIVER_INDICATOR_BORDER
import com.faskn.composeplayground.telemetry.TelemetryConstants.DRIVER_INDICATOR_INNER_SCALE
import com.faskn.composeplayground.telemetry.TelemetryConstants.DRIVER_INDICATOR_RADIUS
import com.faskn.composeplayground.telemetry.TelemetryConstants.DRIVER_PATH_STROKE_WIDTH
import com.faskn.composeplayground.telemetry.TelemetryConstants.MARKER_LINE_WIDTH_SCALE
import com.faskn.composeplayground.telemetry.TelemetryConstants.MAX_TRACK_SCALE
import com.faskn.composeplayground.telemetry.TelemetryConstants.TRACK_ANIMATION_DURATION_MS
import com.faskn.composeplayground.telemetry.TelemetryConstants.TRACK_ASPHALT_STROKE_SCALE
import com.faskn.composeplayground.telemetry.TelemetryConstants.TRACK_EDGE_STROKE_SCALE
import com.faskn.composeplayground.telemetry.TelemetryConstants.TRACK_GLOW_BLUR_SCALE
import com.faskn.composeplayground.telemetry.TelemetryConstants.TRACK_GLOW_STROKE_SCALE
import com.faskn.composeplayground.telemetry.TelemetryConstants.TRACK_SCRATCH_STROKE_SCALE
import com.faskn.composeplayground.telemetry.TelemetryUtils.format
import com.faskn.composeplayground.telemetry.data.TelemetryFrame
import com.faskn.composeplayground.telemetry.data.TrackMarker
import com.faskn.composeplayground.telemetry.racingColors
import kotlin.math.sqrt

private class TelemetryPathSet {
    val brake = Path()
    val throttle = Path()
    val idle = Path()
}

data class PathProcessingState(
    val lastProcessedUserSize: Int = 0,
    val lastProcessedRivalSize: Int = 0,
    val lastProcessedLap: Int = -1
)

private fun calculateOrigin(size: Size): Offset {
    if (size == Size.Zero) return Offset.Zero
    val drawSize = minOf(size.width, size.height)
    return Offset(
        x = (size.width - drawSize) / 2f,
        y = (size.height - drawSize) / 2f
    )
}

private fun appendToPath(
    frames: List<TelemetryFrame>,
    lastSize: Int,
    pathSet: TelemetryPathSet,
    ox: Float,
    oy: Float,
    drawSize: Float
): Int {
    if (frames.size < 2) return frames.size
    for (i in maxOf(0, lastSize - 1) until frames.size - 1) {
        val p1 = frames[i].position ?: continue
        val p2 = frames[i + 1].position ?: continue
        val target = when {
            frames[i].brake > 0f -> pathSet.brake
            frames[i].throttle > 0f -> pathSet.throttle
            else -> pathSet.idle
        }
        target.moveTo(ox + p1.x * drawSize, oy + p1.y * drawSize)
        target.lineTo(ox + p2.x * drawSize, oy + p2.y * drawSize)
    }
    return frames.size
}

@Composable
fun TrackMap(
    points: List<Offset>,
    markers: List<TrackMarker>,
    trackWidthNormalized: Float,
    userLapPath: List<TelemetryFrame>,
    rivalLapPath: List<TelemetryFrame>,
    userCurrentPos: Offset?,
    rivalCurrentPos: Offset?,
    availableLaps: List<Int>,
    selectedLap: Int,
    onLapSelected: (Int) -> Unit,
    lapStatus: String,
    modifier: Modifier = Modifier,
    userDistance: Float? = null,
) {
    var rawScale by remember { mutableFloatStateOf(DEFAULT_TRACK_SCALE) }
    val scale by animateFloatAsState(
        targetValue = rawScale,
        animationSpec = tween(300),
        label = "TrackScale"
    )
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var followMode by remember { mutableStateOf(true) }
    val drawProgress = remember { Animatable(0f) }
    val manualPanOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    var userPaths by remember { mutableStateOf(TelemetryPathSet()) }
    var rivalPaths by remember { mutableStateOf(TelemetryPathSet()) }
    var pathProcessingState by remember { mutableStateOf(PathProcessingState()) }

    val colors = racingColors

    val textPaintColor = colors.white.toArgb()
    val shadowColor = colors.black.toArgb()
    val textPaint = remember(textPaintColor) {
        android.graphics.Paint().apply {
            color = textPaintColor
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            letterSpacing = 0.3f
        }
    }

    LaunchedEffect(userLapPath, rivalLapPath, canvasSize) {
        if (canvasSize == Size.Zero) return@LaunchedEffect
        val drawSize = minOf(canvasSize.width, canvasSize.height)
        val (ox, oy) = calculateOrigin(canvasSize)
        val currentLap = userLapPath.firstOrNull()?.lap ?: -1
        val isSeekBack = userLapPath.size < pathProcessingState.lastProcessedUserSize ||
                rivalLapPath.size < pathProcessingState.lastProcessedRivalSize

        val (newUserPaths, userLastSize) = if (currentLap != pathProcessingState.lastProcessedLap || isSeekBack) {
            TelemetryPathSet() to 0
        } else {
            TelemetryPathSet().apply {
                brake.addPath(userPaths.brake)
                throttle.addPath(userPaths.throttle)
                idle.addPath(userPaths.idle)
            } to pathProcessingState.lastProcessedUserSize
        }

        val (newRivalPaths, rivalLastSize) = if (currentLap != pathProcessingState.lastProcessedLap || isSeekBack) {
            TelemetryPathSet() to 0
        } else {
            TelemetryPathSet().apply {
                brake.addPath(rivalPaths.brake)
                throttle.addPath(rivalPaths.throttle)
                idle.addPath(rivalPaths.idle)
            } to pathProcessingState.lastProcessedRivalSize
        }

        val newProcessedUserSize = appendToPath(
            frames = userLapPath,
            lastSize = userLastSize,
            pathSet = newUserPaths,
            ox = ox,
            oy = oy,
            drawSize = drawSize
        )
        val newProcessedRivalSize = appendToPath(
            frames = rivalLapPath,
            lastSize = rivalLastSize,
            pathSet = newRivalPaths,
            ox = ox,
            oy = oy,
            drawSize = drawSize
        )

        userPaths = newUserPaths
        rivalPaths = newRivalPaths
        pathProcessingState = PathProcessingState(
            lastProcessedUserSize = newProcessedUserSize,
            lastProcessedRivalSize = newProcessedRivalSize,
            lastProcessedLap = currentLap
        )
    }

    LaunchedEffect(points) {
        if (points.isNotEmpty()) {
            drawProgress.snapTo(0f)
            drawProgress.animateTo(1f, tween(TRACK_ANIMATION_DURATION_MS, easing = EaseInOutCubic))
        }
    }

    val followPanOffset = remember(userCurrentPos, canvasSize, scale) {
        if (userCurrentPos == null || canvasSize == Size.Zero) {
            Offset.Zero
        } else {
            val drawSize = minOf(canvasSize.width, canvasSize.height)
            val (ox, oy) = calculateOrigin(canvasSize)
            Offset(
                x = -scale * (ox + userCurrentPos.x * drawSize - canvasSize.width / 2f),
                y = -scale * (oy + userCurrentPos.y * drawSize - canvasSize.height / 2f)
            )
        }
    }

    LaunchedEffect(followMode) {
        if (followMode) {
            manualPanOffset.animateTo(followPanOffset, tween(300, easing = EaseInOutCubic))
        }
    }

    val effectivePanOffset = if (followMode) followPanOffset else manualPanOffset.value

    val fullPath = remember(points, canvasSize) {
        buildPathFromOffsets(points, canvasSize)
    }

    val glowPaint = remember(colors, canvasSize, trackWidthNormalized) {
        if (canvasSize == Size.Zero) return@remember null
        val drawSize = minOf(canvasSize.width, canvasSize.height)
        val strokePx = (trackWidthNormalized * drawSize).coerceAtLeast(1f)
        android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = strokePx * TRACK_GLOW_STROKE_SCALE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            color = colors.trackGlow.toArgb()
            maskFilter =
                BlurMaskFilter(strokePx * TRACK_GLOW_BLUR_SCALE, BlurMaskFilter.Blur.NORMAL)
            pathEffect = CornerPathEffect(strokePx)
        }
    }

    Box(
        modifier = modifier.clipToBounds()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it.toSize() }
                .graphicsLayer {
                    translationX = effectivePanOffset.x
                    translationY = effectivePanOffset.y
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            if (points.isEmpty()) return@Canvas

            val drawSize = minOf(size.width, size.height)
            val (ox, oy) = calculateOrigin(size)
            val strokePx = (trackWidthNormalized * drawSize).coerceAtLeast(1.dp.toPx())
            val progress = drawProgress.value

            // ── 1. Track ────────────────────────────────────────────────────
            val trackPath = if (progress >= 1f) fullPath else {
                val count = (points.size * progress).toInt().coerceIn(2, points.size)
                Path().apply {
                    moveTo(ox + points[0].x * drawSize, oy + points[0].y * drawSize)
                    for (i in 1 until count) lineTo(
                        ox + points[i].x * drawSize,
                        oy + points[i].y * drawSize
                    )
                }
            }

            glowPaint?.let { paint ->
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawPath(trackPath.asAndroidPath(), paint)
                }
            }

            listOf(
                Triple(
                    colors.trackEdge,
                    strokePx * TRACK_EDGE_STROKE_SCALE,
                    PathEffect.cornerPathEffect(strokePx)
                ),
                Triple(
                    colors.trackAsphalt,
                    strokePx * TRACK_ASPHALT_STROKE_SCALE,
                    PathEffect.cornerPathEffect(strokePx)
                )
            ).forEach { (color, width, effect) ->
                drawPath(
                    trackPath,
                    color,
                    style = Stroke(
                        width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = effect
                    )
                )
            }
            drawPath(
                trackPath, colors.trackScratch,
                style = Stroke(
                    width = strokePx * TRACK_SCRATCH_STROKE_SCALE,
                    cap = StrokeCap.Butt,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 50f), 0f)
                )
            )

            // ── 2. Markers (Lines & Labels) ─────────────────────────────────
            val sqrtScale = sqrt(scale)
            val textSizePx = 8.sp.toPx() / sqrtScale
            val trackHalfCanvas = strokePx * TRACK_EDGE_STROKE_SCALE / 2f
            val gapCanvas = 6.dp.toPx() / sqrtScale
            val offsetDist = trackHalfCanvas + gapCanvas + textSizePx
            val halfLen = trackWidthNormalized * drawSize * MARKER_LINE_WIDTH_SCALE

            textPaint.apply {
                textSize = textSizePx
                setShadowLayer(4f / sqrtScale, 0f, 0f, shadowColor)
            }

            markers.forEach { marker ->
                val p = marker.position
                val normalX = marker.normal.x
                val normalY = marker.normal.y

                // 2a. Line
                drawLine(
                    color = colors.white,
                    start = Offset(
                        ox + (p.x + normalX * halfLen / drawSize) * drawSize,
                        oy + (p.y + normalY * halfLen / drawSize) * drawSize
                    ),
                    end = Offset(
                        ox + (p.x - normalX * halfLen / drawSize) * drawSize,
                        oy + (p.y - normalY * halfLen / drawSize) * drawSize
                    ),
                    strokeWidth = 3.dp.toPx() / scale,
                    cap = StrokeCap.Round
                )

                // 2b. Label
                val c = Offset(ox + p.x * drawSize, oy + p.y * drawSize)
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        marker.label,
                        c.x + normalX * offsetDist,
                        c.y + normalY * offsetDist,
                        textPaint
                    )
                }
            }

            // ── 3. Telemetry paths + indicators (after animation) ────────────
            if (progress < 1f) return@Canvas

            val telemetryStyle = Stroke(
                width = DRIVER_PATH_STROKE_WIDTH.toPx() / DEFAULT_TRACK_SCALE,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            drawTelemetrySet(userPaths, colors, telemetryStyle, isRival = false)
            drawTelemetrySet(rivalPaths, colors, telemetryStyle, isRival = true)

            val indicatorR = DRIVER_INDICATOR_RADIUS.toPx() / DEFAULT_TRACK_SCALE
            val borderW = DRIVER_INDICATOR_BORDER.toPx() / DEFAULT_TRACK_SCALE

            userCurrentPos?.let { p ->
                val c = Offset(ox + p.x * drawSize, oy + p.y * drawSize)
                drawCircle(colors.userDriver, indicatorR, c)
                drawCircle(colors.black, indicatorR-borderW, c)
                drawCircle(colors.userDriver, indicatorR * DRIVER_INDICATOR_INNER_SCALE, c)
            }
            rivalCurrentPos?.let { p ->
                val c = Offset(ox + p.x * drawSize, oy + p.y * drawSize)
                val rivalBorderW = borderW * 1.5f
                drawCircle(colors.rivalDriver.copy(alpha = 0.85f), indicatorR, c)
                drawCircle(
                    colors.rivalIndicator,
                    indicatorR * DRIVER_INDICATOR_INNER_SCALE,
                    c
                )
                drawCircle(
                    colors.rivalIndicator,
                    indicatorR - rivalBorderW / 2f,
                    c,
                    style = Stroke(
                        rivalBorderW,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(rivalBorderW, rivalBorderW)
                        )
                    )
                )
            }
        }

        // ── UI overlays (not part of canvas transform) ───────────────────────
        Text(
            text = lapStatus,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .zIndex(2f)
        )

        if (selectedLap != 0 && userDistance != null) {
            Text(
                text = "${userDistance.toInt()}M",
                color = colors.white,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum"
                ),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.BottomCenter)
            )
        }

        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(2f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(4.dp))
                LapSelector(availableLaps, selectedLap, onLapSelected)
            }

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val scaleText by remember {
                    derivedStateOf { "${scale.format(1)}x" }
                }
                Text(
                    text = scaleText,
                    color = colors.white.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniActionButton("−") {
                        rawScale = (rawScale / 2f).coerceAtLeast(DEFAULT_TRACK_SCALE)
                    }
                    MiniActionButton("+") {
                        rawScale = (rawScale * 2f).coerceAtMost(MAX_TRACK_SCALE)
                    }
                }
            }
        }
    }
}

private fun buildPathFromOffsets(points: List<Offset>, canvasSize: Size): Path =
    Path().apply {
        if (points.size < 2 || canvasSize == Size.Zero) return@apply
        val drawSize = minOf(canvasSize.width, canvasSize.height)
        val (ox, oy) = calculateOrigin(canvasSize)
        points.forEachIndexed { i, p ->
            val x = ox + p.x * drawSize
            val y = oy + p.y * drawSize
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

private fun DrawScope.drawTelemetrySet(
    pathSet: TelemetryPathSet,
    colors: RacingColors,
    style: Stroke,
    isRival: Boolean
) {
    val mode = if (isRival) BlendMode.Plus else BlendMode.SrcOver
    if (!pathSet.brake.isEmpty) drawPath(
        pathSet.brake,
        colors.telemetryBrake,
        style = style,
        blendMode = mode
    )
    if (!pathSet.throttle.isEmpty) drawPath(
        pathSet.throttle,
        colors.telemetryThrottle,
        style = style,
        blendMode = mode
    )
    if (!pathSet.idle.isEmpty) drawPath(
        pathSet.idle,
        colors.telemetryIdle,
        style = style,
        blendMode = mode
    )
}