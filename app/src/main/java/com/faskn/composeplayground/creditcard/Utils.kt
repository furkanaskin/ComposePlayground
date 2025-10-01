package com.faskn.composeplayground.creditcard

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.faskn.composeplayground.R
import kotlin.collections.listOf
import kotlin.math.cos
import kotlin.math.sin

val chromaticColors = listOf(
    Color(0xFFFF00FF).copy(alpha = 0.12f), // Magenta
    Color(0xFF00FFFF).copy(alpha = 0.12f), // Cyan
    Color(0xFFFFFF00).copy(alpha = 0.12f), // Yellow
    Color(0xFFFF6B9D).copy(alpha = 0.12f), // Pink
    Color.Transparent
)

@Composable
fun InspectableCard(
    modifier: Modifier = Modifier,
    cardFrontDrawable: Int = R.drawable.mask_visa_front,
    cardBackDrawable: Int = R.drawable.mask_visa_back,
    accentColor: Color,
    isChromatic: Boolean = false
) {
    val localDensity = LocalDensity.current

    var isFlipped by rememberSaveable { mutableStateOf(false) }
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }

    var cardWidth by remember { mutableFloatStateOf(0f) }
    var cardHeight by remember { mutableFloatStateOf(0f) }

    var touchX by remember { mutableFloatStateOf(0.5f) }
    var touchY by remember { mutableFloatStateOf(0.5f) }

    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "flipRotation"
    )

    val animatedRotationX by animateFloatAsState(
        targetValue = rotationX, animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow
        ), label = "rotationX"
    )

    val animatedRotationY by animateFloatAsState(
        targetValue = rotationY, animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow
        ), label = "rotationY"
    )

    val animatedTouchX by animateFloatAsState(
        targetValue = touchX, animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow
        ), label = "touchX"
    )

    val animatedTouchY by animateFloatAsState(
        targetValue = touchY, animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow
        ), label = "touchY"
    )

    Box(
        modifier = modifier, contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .onGloballyPositioned { coordinates ->
                    cardWidth = coordinates.size.width.toFloat()
                    cardHeight = coordinates.size.height.toFloat()
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            rotationX = 0f
                            rotationY = 0f
                            touchX = 0.5f
                            touchY = 0.5f
                        },
                        onDragCancel = {
                            rotationX = 0f
                            rotationY = 0f
                            touchX = 0.5f
                            touchY = 0.5f
                        },
                    ) { change, dragAmount ->

                        change.consume()

                        val currentTouchX = change.position.x
                        val currentTouchY = change.position.y

                        val normalizedX = (currentTouchX - cardWidth / 2f) / (cardWidth / 2f)
                        val normalizedY = (currentTouchY - cardHeight / 2f) / (cardHeight / 2f)

                        val maxRotation = 25f

                        rotationX = -normalizedY * maxRotation
                        rotationY = normalizedX * maxRotation

                        touchX = (currentTouchX / cardWidth).coerceIn(0f, 1f)
                        touchY = (currentTouchY / cardHeight).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            isFlipped = !isFlipped
                        })
                }
                .graphicsLayer {
                    this.rotationY = flipRotation + animatedRotationY
                    this.rotationX = animatedRotationX
                    cameraDistance = 12f * localDensity.density
                }, contentAlignment = Alignment.Center
        ) {
            CardFace(
                modifier = Modifier.wrapContentSize().run {
                    if (flipRotation > 90f) this.graphicsLayer { this.rotationY = 180f }
                    else this
                },
                cardDrawable = if (flipRotation <= 90f) cardFrontDrawable else cardBackDrawable,
                background = accentColor,
                isChromatic = isChromatic,
                touchX = animatedTouchX,
                touchY = animatedTouchY
            )
        }
    }
}

@Composable
fun CardFace(
    modifier: Modifier = Modifier,
    @DrawableRes cardDrawable: Int,
    background: Color = Color.White,
    isChromatic: Boolean = false,
    touchX: Float = 0.5f,
    touchY: Float = 0.5f
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.clip(shape = RoundedCornerShape(size = 20.dp)),
            colors = CardDefaults.cardColors(containerColor = if (isChromatic) Color.White else background),
        ) {
            Image(
                painter = painterResource(cardDrawable),
                modifier = Modifier.aspectRatio(0.66f),
                contentDescription = ""
            )
        }

        // Chromatic overlay
        if (isChromatic) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape = RoundedCornerShape(size = 20.dp))
            ) {
                val gradientBrush = Brush.linearGradient(colors = chromaticColors)

                drawRect(
                    brush = gradientBrush, size = size, blendMode = BlendMode.Multiply
                )

                // Add secondary linear gradient for more dynamic effect
                val angle = (touchX - 0.5f) * 3.14f * 2f
                val gradientBrush2 = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00FFFF).copy(alpha = 0.12f), // Cyan
                        Color.Transparent,
                        Color(0xFFFF00FF).copy(alpha = 0.12f), // Magenta
                    ), start = Offset(
                        x = size.width * touchX - cos(angle) * size.width * 0.5f,
                        y = size.height * touchY - sin(angle) * size.height * 0.5f
                    ), end = Offset(
                        x = size.width * touchX + cos(angle) * size.width * 0.5f,
                        y = size.height * touchY + sin(angle) * size.height * 0.5f
                    )
                )

                drawRect(
                    brush = gradientBrush2, size = size, blendMode = BlendMode.Multiply
                )
            }
        }
    }
}