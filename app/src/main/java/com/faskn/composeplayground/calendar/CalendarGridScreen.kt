package com.faskn.composeplayground.calendar

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.State
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.faskn.composeplayground.ui.theme.TechBlack
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CalendarGridScreen(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(0.dp)
) {
    val dates = remember { (13..19).toList() }
    val hours = remember { (0..23).toList() }

    val events = remember {
        mutableStateListOf<CalendarEvent>().apply {
            addAll(getMockCalendarEvents())
        }
    }

    var draggingEventId by remember { mutableStateOf<String?>(null) }
    var expandedCell by remember { mutableStateOf<CellIndex?>(null) }

    val onEventMoved: (String, Int, Int) -> Unit = { eventId, newDay, newStartHour ->
        val index = events.indexOfFirst { it.id == eventId }
        if (index != -1) {
            val oldEvent = events.removeAt(index)
            val oldCell = CellIndex(oldEvent.dayOfWeek, oldEvent.startTime)
            val newCell = CellIndex(newDay, newStartHour)

            val duration = oldEvent.endTime - oldEvent.startTime
            val updatedEvent = oldEvent.copy(
                dayOfWeek = newDay,
                startTime = newStartHour,
                endTime = newStartHour + duration
            )
            // Add to the end so it's always visually "on top"
            events.add(updatedEvent)

            // If we moved an event OUT of an expanded cell, collapse it.
            if (expandedCell == oldCell && oldCell != newCell) {
                expandedCell = null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(padding)
    ) {
        CalendarTopBar(month = "September 2026")

        CalendarDayHeaders(
            days = DaysOfWeek,
            dates = dates,
            highlightedIndex = 1
        )

        CalendarGridBody(
            hours = hours,
            events = events,
            daysCount = DaysInWeek,
            onEventMoved = onEventMoved,
            draggingEventId = draggingEventId,
            expandedCell = expandedCell,
            onCellExpanded = { expandedCell = it },
            onDraggingStarted = { draggingEventId = it },
            onDraggingFinished = { draggingEventId = null },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CalendarText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        style = style,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
private fun CalendarTopBar(month: String) {

    CalendarText(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.Black)
            .padding(bottom = 12.dp, start = 20.dp),
        text = month,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White
    )
}

@Composable
private fun CalendarDayHeaders(
    days: List<String>,
    dates: List<Int>,
    highlightedIndex: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(bottom = 8.dp)
    ) {
        Spacer(modifier = Modifier.width(TimeColumnWidth))
        days.forEachIndexed { index, day ->
            val isHighlighted = index == highlightedIndex
            val accentColor = if (isHighlighted) Color(0xFFFF1744) else Color.Gray
            val dateColor = if (isHighlighted) Color(0xFFFF1744) else Color.White

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CalendarText(
                    text = day,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isHighlighted) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CalendarText(
                        text = dates[index].toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = dateColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarGridBody(
    hours: List<Int>,
    events: List<CalendarEvent>,
    daysCount: Int,
    onEventMoved: (String, Int, Int) -> Unit,
    draggingEventId: String?,
    expandedCell: CellIndex?,
    onCellExpanded: (CellIndex?) -> Unit,
    onDraggingStarted: (String) -> Unit,
    onDraggingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        val hourHeightPx = with(density) { CellHeight.toPx() }
        val scrollPos = (CURRENT_TIME_HOUR - 1 + CURRENT_TIME_MINUTE / 60f) * hourHeightPx
        scrollState.scrollTo(scrollPos.roundToInt().coerceAtLeast(0))
    }

    Row(modifier = modifier.fillMaxSize()) {
        CalendarTimeAxis(
            hours = hours,
            scrollState = scrollState
        )

        CalendarGridArea(
            hours = hours,
            events = events,
            daysCount = daysCount,
            scrollState = scrollState,
            onEventMoved = onEventMoved,
            draggingEventId = draggingEventId,
            expandedCell = expandedCell,
            onCellExpanded = onCellExpanded,
            onDraggingStarted = onDraggingStarted,
            onDraggingFinished = onDraggingFinished
        )
    }
}

@Composable
private fun CalendarTimeAxis(
    hours: List<Int>,
    scrollState: ScrollState
) {
    Box(
        modifier = Modifier
            .width(TimeColumnWidth)
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Column {
            hours.forEach { hour ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CellHeight),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CalendarText(
                        text = formatHour(hour),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        val currentTimePos = CellHeight * (CURRENT_TIME_HOUR + CURRENT_TIME_MINUTE / 60f)
        Box(
            modifier = Modifier
                .offset(y = currentTimePos - 12.dp)
                .padding(horizontal = 4.dp)
                .background(
                    Color(0xFFFF1744),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.TopCenter)
        ) {
            CalendarText(
                text = String.format(
                    java.util.Locale.US,
                    "%02d:%02d",
                    CURRENT_TIME_HOUR,
                    CURRENT_TIME_MINUTE
                ),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun CalendarGridArea(
    hours: List<Int>,
    events: List<CalendarEvent>,
    daysCount: Int,
    scrollState: ScrollState,
    onEventMoved: (String, Int, Int) -> Unit,
    draggingEventId: String?,
    expandedCell: CellIndex?,
    onCellExpanded: (CellIndex?) -> Unit,
    onDraggingStarted: (String) -> Unit,
    onDraggingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer { clip = true }
            .fillMaxSize()) {

        val totalHeight = CellHeight * hours.size
        val dayWidth = maxWidth / daysCount

        // Layer 1: Background and grid lines
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    detectTapGestures { onCellExpanded(null) }
                }
        ) {
            CalendarGridLines(
                hoursCount = hours.size,
                daysCount = daysCount,
                totalHeight = totalHeight
            )

            val currentTimePos = CellHeight * (CURRENT_TIME_HOUR + CURRENT_TIME_MINUTE / 60f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = currentTimePos)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF1744), Color.Transparent)
                        )
                    )
            )
        }

        // Shared "jiggle" clock used by every non-dragged card while any
        // card is being dragged. A single InfiniteTransition for the whole
        // grid - and none at all while nothing is being dragged - instead
        // of every event card running its own infinite animation forever.
        val sharedShakeRotation = rememberSharedShakeRotation(isActive = draggingEventId != null)

        // Layer 2: Events
        val eventsByCell by remember(events) {
            derivedStateOf { events.groupBy { CellIndex(it.dayOfWeek, it.startTime) } }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = -scrollState.value.toFloat() }
        ) {
            events.forEach { event ->
                key(event.id) {
                    val cellIndex = remember(event.dayOfWeek, event.startTime) {
                        CellIndex(event.dayOfWeek, event.startTime)
                    }
                    val cellEvents = eventsByCell[cellIndex] ?: emptyList()
                    val stackIndex = cellEvents.indexOf(event)
                    val isExpanded = expandedCell == cellIndex

                    CalendarEventItem(
                        event = event,
                        dayWidth = dayWidth,
                        cellHeight = CellHeight,
                        daysCount = daysCount,
                        hoursCount = hours.size,
                        onEventMoved = onEventMoved,
                        isAnyItemDragging = draggingEventId != null,
                        isThisItemDragging = draggingEventId == event.id,
                        isExpanded = isExpanded,
                        isAnyItemExpanded = expandedCell != null,
                        stackIndex = stackIndex,
                        stackCount = cellEvents.size,
                        sharedShakeRotation = sharedShakeRotation,
                        onExpandRequested = { onCellExpanded(cellIndex) },
                        onCollapseRequested = { onCellExpanded(null) },
                        onDraggingStarted = {
                            onDraggingStarted(event.id)
                        },
                        onDraggingFinished = onDraggingFinished
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarGridLines(
    hoursCount: Int,
    daysCount: Int,
    totalHeight: Dp
) {
    val lineColor = Color.White.copy(alpha = 0.15f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        val strokeWidthPx = 0.5.dp.toPx()
        val hourHeightPx = size.height / hoursCount
        val dayWidthPx = size.width / daysCount

        for (i in 0..hoursCount) {
            val y = i * hourHeightPx
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidthPx
            )
        }
        for (i in 0..daysCount) {
            val x = i * dayWidthPx
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidthPx
            )
        }
    }
}

/**
 * The "other cards jiggle while one is lifted" clock, shared by the whole
 * grid. While no card is being dragged this doesn't create an
 * InfiniteTransition at all - no animation clock runs, nothing to
 * invalidate, nothing to draw - it just hands back a constant 0f. The
 * moment a drag starts it flips to a single running InfiniteTransition
 * that every sibling card reads from, instead of each of them spinning up
 * (and permanently keeping alive) its own.
 */
@Composable
private fun rememberSharedShakeRotation(isActive: Boolean): State<Float> {
    return if (isActive) {
        val transition = rememberInfiniteTransition(label = "shake")
        transition.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(125, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shakeRotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
}

@Composable
private fun CalendarEventItem(
    event: CalendarEvent,
    dayWidth: Dp,
    cellHeight: Dp,
    daysCount: Int,
    hoursCount: Int,
    onEventMoved: (String, Int, Int) -> Unit,
    isAnyItemDragging: Boolean,
    isThisItemDragging: Boolean,
    isExpanded: Boolean,
    isAnyItemExpanded: Boolean,
    stackIndex: Int,
    stackCount: Int,
    sharedShakeRotation: State<Float>,
    onExpandRequested: () -> Unit,
    onCollapseRequested: () -> Unit,
    onDraggingStarted: () -> Unit,
    onDraggingFinished: () -> Unit
) {
    val duration = event.endTime - event.startTime
    val effectiveDuration = if (stackCount > 1) 1 else duration

    val animatedHeight by animateDpAsState(
        targetValue = cellHeight * effectiveDuration - 4.dp,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
        label = "heightAnim"
    )

    // Kept as State (not destructured with `by`) on purpose: these feed
    // calendarEventVisuals, which reads them inside a graphicsLayer block.
    // That keeps every animation tick from recomposing this whole card -
    // see the comment in CalendarItemModifier.kt.
    val scaleAnim = animateFloatAsState(
        targetValue = if (isThisItemDragging) 1.08f else 1f,
        label = "scaleAnim"
    )
    val elevationAnim = animateDpAsState(
        targetValue = if (isThisItemDragging) 16.dp else 2.dp,
        label = "elevationAnim"
    )

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Only the top 2 cards peek out when collapsed; anything older sits fully
    // hidden underneath. Computed once per composition, reused both for the
    // initial Animatable value and inside the animation effect below.
    val visibleCollapsedIndex = stackIndex - (stackCount - 2).coerceAtLeast(0)
    val isHiddenWhenCollapsed = stackCount > 2 && visibleCollapsedIndex < 0
    // A hidden, collapsed card shouldn't be able to steal taps/drags meant for
    // the visible peek stack.
    val isInteractable = !(isHiddenWhenCollapsed && !isExpanded)

    val dragOffsetState = remember(event.id) { mutableStateOf(Offset.Zero) }
    var dragOffset by dragOffsetState
    val tiltAngleState = remember { mutableFloatStateOf(0f) }
    var tiltAngle by tiltAngleState

    val potentialDay by remember(event.id, daysCount) {
        derivedStateOf {
            val dayWidthPx = with(density) { dayWidth.toPx() }
            val currentX = dayWidthPx * event.dayOfWeek + dragOffset.x
            (currentX / dayWidthPx).roundToInt().coerceIn(0, daysCount - 1)
        }
    }

    val potentialStartHour by remember(event.id, hoursCount, duration) {
        derivedStateOf {
            val cellHeightPx = with(density) { cellHeight.toPx() }
            val currentY = cellHeightPx * event.startTime + dragOffset.y
            (currentY / cellHeightPx).roundToInt().coerceIn(0, 23)
        }
    }

    val settleAnim = remember(event.id) {
        Animatable(Offset.Zero, Offset.VectorConverter)
    }

    val stackOffsetX = remember(event.id) { Animatable(0f) }
    val stackOffsetY = remember(event.id) { Animatable(0f) }
    val stackRot = remember(event.id) { Animatable(0f) }
    val stackScale = remember(event.id) { Animatable(1f) }
    val stackAlpha = remember(event.id) {
        Animatable(if (isHiddenWhenCollapsed) 0f else 1f)
    }

    LaunchedEffect(isExpanded, stackIndex, stackCount, isThisItemDragging) {
        if (isThisItemDragging) {
            // Seamlessly hand off to the drag gesture - no fan/stack maths
            // while the user is actively holding this card.
            stackOffsetX.snapTo(0f)
            stackOffsetY.snapTo(0f)
            stackRot.snapTo(0f)
            stackScale.snapTo(1f)
            stackAlpha.snapTo(1f)
            return@LaunchedEffect
        }

        val bounceSpec = spring<Float>(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)

        if (stackCount <= 1) {
            launch { stackOffsetX.animateTo(0f, bounceSpec) }
            launch { stackOffsetY.animateTo(0f, bounceSpec) }
            launch { stackRot.animateTo(0f, bounceSpec) }
            launch { stackScale.animateTo(1f, bounceSpec) }
            launch { stackAlpha.animateTo(1f, tween(150)) }
            return@LaunchedEffect
        }

        // Sequential reveal: the fan opens outward starting from the newest
        // (highest-index) card and closes in reverse order, so the whole
        // stack reads as one continuous sweep instead of everyone jumping
        // to place at once.
        val order = if (isExpanded) stackIndex else (stackCount - 1 - stackIndex)
        delay((order * CAROUSEL_STAGGER_STEP_MS).milliseconds)

        val targetX: Float
        val targetY: Float
        val targetRot: Float
        val targetAlpha: Float
        val targetScale: Float

        if (isExpanded) {
            val dayWidthPx = with(density) { dayWidth.toPx() }
            val cellHeightPx = with(density) { cellHeight.toPx() }

            val leftThird = daysCount / 3f
            val rightThird = daysCount - daysCount / 3f
            val horizontalBias = when {
                event.dayOfWeek < leftThird -> 1
                event.dayOfWeek > rightThird -> -1
                else -> 0
            }
            val topQuarter = hoursCount / 4f
            val bottomQuarter = hoursCount - hoursCount / 4f
            val verticalBias = when {
                event.startTime < topQuarter -> 1
                event.startTime > bottomQuarter -> -1
                else -> 0
            }
            val centerAngleDeg = when {
                horizontalBias > 0 && verticalBias > 0 -> 45f
                horizontalBias > 0 && verticalBias < 0 -> -45f
                horizontalBias < 0 && verticalBias > 0 -> 135f
                horizontalBias < 0 && verticalBias < 0 -> -135f
                horizontalBias > 0 -> 0f
                horizontalBias < 0 -> 180f
                verticalBias < 0 -> -90f
                else -> 90f
            }

            val spanDeg = ((stackCount - 1) * CAROUSEL_SPAN_STEP_DEG)
                .coerceIn(CAROUSEL_MIN_SPAN_DEG, CAROUSEL_MAX_SPAN_DEG)
            val angleStepDeg = spanDeg / (stackCount - 1)
            val itemAngleDeg = centerAngleDeg - spanDeg / 2f + angleStepDeg * stackIndex
            val angleRad = Math.toRadians(itemAngleDeg.toDouble())

            val cardDiagonalPx = hypot(dayWidthPx, cellHeightPx)
            val minSpacingPx = cardDiagonalPx * CAROUSEL_CARD_SPACING_FACTOR
            val angleStepRad = Math.toRadians(angleStepDeg.toDouble())
            val neededRadiusPx = minSpacingPx / (2f * sin(angleStepRad / 2f).toFloat())
            val radiusPx = neededRadiusPx.coerceIn(
                with(density) { CAROUSEL_MIN_RADIUS_DP.dp.toPx() },
                with(density) { CAROUSEL_MAX_RADIUS_DP.dp.toPx() }
            )

            val cellCenterX = dayWidthPx / 2f
            val cellCenterY = cellHeightPx / 2f
            val cardCenterX = dayWidthPx / 2f
            val cardCenterY = (cellHeightPx * effectiveDuration) / 2f

            targetX = (cos(angleRad) * radiusPx).toFloat() + (cellCenterX - cardCenterX)
            targetY = (sin(angleRad) * radiusPx).toFloat() + (cellCenterY - cardCenterY)

            val normalizedAngle = itemAngleDeg - centerAngleDeg
            targetRot = (normalizedAngle * 0.18f).coerceIn(-14f, 14f)
            targetAlpha = 1f
            targetScale = 1f
        } else {
            targetX = 0f
            targetY = if (!isHiddenWhenCollapsed) {
                ((2 - 1) - visibleCollapsedIndex) * 6.dp.value * density.density
            } else {
                0f
            }
            targetRot = 0f
            targetAlpha = if (isHiddenWhenCollapsed) 0f else 1f
            val depthFromTop = (1 - visibleCollapsedIndex).coerceIn(0, 1)
            targetScale = 1f - depthFromTop * 0.045f
        }

        launch { stackOffsetX.animateTo(targetX, bounceSpec) }
        launch { stackOffsetY.animateTo(targetY, bounceSpec) }
        launch { stackRot.animateTo(targetRot, bounceSpec) }
        launch { stackAlpha.animateTo(targetAlpha, tween(180)) }
        launch {
            stackScale.snapTo((targetScale - 0.1f).coerceAtLeast(0.5f))
            stackScale.animateTo(targetScale, bounceSpec)
        }
    }

    var settleJob by remember {
        mutableStateOf<kotlinx.coroutines.Job?>(null)
    }

    LaunchedEffect(isThisItemDragging) {
        if (isThisItemDragging) {
            while (isActive) {
                withFrameMillis {
                    tiltAngle *= 0.88f
                    if (abs(tiltAngle) < 0.05f) tiltAngle = 0f
                }
            }
        } else {
            tiltAngle = 0f
        }
    }

    fun animateOffsetBackToZero(from: Offset) {
        settleJob?.cancel()

        settleJob = scope.launch {
            settleAnim.snapTo(from)

            settleAnim.animateTo(
                targetValue = Offset.Zero,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) {
                dragOffset = value
            }

            dragOffset = Offset.Zero
        }
    }

    fun finishDrag(commitMove: Boolean) {
        val currentDragOffset = dragOffset

        if (!commitMove) {
            onDraggingFinished()
            animateOffsetBackToZero(currentDragOffset)
            return
        }

        val dayWidthPx = with(density) { dayWidth.toPx() }
        val cellHeightPx = with(density) { cellHeight.toPx() }

        val currentX = dayWidthPx * event.dayOfWeek + currentDragOffset.x
        val currentY = cellHeightPx * event.startTime + currentDragOffset.y

        val newDay = (currentX / dayWidthPx)
            .roundToInt()
            .coerceIn(0, daysCount - 1)

        val newStartHour = (currentY / cellHeightPx)
            .roundToInt()
            .coerceIn(0, 24 - duration)

        val deltaX = (event.dayOfWeek - newDay) * dayWidthPx
        val deltaY = (event.startTime - newStartHour) * cellHeightPx

        val targetCompensation = Offset(
            x = currentDragOffset.x + deltaX,
            y = currentDragOffset.y + deltaY
        )

        onEventMoved(event.id, newDay, newStartHour)
        onDraggingFinished()

        dragOffset = targetCompensation
        animateOffsetBackToZero(targetCompensation)
    }

    Box(
        modifier = Modifier
            .width(dayWidth)
            .height(animatedHeight)
            .offset {
                IntOffset(
                    x = (dayWidth.toPx() * event.dayOfWeek + 2.dp.toPx() + stackOffsetX.value).roundToInt(),
                    y = (cellHeight.toPx() * event.startTime + 2.dp.toPx() + stackOffsetY.value).roundToInt()
                )
            }
            .then(
                if (isInteractable) {
                    Modifier.pointerInput(
                        event.id,
                        event.dayOfWeek,
                        event.startTime,
                        dayWidth,
                        cellHeight
                    ) {
                        detectDragGestures(
                            onDragStart = { _ ->
                                settleJob?.cancel()

                                // Capture current animation values before snapping
                                val currentStackX = stackOffsetX.value
                                val currentStackY = stackOffsetY.value

                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDraggingStarted()

                                // Seamlessly transition stack offset into drag offset
                                dragOffset = Offset(currentStackX, currentStackY)
                            },
                            onDragEnd = {
                                finishDrag(commitMove = true)
                            },
                            onDragCancel = {
                                finishDrag(commitMove = false)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                                tiltAngle = (tiltAngle + dragAmount.x * 0.12f).coerceIn(-8f, 8f)
                            }
                        )
                    }
                } else Modifier
            )
            .then(
                if (isInteractable) {
                    Modifier.pointerInput(stackCount, isExpanded) {
                        detectTapGestures {
                            if (stackCount > 1) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isExpanded) onCollapseRequested() else onExpandRequested()
                            }
                        }
                    }
                } else Modifier
            )
            .padding(horizontal = 2.dp)
            .zIndex(if (isThisItemDragging) 200f else if (isExpanded) 100f + stackIndex else 1f + stackIndex)
            .calendarEventVisuals(
                isDragging = isThisItemDragging,
                isAnyItemDragging = isAnyItemDragging,
                isAnyItemExpanded = isAnyItemExpanded,
                isExpanded = isExpanded,
                dragOffset = { dragOffsetState.value },
                tiltAngle = { tiltAngleState.value },
                stackRot = { stackRot.value },
                stackScale = { stackScale.value },
                stackAlpha = { stackAlpha.value },
                scaleAnim = { scaleAnim.value },
                elevationAnim = { elevationAnim.value },
                shakeRotation = { sharedShakeRotation.value },
            )
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        // Background Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(
                            alpha = 0.95f,
                            radius = BlurRadiusSpec.verticalGradient(10.dp, 48.dp),
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        )
                    } else Modifier
                )
                .background(event.color, RoundedCornerShape(10.dp))
        )

        AnimatedContent(
            modifier = Modifier.padding(8.dp),
            targetState = isThisItemDragging,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 2 }) togetherWith
                        (fadeOut() + slideOutVertically { -it / 2 })
            },
            label = "contentChange"
        ) { dragging ->
            if (dragging) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CalendarText(
                        text = DaysOfWeek[potentialDay],
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TechBlack.copy(alpha = 0.9f)
                    )
                    CalendarText(
                        text = formatHour(potentialStartHour),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TechBlack.copy(alpha = 0.7f)
                    )
                }
            } else {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            event.icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = TechBlack.copy(alpha = 0.7f)
                                )
                            }

                            if (!isExpanded && stackIndex == stackCount - 1 && stackCount > 2) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .background(
                                            TechBlack.copy(alpha = 0.08f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    CalendarText(
                                        text = "+${stackCount - 1}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TechBlack.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = TechBlack.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalendarText(
                            text = event.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TechBlack.copy(alpha = 0.95f),
                            lineHeight = 14.sp,
                            maxLines = if (duration > 1) 3 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    CalendarText(
                        text = event.description,
                        fontSize = 9.sp,
                        color = TechBlack.copy(alpha = 0.7f),
                        lineHeight = 11.sp,
                        maxLines = if (duration > 1) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true, widthDp = 540, heightDp = 1080)
@Composable
fun PreviewCalendarGridScreen() {
    MaterialTheme {
        CalendarGridScreen()
    }
}