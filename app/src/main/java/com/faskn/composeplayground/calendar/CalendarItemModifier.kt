package com.faskn.composeplayground.calendar

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies drag / stack / shake visuals for one calendar event card as a
 * single graphicsLayer.
 *
 * Every parameter is a *deferred read* (a lambda), not a plain value.
 * `graphicsLayer { ... }` runs at layout/draw time, not composition time,
 * so reading Animatable/State values only *inside* this block means an
 * animation tick updates the layer directly, without ever recomposing the
 * caller. Previously these values (stackRot.value, dragOffset, tiltAngle,
 * etc.) were read eagerly where the modifier was called, which forced a
 * full recomposition of the card on every single animation frame - during
 * a drag, or while the stack fan/collapse spring was settling, that meant
 * ~60 recompositions/sec per visible card.
 *
 * This is deliberately a plain (non-composed) Modifier factory now: it no
 * longer calls any @Composable function itself - animateFloatAsState,
 * animateDpAsState and the shake InfiniteTransition now live in the
 * caller (CalendarEventItem), which is already @Composable. That means we
 * no longer need `composed {}` here, which used to allocate an extra,
 * independent recomposition scope for every event card on screen just to
 * host those calls.
 */
fun Modifier.calendarEventVisuals(
    isDragging: Boolean,
    isAnyItemDragging: Boolean,
    isAnyItemExpanded: Boolean,
    isExpanded: Boolean,
    dragOffset: () -> Offset,
    tiltAngle: () -> Float,
    stackRot: () -> Float,
    stackScale: () -> Float,
    stackAlpha: () -> Float,
    scaleAnim: () -> Float,
    elevationAnim: () -> Dp,
    shakeRotation: () -> Float,
): Modifier = graphicsLayer {
    val baseAlpha = stackAlpha()
    alpha = if (isAnyItemExpanded && !isExpanded && !isDragging) baseAlpha * 0.4f else baseAlpha

    val finalScale = stackScale() * scaleAnim()
    scaleX = finalScale
    scaleY = finalScale

    val offset = dragOffset()
    translationX = offset.x
    translationY = offset.y

    rotationZ = stackRot() + tiltAngle() +
            if (isAnyItemDragging && !isDragging) shakeRotation() else 0f

    shadowElevation = elevationAnim().toPx()
    shape = RoundedCornerShape(10.dp)
    clip = false
}

fun formatHour(hour: Int): String {
    return when (hour) {
        0 -> "12 AM"
        12 -> "12 PM"
        in 1..11 -> "$hour AM"
        else -> "${hour - 12} PM"
    }
}