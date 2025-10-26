package com.faskn.composeplayground.sidepanel

import android.app.Activity
import android.graphics.BlurMaskFilter
import android.graphics.Rect
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sqrt

private const val DRAG_THRESHOLD = 0.5f
private const val RAINBOW_CIRCLE_THICKNESS = 150f
private const val BLUR_RADIUS = 24f

private val RAINBOW_COLORS = listOf(
    Color(0xFF7F3FF2), // Deep Violet
    Color(0xFF3F8EF2), // Tech Blue
    Color(0xFF3FF2C5), // Aqua Green
    Color(0xFFF2E23F), // Vivid Yellow
    Color(0xFFF29F3F), // Orange
    Color(0xFFF23F55), // Magenta Red
    Color.Transparent  // For clearing center with BlendMode.Clear
)

/**
 * A side panel layout with drag gestures and arc animation effect.
 *
 * Features:
 * - Smooth drag gesture handling
 * - Rainbow ripple animation on expand
 * - Arc-shaped edge animation
 *
 * @param modifier Modifier to be applied to the layout
 * @param state State holder for managing the side panel state
 * @param arrangement Placement of the side panel (Start or End)
 * @param containerColor Background color of the side panel container
 * @param dragHandleContent Composable content for the drag handle
 * @param content Composable content inside the side panel
 */
@Composable
fun SidePanelLayout(
    modifier: Modifier = Modifier,
    state: SidePanelStateHolder,
    arrangement: SidePanelArrangement = SidePanelArrangement.End,
    containerColor: Color = Color.Magenta,
    dragHandleContent: @Composable (isExpanded: Boolean, progress: Float) -> Unit,
    content: @Composable (Float) -> Unit
) {
    val windowInfo = LocalWindowInfo.current
    val screenWidthPx = windowInfo.containerSize.width.toFloat()
    val scope = rememberCoroutineScope()

    val outlineShape =
        remember(state.arcProgress, arrangement, state.animatedWidth, state.currentState) {
            ArcShape(
                progress = state.arcProgress,
                arrangement = arrangement,
                maxArcHeight = state.animatedWidth,
                state = state.currentState
            )
        }

    Layout(
        content = {
            Box(
                modifier = modifier
                    .fillMaxHeight()
                    .background(
                        shape = outlineShape,
                        color = containerColor
                    )
            ) {
                content(state.arcProgress)
            }

            RainbowAnimationCanvas(
                state = state,
                outlineShape = outlineShape
            )

            DragHandle(
                state = state,
                arrangement = arrangement,
                dragHandleContent = dragHandleContent,
            )
        }
    ) { measurables, constraints ->
        require(measurables.size == 3) { "SidePanelLayout expects exactly 3 measurables" }

        val sheetPlaceable = measurables[0].measure(
            constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth)
        )

        val canvasPlaceable = measurables[1].measure(
            constraints.copy(
                minWidth = sheetPlaceable.width,
                maxWidth = sheetPlaceable.width,
                minHeight = sheetPlaceable.height,
                maxHeight = sheetPlaceable.height
            )
        )

        val dragHandlePlaceable = measurables[2].measure(constraints)

        // Update measured width
        scope.launch { state.setMeasuredWidth(sheetPlaceable.width.toFloat()) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val positions = calculatePositions(
                arrangement = arrangement,
                currentWidth = state.animatedWidth.toInt(),
                screenWidth = screenWidthPx.toInt(),
                sheetWidth = sheetPlaceable.width,
                dragHandleWidth = dragHandlePlaceable.width,
                maxHeight = constraints.maxHeight,
                sheetHeight = sheetPlaceable.height,
                dragHandleHeight = dragHandlePlaceable.height
            )

            sheetPlaceable.place(positions.sheetX, positions.sheetY)
            canvasPlaceable.place(positions.sheetX, positions.sheetY)
            dragHandlePlaceable.place(positions.dragHandleX, positions.dragHandleY)
        }
    }
}

@Composable
private fun RainbowAnimationCanvas(
    state: SidePanelStateHolder,
    outlineShape: ArcShape
) {
    Canvas(
        modifier = Modifier
            .fillMaxHeight()
            .graphicsLayer {
                compositingStrategy =
                    CompositingStrategy.Offscreen // Needed for BlendMode.Clear Trick
            }
    ) {
        if (shouldShowRainbowAnimation(state)) {
            drawRainbowAnimation(state, outlineShape, size, layoutDirection)
        }
    }
}

@Composable
private fun DragHandle(
    state: SidePanelStateHolder,
    arrangement: SidePanelArrangement,
    dragHandleContent: @Composable (Boolean, Float) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var dragHandleRect by remember { mutableStateOf(Rect(0, 0, 0, 0)) }
    ExcludeDragHandleRectFromGesture(dragHandleRect)

    Box(
        modifier = Modifier
            .onGloballyPositioned { layoutCoordinates ->
                dragHandleRect = calculateDragHandleRect(layoutCoordinates)
            }
            .pointerInput(state) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch { state.handleDragEnd() }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val dragDelta = calculateDragDelta(arrangement, dragAmount.x)
                            state.handleDrag(dragDelta)
                        }
                    }
                )
            }
    ) {
        dragHandleContent(
            state.currentState == SidePanelState.Expanded,
            state.arcProgress
        )
    }
}


enum class SidePanelState { Expanded, Collapsed }
enum class SidePanelArrangement { Start, End }

@Immutable
class SidePanelStateHolder internal constructor(
    initialState: SidePanelState
) {
    private val _state = mutableStateOf(initialState)
    val currentState: SidePanelState by _state

    private val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    private val sheetWidth = Animatable(0f)
    private var measuredSheetWidthPx by mutableFloatStateOf(0f)
    private var isInitialized by mutableStateOf(false)

    val animatedWidth: Float get() = sheetWidth.value
    val arcProgress: Float
        get() = if (measuredSheetWidthPx > 0) {
            (sheetWidth.value / measuredSheetWidthPx).coerceIn(0f, 1f)
        } else {
            0f
        }

    internal suspend fun setMeasuredWidth(width: Float) {
        if (measuredSheetWidthPx == width) return

        measuredSheetWidthPx = width
        if (!isInitialized && width > 0f) {
            isInitialized = true
            val targetWidth = if (currentState == SidePanelState.Expanded) width else 0f
            sheetWidth.updateBounds(0f, width)
            sheetWidth.snapTo(targetWidth)
        }
    }

    suspend fun animateTo(
        state: SidePanelState,
        arcAnimationSpec: FiniteAnimationSpec<Float> = animationSpec
    ) {
        if (measuredSheetWidthPx <= 0f) return

        val targetWidth = if (state == SidePanelState.Expanded) measuredSheetWidthPx else 0f
        sheetWidth.animateTo(targetWidth, arcAnimationSpec)
        _state.value = state
    }

    suspend fun expand() = animateTo(SidePanelState.Expanded)
    suspend fun collapse() = animateTo(SidePanelState.Collapsed)

    suspend fun toggle() {
        val newState = if (currentState == SidePanelState.Expanded) {
            SidePanelState.Collapsed
        } else {
            SidePanelState.Expanded
        }
        animateTo(newState)
    }

    internal suspend fun handleDrag(dragDelta: Float) {
        if (measuredSheetWidthPx <= 0f) return
        val newWidth = (sheetWidth.value + dragDelta).coerceIn(0f, measuredSheetWidthPx)
        sheetWidth.snapTo(newWidth)
    }

    internal suspend fun handleDragEnd() {
        if (measuredSheetWidthPx <= 0f) return
        val expandThreshold = measuredSheetWidthPx * DRAG_THRESHOLD
        val newState = if (sheetWidth.value < expandThreshold) {
            SidePanelState.Collapsed
        } else {
            SidePanelState.Expanded
        }
        animateTo(newState)
    }
}

@Composable
fun rememberSidePanelState(
    initialState: SidePanelState = SidePanelState.Collapsed,
): SidePanelStateHolder {
    return remember(initialState) {
        SidePanelStateHolder(initialState)
    }
}

private fun shouldShowRainbowAnimation(state: SidePanelStateHolder): Boolean {
    return state.currentState == SidePanelState.Collapsed && state.arcProgress > 0f
}

private fun calculateDragDelta(arrangement: SidePanelArrangement, dragX: Float): Float {
    return if (arrangement == SidePanelArrangement.End) -dragX else dragX
}

private fun calculateDragHandleRect(layoutCoordinates: androidx.compose.ui.layout.LayoutCoordinates): Rect {
    val position = layoutCoordinates.localToWindow(Offset.Zero)
    val size = layoutCoordinates.size
    return Rect(
        position.x.toInt(),
        position.y.toInt(),
        (position.x + size.width).toInt(),
        (position.y + size.height).toInt()
    )
}

private data class LayoutPositions(
    val sheetX: Int,
    val sheetY: Int,
    val dragHandleX: Int,
    val dragHandleY: Int
)

private fun calculatePositions(
    arrangement: SidePanelArrangement,
    currentWidth: Int,
    screenWidth: Int,
    sheetWidth: Int,
    dragHandleWidth: Int,
    maxHeight: Int,
    sheetHeight: Int,
    dragHandleHeight: Int
): LayoutPositions {
    val isEnd = arrangement == SidePanelArrangement.End

    val sheetX = if (isEnd) {
        screenWidth - currentWidth
    } else {
        currentWidth - sheetWidth
    }

    val dragHandleX = if (isEnd) {
        screenWidth - currentWidth - (dragHandleWidth / 2)
    } else {
        currentWidth - (dragHandleWidth / 2)
    }

    val sheetY = (maxHeight - sheetHeight) / 2
    val dragHandleY = (maxHeight - dragHandleHeight) / 2

    return LayoutPositions(sheetX, sheetY, dragHandleX, dragHandleY)
}

private fun DrawScope.drawRainbowAnimation(
    state: SidePanelStateHolder,
    outlineShape: ArcShape,
    size: Size,
    layoutDirection: LayoutDirection
) {
    val outline = outlineShape.createOutline(size, layoutDirection, this)
    val path = (outline as? Outline.Generic)?.path ?: return

    clipPath(path) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = calculateMaxRadius(centerX, centerY)
        val progress = state.arcProgress
        val animatedMaxRadius = maxRadius * progress

        drawRainbowCircles(centerX, centerY, animatedMaxRadius)
    }
}

private fun calculateMaxRadius(centerX: Float, centerY: Float): Float {
    return sqrt(centerX * centerX + centerY * centerY) * PI.div(2).toFloat()
}

private fun DrawScope.drawRainbowCircles(
    centerX: Float,
    centerY: Float,
    maxRadius: Float
) {
    RAINBOW_COLORS.forEachIndexed { index, color ->
        val circleRadius = maxRadius - (index * RAINBOW_CIRCLE_THICKNESS)

        if (index == RAINBOW_COLORS.size - 1) {
            // Clear center circle
            drawCircle(
                color = color,
                radius = circleRadius,
                center = Offset(centerX, centerY),
                blendMode = BlendMode.Clear
            )
        } else {
            // Draw colored circles with blur
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    this.color = color
                    this.asFrameworkPaint().maskFilter =
                        BlurMaskFilter(BLUR_RADIUS, BlurMaskFilter.Blur.INNER)
                }
                canvas.drawCircle(Offset(centerX, centerY), circleRadius, paint)
            }
        }
    }
}

/**
 * Exclude the drag handle area from system gesture detection to prevent conflicts.
 */
@Composable
private fun ExcludeDragHandleRectFromGesture(dragHandleRect: Rect) {
    val context = LocalContext.current

    LaunchedEffect(dragHandleRect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val activity = context as? Activity ?: return@LaunchedEffect
            val decorView = activity.window?.decorView ?: return@LaunchedEffect
            decorView.systemGestureExclusionRects = listOf(dragHandleRect)
        }
    }
}


/**
 * Custom shape that creates an arc animation effect.
 * The arc appears during transitions between collapsed and expanded states.
 */
class ArcShape(
    private val progress: Float,
    private val arrangement: SidePanelArrangement,
    private val maxArcHeight: Float,
    private val state: SidePanelState
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val currentWidth = size.width * progress
        val arcProgress = calculateArcProgress(progress)
        val arcDepth = arcProgress * maxArcHeight

        val isEnd = arrangement == SidePanelArrangement.End
        val edgeX = if (isEnd) size.width - currentWidth else currentWidth
        val startX = if (isEnd) size.width else 0f

        // Build the path
        path.moveTo(startX, 0f)
        path.lineTo(startX, size.height)
        path.lineTo(edgeX, size.height)

        if (arcDepth > 0 && currentWidth > 0f) {
            addArcToPath(path, edgeX, arcDepth, size.height, isEnd)
        } else {
            path.lineTo(edgeX, 0f)
        }

        path.close()
        return Outline.Generic(path)
    }

    private fun calculateArcProgress(progress: Float): Float {
        return if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
    }

    private fun addArcToPath(
        path: Path,
        edgeX: Float,
        arcDepth: Float,
        height: Float,
        isEnd: Boolean
    ) {
        val arcSign = if (state == SidePanelState.Collapsed) 1 else -1
        val controlX = if (isEnd) {
            edgeX - (arcSign * arcDepth)
        } else {
            edgeX + (arcSign * arcDepth)
        }

        path.cubicTo(
            controlX, height * 0.75f,
            controlX, height * 0.25f,
            edgeX, 0f
        )
    }
}