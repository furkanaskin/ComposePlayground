package com.faskn.composeplayground.telemetry.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.faskn.composeplayground.R
import com.faskn.composeplayground.telemetry.data.ChatMessage
import com.faskn.composeplayground.telemetry.data.ReplayMarker
import com.faskn.composeplayground.telemetry.data.Role
import com.faskn.composeplayground.telemetry.racingColors
import com.faskn.composeplayground.ui.theme.Black950
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val BUBBLE_SIZE_DP = 56
private const val EXPANDED_WIDTH_DP = 320
private const val EXPANDED_HEIGHT_DP = 400
private const val MARGIN_DP = 16

private data class BubbleSizes(
    val bubbleSizePx: Int,
    val expandedWidthPx: Int,
    val expandedHeightPx: Int,
    val marginPx: Int
)

@Composable
fun RaceEngineerBubble(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onMarkerClick: (ReplayMarker) -> Unit = {},
) {
    var isExpanded by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val colors = racingColors
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val sizes = remember(density) {
        with(density) {
            BubbleSizes(
                bubbleSizePx = BUBBLE_SIZE_DP.dp.toPx().toInt(),
                expandedWidthPx = EXPANDED_WIDTH_DP.dp.toPx().toInt(),
                expandedHeightPx = EXPANDED_HEIGHT_DP.dp.toPx().toInt(),
                marginPx = MARGIN_DP.dp.toPx().toInt()
            )
        }
    }

    // Using Animatable for high-performance manual offset control
    val offsetAnim = remember { Animatable(IntOffset(0, 0), IntOffset.VectorConverter) }
    var initialized by remember { mutableStateOf(false) }

    // Initial positioning
    LaunchedEffect(containerSize) {
        if (containerSize != IntSize.Zero && !initialized) {
            val initialX = containerSize.width - sizes.bubbleSizePx - sizes.marginPx
            val initialY = containerSize.height - sizes.bubbleSizePx - sizes.marginPx
            offsetAnim.snapTo(IntOffset(initialX.coerceAtLeast(0), initialY.coerceAtLeast(0)))
            initialized = true
        }
    }

    fun getConstraints(expanded: Boolean): Pair<Int, Int> {
        val w = if (expanded) sizes.expandedWidthPx else sizes.bubbleSizePx
        val h = if (expanded) sizes.expandedHeightPx else sizes.bubbleSizePx
        return (containerSize.width - w).coerceAtLeast(0) to (containerSize.height - h).coerceAtLeast(
            0
        )
    }

    fun snapAfterToggle(targetExpanded: Boolean) {
        val (maxX, maxY) = getConstraints(targetExpanded)
        val currentW = if (!targetExpanded) sizes.expandedWidthPx else sizes.bubbleSizePx
        val snapX = if (offsetAnim.value.x > (containerSize.width - currentW) / 2) maxX else 0
        scope.launch {
            offsetAnim.animateTo(
                IntOffset(snapX, offsetAnim.value.y.coerceIn(0, maxY)),
                tween(300)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { containerSize = it.size }
    ) {
        Box(
            modifier = Modifier
                .offset { offsetAnim.value }
                .pointerInput(containerSize, isExpanded) {
                    detectDragGestures(
                        onDragEnd = {
                            val (maxX, _) = getConstraints(isExpanded)
                            val targetX = if (offsetAnim.value.x > maxX / 2) maxX else 0
                            scope.launch {
                                offsetAnim.animateTo(offsetAnim.value.copy(x = targetX), tween(300))
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val (maxX, maxY) = getConstraints(isExpanded)
                        val newX =
                            (offsetAnim.value.x + dragAmount.x.roundToInt()).coerceIn(0, maxX)
                        val newY =
                            (offsetAnim.value.y + dragAmount.y.roundToInt()).coerceIn(0, maxY)
                        scope.launch { offsetAnim.snapTo(IntOffset(newX, newY)) }
                    }
                }
                .clip(RoundedCornerShape(if (isExpanded) 16.dp else 28.dp))
                .background(if (isExpanded) Black950 else colors.raceEngineerDark)
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300)) using SizeTransform(clip = false)
                },
                label = "bubbleContent"
            ) { expanded ->
                if (expanded) {
                    Box(modifier = Modifier
                        .width(EXPANDED_WIDTH_DP.dp)
                        .height(EXPANDED_HEIGHT_DP.dp)) {
                        RaceEngineerSection(
                            messages = messages,
                            isLoading = isLoading,
                            isExpanded = true,
                            onToggleExpanded = {
                                isExpanded = false
                                snapAfterToggle(false)
                            },
                            onSendMessage = onSendMessage,
                            onMarkerClick = onMarkerClick
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(BUBBLE_SIZE_DP.dp)
                            .clickable {
                                isExpanded = true
                                snapAfterToggle(true)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.gemini_24dp),
                            contentDescription = "Race Engineer",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun RaceEngineerBubblePreview() {
    Box(Modifier
        .fillMaxSize()
        .background(Color.Gray)) {
        RaceEngineerBubble(
            messages = listOf(
                ChatMessage(
                    role = Role.ENGINEER,
                    text = "Hello, I am your race engineer."
                )
            ),
            isLoading = false,
            onSendMessage = {}
        )
    }
}

@Preview
@Composable
fun RaceEngineerBubbleExpandedPreview() {
    Box(Modifier
        .fillMaxSize()
        .background(Color.Gray)) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
                .background(Black950)
        ) {
            RaceEngineerSection(
                messages = listOf(
                    ChatMessage(
                        role = Role.ENGINEER,
                        text = "Hello, I am your race engineer. How can I help you today with your telemetry data?"
                    ),
                    ChatMessage(
                        role = Role.USER,
                        text = "Check my last lap performance."
                    )
                ),
                isLoading = false,
                isExpanded = true,
                onToggleExpanded = { },
                onSendMessage = {}
            )
        }
    }
}
