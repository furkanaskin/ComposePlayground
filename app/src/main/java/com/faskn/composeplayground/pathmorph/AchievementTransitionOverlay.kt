package com.faskn.composeplayground.pathmorph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.drawable.toDrawable
import com.faskn.composeplayground.ui.theme.Black900

@Composable
fun AchievementTransitionOverlay(
    speedMultiplier: Float = 1f,
    onTransitionComplete: () -> Unit
) {
    val clearProgress = remember { Animatable(0f) }
    var isClearing by remember { mutableStateOf(false) }
    val view = LocalView.current

    LaunchedEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        window?.let {
            it.setDimAmount(0f)
            it.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                onDrawWithContent {
                    drawRect(Black900)
                    drawContent()
                }
            }
    ) {
        AchievementTransition(
            isClearing = isClearing,
            clearProgress = clearProgress.value,
            speedMultiplier = speedMultiplier,
            onComplete = {
                isClearing = true
            }
        )

        LaunchedEffect(isClearing) {
            if (isClearing) {
                clearProgress.animateTo(
                    targetValue = 1f, // 0 to 1
                    animationSpec = tween((1000 / speedMultiplier).toInt(), easing = FastOutSlowInEasing)
                )
                onTransitionComplete()
            }
        }
    }
}
